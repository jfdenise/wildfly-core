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

import java.io.File;
import java.util.function.Consumer;
import org.aesh.terminal.Connection;
import org.aesh.terminal.ssh.netty.NettySshTtyBootstrap;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.impl.ReadlineConsole;

/**
 *
 * @author jdenise@redhat.com
 */
public class SSHCliServer {

    private final SSHConfiguration sshConfig;
    private final CommandContextConfiguration ctxConfig;
    private boolean closed;
    private boolean started;

    public SSHCliServer(CommandContextConfiguration ctxConfig, SSHConfiguration sshConfig) {
        this.ctxConfig = ctxConfig;
        this.sshConfig = sshConfig;
    }

    public void start() throws Exception {
        if (closed || started) {
            throw new IllegalStateException("SSHServer in nvalid state");
        }
        if (!started) {
            AbstractGeneratorHostKeyProvider hostKeyProvider
                    = new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath());
            hostKeyProvider.setAlgorithm("RSA");
            NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap().
                    setPort(5000).
                    setHost("localhost")
                    .setKeyPairProvider(hostKeyProvider) //.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("/tmp/mysample").toPath()))
                    ;
            Consumer<Connection> consumer = (connection) -> {
                System.out.println("New connection");
                // Need a new console.
                SSHConsoleFactory factory = (ReadlineConsole.Settings settings) -> {
                    return new SSHReadlineConsole(settings, connection);
                };
                try {
                    // Need a new CommandContext.
                    CommandContextImpl ctx = new CommandContextImpl(ctxConfig, factory);
                    ctx.interact();
                    System.out.println("End of connection");
                } catch (CliInitializationException ex) {
                    throw new RuntimeException(ex);
                }
            };
            Consumer<Throwable> doneHandler = (ex) -> {
                if (ex != null) {
                    throw new RuntimeException(ex);
                }
            };
            bootstrap.start(consumer, doneHandler);
            System.out.println("SSH started on localhost:5000 ");
            try {
                synchronized (SSHCliServer.class) {
                    SSHCliServer.class.wait();
                }
            } catch (InterruptedException ex) {
                Thread.interrupted();
                throw new RuntimeException(ex);
            }
        }
    }
}
