/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.cli.impl;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.aesh.readline.Prompt;
import org.aesh.readline.terminal.TerminalBuilder;
import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Terminal;
import org.aesh.terminal.tty.Signal;
import org.aesh.util.Parser;
import org.aesh.utils.Config;
import static org.jboss.as.cli.impl.ReadlineConsole.LOG;

/**
 *
 * @author jdenise@redhat.com
 */
public class TerminalConsole extends ReadlineConsole {

    /**
     *
     * All chars printed by commands and prompts go through this class that log
     * any printed content. What is received by inputstream is echoed in the
     * outputstream.
     */
    private static class CLITerminalConnection extends TerminalConnection {

        private final Consumer<int[]> interceptor;
        private Thread connectionThread;

        CLITerminalConnection(Terminal terminal) {
            super(terminal);
            interceptor = (int[] ints) -> {
                if (isTraceEnabled) {
                    LOG.tracef("Writing %s",
                            Parser.stripAwayAnsiCodes(Parser.fromCodePoints(ints)));
                }
                CLITerminalConnection.super.stdoutHandler().accept(ints);
            };
        }

        @Override
        public Consumer<int[]> stdoutHandler() {
            return interceptor;
        }

        /**
         * Required to close the connection reading on the terminal, otherwise
         * it can't be interrupted.
         *
         * @throws InterruptedException
         */
        public void openBlockingInterruptable()
                throws InterruptedException {
            // We need to thread this call in order to interrupt it (when Ctrl-C occurs).
            connectionThread = new Thread(() -> {
                // This thread can't be interrupted from another thread.
                // Will stay alive until System.exit is called.
                Thread thr = new Thread(() -> super.openBlocking(),
                        "CLI Terminal Connection (uninterruptable)");
                thr.start();
                try {
                    thr.join();
                } catch (InterruptedException ex) {
                    // XXX OK, interrupted, just leaving.
                }
            }, "CLI Terminal Connection (interruptable)");
            connectionThread.start();
            connectionThread.join();
        }

        @Override
        public void close() {
            super.close();
            if (connectionThread != null) {
                connectionThread.interrupt();
            }
        }
    }

    private CLITerminalConnection connection;
    private Thread startThread;

    TerminalConsole(Settings settings) throws IOException {
        super(settings);
    }

    @Override
    protected void initializeConnection() throws IOException {
        if (connection == null) {
            connection = newConnection();
            interruptHandler = signal -> {
                if (signal == Signal.INT) {
                    LOG.trace("Calling InterruptHandler");
                    connection.write(Config.getLineSeparator());
                    connection.close();
                }
            };
            connection.setSignalHandler(interruptHandler);
            // Do not display ^C
            Attributes attr = connection.getAttributes();
            attr.setLocalFlag(Attributes.LocalFlag.ECHOCTL, false);
            connection.setAttributes(attr);
        }
    }

    private CLITerminalConnection newConnection() throws IOException {
        LOG.trace("Creating terminal connection");

        // The choice of the outputstream to use is ruled by the following rules:
        // - If the output is redefined in CLIPrintStream, the terminal use the CLIPrintStream
        // - If some redirection have been establised ( <, or remote process), CLIPrintStream is to be used.
        //   That is required to protect the CLI against embed-server I/O handling.
        // - Otherwise, a system terminal is used. system terminals don't use System.out
        //   so are protected against embed-server I/O handling.
        Terminal terminal = TerminalBuilder.builder()
                .input(settings.getInStream() == null
                        ? System.in : settings.getInStream())
                // Use CLI stream if not a system terminal, it protects against
                // embed-server I/O redefinition
                .output(settings.getOutStream())
                .nativeSignals(true)
                .name("CLI Terminal")
                // We ask for a system terminal only if the Output has not been redefined.
                // If the IO context is redirected ( <, or remote process usage),
                // then, whatever the output being redefined or not, the terminal
                // will be NOT a system terminal, that is the TerminalBuilder behavior.
                .system(!settings.isOutputRedefined())
                .build();
        if (isTraceEnabled) {
            LOG.tracef("New Terminal %s", terminal.getClass());
        }
        CLITerminalConnection c = new CLITerminalConnection(terminal);
        return c;
    }

    private String promptFromNonStartedConsole(Prompt prompt) throws InterruptedException, IOException {
        initializeConnection();
        LOG.trace("Not started");
        String[] out = new String[1];
        if (connection.suspended()) {
            connection.awake();
        }
        getReadLine().readline(connection, prompt, newLine -> {
            out[0] = newLine;
            LOG.trace("Got some input");

            // We must call stopReading to stop reading from terminal
            // and release this thread.
            connection.stopReading();
        }, null, null, null, null, READLINE_FLAGS);
        connection.openBlockingInterruptable();
        LOG.trace("Done for prompt");
        return out[0];
    }

    private String promptFromStartedConsole(Prompt prompt) throws InterruptedException, IOException {
        initializeConnection();
        String[] out = new String[1];
        // We must be called from another Thread. connection is reading in Main thread.
        // calling readline will wakeup the Main thread that will execute
        // the Prompt handling.
        // We can safely wait.
        if (readingThread == startThread) {
            throw new RuntimeException("Can't prompt from the Thread that is "
                    + "reading terminal input");
        }
        CountDownLatch latch = new CountDownLatch(1);
        // We need to set the interrupt SignalHandler to interrupt the current thread.
        Consumer<Signal> prevHandler = connection.getSignalHandler();
        connection.setSignalHandler(interruptHandler);
        getReadLine().readline(connection, prompt, newLine -> {
            out[0] = newLine;
            LOG.trace("Got some input");
            latch.countDown();
        }, null, null, null, null, READLINE_FLAGS);
        try {
            latch.await();
        } finally {
            connection.setSignalHandler(prevHandler);
        }
        LOG.trace("Done for prompt");
        return out[0];
    }

    @Override
    protected String doReadLine(Prompt prompt) throws InterruptedException, IOException {
        if (!started) {
            // That is the case of the CLI connecting prior to start the terminal.
            // No Terminal waiting in Main thread yet.
            // We are opening the connection to the terminal until we have read
            // something from prompt.
            return promptFromNonStartedConsole(prompt);
        } else {
            return promptFromStartedConsole(prompt);
        }
    }

    @Override
    public void doStart() throws IOException {
        if (!started) {
            initializeConnection();
            startThread = Thread.currentThread();
            started = true;
            loop();
            LOG.tracef("Started in thread %s. Waiting...",
                    startThread.getName());

            try {
                connection.openBlockingInterruptable();
            } catch (InterruptedException ex) {
                // OK leaving
            }
            LOG.trace("Leaving console");
        } else {
            LOG.trace("Already started");
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

}
