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
package org.jboss.as.cli.impl.ssh;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.aesh.readline.Prompt;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Signal;
import org.jboss.as.cli.impl.ReadlineConsole;

/**
 *
 * @author jdenise@redhat.com
 */
public class SSHReadlineConsole extends ReadlineConsole {
    private final Connection connection;

    public SSHReadlineConsole(ReadlineConsole.Settings settings, Connection connection) throws IOException {
        super(settings);
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    protected void initializeConnection() throws IOException {
    }

    @Override
    protected String doReadLine(Prompt prompt) throws InterruptedException, IOException {
        return promptFromStartedConsole(prompt);
    }

    private String promptFromStartedConsole(Prompt prompt) throws InterruptedException, IOException {
        initializeConnection();
        String[] out = new String[1];
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
    public void doStart() throws IOException {
        loop();
        connection.openBlocking();
    }

}
