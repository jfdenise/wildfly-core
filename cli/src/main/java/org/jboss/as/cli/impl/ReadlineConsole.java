/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.cli.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.aesh.readline.Prompt;
import org.aesh.readline.Readline;
import org.aesh.readline.alias.AliasCompletion;
import org.aesh.readline.alias.AliasManager;
import org.aesh.readline.alias.AliasPreProcessor;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.history.FileHistory;
import org.aesh.terminal.Terminal;
import org.aesh.readline.terminal.TerminalBuilder;
import org.aesh.terminal.tty.Signal;
import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Attributes;
import org.aesh.utils.ANSI;
import org.aesh.utils.Config;
import org.aesh.util.FileAccessPermission;
import org.aesh.util.Parser;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.logmanager.Logger;

/**
 * Integration point with aesh-readline.
 *
 * @author jdenise@redhat.com
 */
public class ReadlineConsole implements Console {

    private static final Logger LOG = Logger.getLogger(ReadlineConsole.class.getName());

    public interface Settings {

        /**
         * @return the inStream
         */
        InputStream getInStream();

        /**
         * @return the outStream
         */
        OutputStream getOutStream();

        /**
         * @return the disableHistory
         */
        boolean isDisableHistory();

        /**
         * @return the outputRedefined
         */
        boolean isOutputRedefined();

        /**
         * @return the historyFile
         */
        File getHistoryFile();

        /**
         * @return the historySize
         */
        int getHistorySize();

        /**
         * @return the permission
         */
        FileAccessPermission getPermission();

        /**
         * @return the interrupt
         */
        Runnable getInterrupt();
    }

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
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Writing {0}",
                            Parser.stripAwayAnsiCodes(Parser.fromCodePoints(ints)));
                }
                CLITerminalConnection.super.stdoutHandler().accept(ints);
            };
        }

        @Override
        public Consumer<int[]> stdoutHandler() {
            return interceptor;
        }

        public void openBlockingInterruptable()
                throws InterruptedException {
            // We need to thread this call in order to be able to handle Ctrl-C
            // It will be interrupted and the console will leave.
            connectionThread = new Thread(() -> {
                // This thread can't be interrupted from another thread.
                // Will stay alive until System.exit is called.
                Thread thr = new Thread(() -> super.openBlocking());
                thr.start();
                try {
                    thr.join();
                } catch (InterruptedException ex) {
                    // XXX OK, interrupted, just leaving.
                }
            });
            connectionThread.start();
            connectionThread.join();
        }

        @Override
        public void close() {
            if(connectionThread !=null) {
                connectionThread.interrupt();
            }
            super.close();
        }
    }

    private static class SettingsImpl implements Settings {

        private final InputStream inStream;
        private final OutputStream outStream;
        private final boolean disableHistory;
        private final File historyFile;
        private final int historySize;
        private final FileAccessPermission permission;
        private final Runnable interrupt;
        private final boolean outputRedefined;

        private SettingsImpl(InputStream inStream,
                OutputStream outStream,
                boolean outputRedefined,
                boolean disableHistory,
                File historyFile,
                int historySize,
                FileAccessPermission permission,
                Runnable interrupt) {
            this.inStream = inStream;
            this.outStream = outStream;
            this.outputRedefined = outputRedefined;
            this.disableHistory = disableHistory;
            this.historyFile = historyFile;
            this.historySize = historySize;
            this.permission = permission;
            this.interrupt = interrupt;
        }

        /**
         * @return the inStream
         */
        @Override
        public InputStream getInStream() {
            return inStream;
        }

        /**
         * @return the outStream
         */
        @Override
        public OutputStream getOutStream() {
            return outStream;
        }

        /**
         * @return the disableHistory
         */
        @Override
        public boolean isDisableHistory() {
            return disableHistory;
        }

         /**
         * @return the outputRedefined
         */
        @Override
        public boolean isOutputRedefined() {
            return outputRedefined;
        }

        /**
         * @return the historyFile
         */
        @Override
        public File getHistoryFile() {
            return historyFile;
        }

        /**
         * @return the historySize
         */
        @Override
        public int getHistorySize() {
            return historySize;
        }

        /**
         * @return the permission
         */
        @Override
        public FileAccessPermission getPermission() {
            return permission;
        }

        /**
         * @return the interrupt
         */
        @Override
        public Runnable getInterrupt() {
            return interrupt;
        }
    }

    public static class SettingsBuilder {

        private InputStream inStream;
        private OutputStream outStream;
        private boolean disableHistory;
        private File historyFile;
        private int historySize;
        private FileAccessPermission permission;
        private Runnable interrupt;
        private boolean outputRedefined;

        public SettingsBuilder inputStream(InputStream inStream) {
            this.inStream = inStream;
            return this;
        }

        public SettingsBuilder outputStream(OutputStream outStream) {
            this.outStream = outStream;
            return this;
        }

        public SettingsBuilder disableHistory(boolean disableHistory) {
            this.disableHistory = disableHistory;
            return this;
        }

        public SettingsBuilder historyFile(File historyFile) {
            this.historyFile = historyFile;
            return this;
        }

        public SettingsBuilder historySize(int historySize) {
            this.historySize = historySize;
            return this;
        }

        public SettingsBuilder historyFilePermission(FileAccessPermission permission) {
            this.permission = permission;
            return this;
        }

        public SettingsBuilder interruptHook(Runnable interrupt) {
            this.interrupt = interrupt;
            return this;
        }

        public SettingsBuilder outputRedefined(boolean outputRedefined) {
            this.outputRedefined = outputRedefined;
            return this;
        }

        public Settings create() {
            return new SettingsImpl(inStream, outStream, outputRedefined,
                    disableHistory, historyFile, historySize, permission, interrupt);
        }
    }

    class HistoryImpl implements CommandHistory {

        @Override
        public List<String> asList() {
            List<String> lst = new ArrayList<>();
            for (int[] l : readlineHistory.getAll()) {
                lst.add(Parser.stripAwayAnsiCodes(Parser.fromCodePoints(l)));
            }
            return lst;
        }

        @Override
        public boolean isUseHistory() {
            return readlineHistory.isEnabled();
        }

        @Override
        public void setUseHistory(boolean useHistory) {
            if (useHistory) {
                readlineHistory.enable();
            } else {
                readlineHistory.disable();
            }

        }

        @Override
        public void clear() {
            readlineHistory.clear();
        }

        @Override
        public int getMaxSize() {
            return readlineHistory.size();
        }
    }
    private final CommandContext cmdCtx;
    private final List<Completion> completions = new ArrayList<>();
    private final Readline readline;
    private final CLITerminalConnection connection;
    private final CommandHistory history = new HistoryImpl();
    private final FileHistory readlineHistory;
    private String prompt;
    private final Settings settings;
    private volatile boolean started;
    private volatile boolean closed;
    private Thread startThread;
    private Thread readingThread;
    private Consumer<String> callback;

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    private final AliasManager aliasManager;
    private final List<Function<String, Optional<String>>> preProcessors = new ArrayList<>();

    private final Consumer<Signal> exitHandler;
    private final Consumer<Signal> interruptHandler;
    private boolean interrupted;

    ReadlineConsole(CommandContext cmdCtx, Settings settings) throws IOException {
        this.cmdCtx = cmdCtx;
        this.settings = settings;
        readlineHistory = new FileHistory(settings.getHistoryFile(),
                settings.getHistorySize(), settings.getPermission(), false);
        if (settings.isDisableHistory()) {
            readlineHistory.disable();
        } else {
            readlineHistory.enable();
        }
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "History is enabled? {0}", !settings.isDisableHistory());
        }
        connection = newConnection();

        exitHandler = (signal) -> {
            if (signal == Signal.INT) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Calling exitHandler");
                }
                connection.write(Config.getLineSeparator());
                interrupted = true;
                settings.getInterrupt().run();
            }
        };
        interruptHandler = (signal) -> {
            if (signal == Signal.INT) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Calling interruptHandler");
                }
                connection.write(Config.getLineSeparator());
                interrupted = true;
                readingThread.interrupt();
            }
        };
        // Exit if no command is running.
        connection.setSignalHandler(exitHandler);
        // Do not display ^C
        Attributes attr = connection.getAttributes();
        attr.setLocalFlag(Attributes.LocalFlag.ECHOCTL, false);
        connection.setAttributes(attr);

        aliasManager = new AliasManager(new File(Config.getHomeDir()
                + Config.getPathSeparator() + ".aesh_aliases"), true);
        AliasPreProcessor aliasPreProcessor = new AliasPreProcessor(aliasManager);
        preProcessors.add(aliasPreProcessor);
        completions.add(new AliasCompletion(aliasManager));
        readline = new Readline();
    }

    @Override
    public boolean hasBeenInterrupted() {
        return interrupted;
    }

    @Override
    public void setActionCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    private CLITerminalConnection newConnection() throws IOException {
        CLIPrintStream stream = (CLIPrintStream) settings.getOutStream();
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Creating terminal connection ");
        }

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
                .output(stream)
                .nativeSignals(true)
                .name("CLI Terminal")
                // We ask for a system terminal only if the Output has not been redefined.
                // If the IO context is redirected ( <, or remote process usage),
                // then, whatever the output being redefined or not, the terminal
                // will be NOT a system terminal, that is the TerminalBuilder behavior.
                .system(!settings.isOutputRedefined())
                .build();
        CLITerminalConnection c = new CLITerminalConnection(terminal);
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "New Terminal {0}", terminal.getClass());
        }
        return c;
    }

    @Override
    public void addCompleter(CommandLineCompleter completer) {
        completions.add((Completion) (CompleteOperation co) -> {
            List<String> candidates = new ArrayList<>();
            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Completing {0}", co.getBuffer());
            }
            int offset = completer.complete(cmdCtx,
                    co.getBuffer(), co.getCursor(), candidates);
            co.setOffset(offset);
            co.addCompletionCandidates(candidates);
            String buffer = cmdCtx.getArgumentsString() == null
                    ? co.getBuffer() : cmdCtx.getArgumentsString() + co.getBuffer();
            if (co.getCompletionCandidates().size() == 1
                    && co.getCompletionCandidates().get(0).
                    getCharacters().startsWith(buffer)) {
                co.doAppendSeparator(true);
            } else {
                co.doAppendSeparator(false);
            }
            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Completion candidates {0}",
                        co.getCompletionCandidates());
            }
        });
    }

    @Override
    public CommandHistory getHistory() {
        return history;
    }

    @Override
    public void clearScreen() {
        connection.stdoutHandler().accept(ANSI.CLEAR_SCREEN);
    }

    @Override
    public void printColumns(Collection<String> list) {
        String[] newList = new String[list.size()];
        list.toArray(newList);
        connection.write(
                Parser.formatDisplayList(newList,
                        getHeight(),
                        getWidth()));
    }

    @Override
    public void print(String line) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Print {0}", line);
        }
        connection.write(line);
    }

    @Override
    public void printNewLine() {
        print(Config.getLineSeparator());
    }

    @Override
    public String readLine(String prompt) throws IOException {
        return readLine(prompt, null);
    }

    /**
     * Prompt a user. The complexity of this method is implied by the Ctrl-C
     * handling. When Ctrl-C occurs, the Exit hook will call this.close that
     * interrupts the thread calling this operation.<p>
     * We have 2 cases.
     * <p>
     * 1) prompting prior to have started the console: - Must start a new
     * connection. - Make it non blocking. - Wait on latch to resync and to
     * catch Thread.interrupt.
     * <p>
     * 2) prompting after to have started the console: - No need to open the
     * connection. - Wait on latch to resync and to catch Thread.interrupt.
     *
     * @param prompt
     * @param mask
     * @return
     * @throws IOException
     */
    @Override
    public String readLine(String prompt, Character mask) throws IOException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Prompt {0} mask {1}", new Object[]{prompt, mask});
        }
        // Keep a reference on the caller thread in case Ctrl-C is pressed
        // and thread needs to be interrupted.
        readingThread = Thread.currentThread();
        try {
            if (!started) {
                // That is the case of the CLI connecting prior to start the terminal.
                // No Terminal waiting in Main thread yet.
                // We are opening the connection to the terminal until we have read
                // something from prompt.
                return promptFromNonStartedConsole(prompt, mask);
            } else {
                return promptFromStartedConsole(prompt, mask);
            }
        } finally {
            readingThread = null;
        }
    }

    private String promptFromNonStartedConsole(String prompt, Character mask) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Not started");
        }
        String[] out = new String[1];
        if (connection.suspended()) {
            connection.awake();
        }
        connection.setSignalHandler(exitHandler);
        readline.readline(connection, new Prompt(prompt, mask), newLine -> {
            out[0] = newLine;
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Got some input");
            }
            // We must call stopReading to stop reading from terminal
            // and release this thread.
            connection.stopReading();
        });
        try {
            connection.openBlockingInterruptable();
        } catch (InterruptedException ex) {
            interrupted(ex);
        }
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Done for prompt");
        }
        return out[0];
    }

    private String promptFromStartedConsole(String prompt, Character mask) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Prompt {0} mask {1}", new Object[]{prompt, mask});
        }
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
        connection.setSignalHandler(interruptHandler);
        // We can't reuse readline when prompting from a command.
        Readline rd = new Readline();
        rd.readline(connection, new Prompt(prompt, mask), newLine -> {
            out[0] = newLine;
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Got some input");
            }
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            interrupted(ex);
        }
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Done for prompt");
        }
        return out[0];
    }

    private void interrupted(InterruptedException ex) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Thread interrupted");
        }
        // Ctrl-C, interrupt and throw exception to cancel prompting.
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
    }

    @Override
    public int getTerminalWidth() {
        return getWidth();
    }

    @Override
    public int getTerminalHeight() {
        return getHeight();
    }

    private int getHeight() {
        if (connection instanceof TerminalConnection) {
            return ((TerminalConnection) connection).getTerminal().getSize().getHeight();
        }
        return connection.size().getHeight();
    }

    private int getWidth() {
        if (connection instanceof TerminalConnection) {
            return ((TerminalConnection) connection).getTerminal().getSize().getWidth();
        }
        return connection.size().getWidth();
    }

    @Override
    public void start() throws IOException {
        if (closed) {
            throw new IllegalStateException("Console has already been closed");
        }
        if (!started) {
            startThread = Thread.currentThread();
            started = true;
            loop();
            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Started in thread {0}. Waiting...",
                        startThread.getName());
            }
            try {
                connection.openBlockingInterruptable();
            } catch (InterruptedException ex) {
                // OK leaving
            }
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Leavinig console");
            }
        } else if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Already started");
        }
    }

    private void loop() {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Set a readline callback with prompt {0}", prompt);
        }
        // Console could have been closed during a command execution.
        if (!closed) {
            interrupted = false;
            connection.setSignalHandler(exitHandler);
            readline.readline(connection, new Prompt(prompt), line -> {
                // All commands can lead to prompting the user. So require
                // to be executed in a dedicated thread.
                // This can happen if a security configuration occurs
                // on the remote server.
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER,
                            "Executing command {0} in a new thread.", line);
                }
                if (line == null || line.trim().length() == 0 || handleAlias(line)) {
                    loop();
                    return;
                }
                executor.submit(() -> {
                    try {
                        callback.accept(line);
                    } finally {
                        if (LOG.isLoggable(Level.FINER)) {
                            LOG.log(Level.FINER, "Done Executing command {0}",
                                    line);
                        }
                        loop();
                    }
                });
            }, completions, preProcessors, readlineHistory, null);
        }
    }

    private boolean handleAlias(String line) {
        if (line.startsWith("alias ") || line.equals("alias")) {
            String out = aliasManager.parseAlias(line.trim());
            if (out != null) {
                connection.write(out);
            }
            return true;
        } else if (line.startsWith("unalias ") || line.equals("unalias")) {
            String out = aliasManager.removeAlias(line.trim());
            if (out != null) {
                connection.write(out);
            }
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        if (!closed) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Stopping.");
            }
            closed = true;
            if (readingThread != null) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Interrupting reading thread");
                }
                readingThread.interrupt();
            }
            if (started) {
                readlineHistory.stop();
                aliasManager.persist();
            }
            executor.shutdown();
            connection.close();
        }
    }

    @Override
    public boolean running() {
        return started;
    }

    @Override
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
