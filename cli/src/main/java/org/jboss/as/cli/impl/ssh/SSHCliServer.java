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
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.impl.ReadlineConsole;

/**
 * Security configuration. If no security configuration, unsecured. If a name or
 * authorized_keys file path is provided security is enabled. As soon as
 * security is enabled, strict checks are applied to both mechanisms.
 *
 * The following is the SSH client authentication sequence:
 *
 * 1) If a public key exists for the client, ssh sends the public key. If an
 * authorized_keys file has been configured, the key is checked against it. If
 * the checks succeed, the client is authenticated. If no file has been
 * provided, the authentication fails.
 *
 * 2) If no public key or the public key authentication has failed, password
 * authentication is launched. User is prompted for a password. password+name
 * received by CLI are check against the configured user. If equals, client is
 * authenticated. If it fails, or no user configured, the authentication fails.
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
        this.sshConfig = sshConfig == null ? new SSHConfiguration.Builder().build() : sshConfig;
    }

    public void start() throws Exception {
        if (closed || started) {
            throw new IllegalStateException("SSHServer invalid state");
        }
        if (!started) {
            AbstractGeneratorHostKeyProvider hostKeyProvider
                    = new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath());
            hostKeyProvider.setAlgorithm("RSA");
            PasswordAuthenticator auth = (username, password, session) -> {
                System.out.println("Clear password for " + username);
                if (shouldCheck()) {
                    return username.equals(sshConfig.getAuthUserName()) && password.equals(sshConfig.getAuthPassword());
                }
                return true;
            };
            PublickeyAuthenticator authKey = (username, key, session) -> {
                System.out.println("Public key for " + username);
                if (shouldCheck()) {
                    if (sshConfig.getAuthorizedKeys() != null) {
                        return new AuthorizedKeysAuthenticator(sshConfig.getAuthorizedKeys()).authenticate(username, key, session);
                    } else {
                        return false;
                    }
                }
                return true;
            };
            NettySshTtyBootstrap bootstrap = new NettySshTtyBootstrap().
                    setPort(sshConfig.getPort()).
                    setPasswordAuthenticator(auth).
                    setPublicKeyAuthenticator(authKey).
                    setHost(sshConfig.getAdress()).
                    setKeyPairProvider(hostKeyProvider);
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
            System.out.println("SSH started on " + sshConfig.getAdress() + ":" + sshConfig.getPort());
            if ((sshConfig.getAuthPassword() == null || sshConfig.getAuthUserName() == null) && sshConfig.getAuthorizedKeys() == null) {
                System.out.println("WARNING: Running with no authentication enabled.");
            }
            if (sshConfig.getAuthPassword() != null && sshConfig.getAuthUserName() != null) {
                System.out.println("SSH clear text password authentication enabled");
            } else {
                System.out.println("SSH clear text password authentication disabled");
            }
            if (sshConfig.getAuthorizedKeys() != null) {
                System.out.println("SSH public key authentication enabled");
            } else {
                System.out.println("SSH public key authentication disabled");
            }
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

    /**
     * - Server has no user nor authorized keys path ==> unsecure
     *
     * - Server is configured with user Auth/Password, no authorized keys file.
     * ==> secured whatever the mechanism. Only this user. Users that can send     * a public key shouldn't establish a connection!
     *
     * - Server is configured with authorized keys ==> Only with public keys are
     * accepted.
     *
     * - Server is configured with both, both can connect.
     *
     * @return
     */
    private boolean shouldCheck() {
        return sshConfig.getAuthUserName() != null || sshConfig.getAuthorizedKeys() != null;
    }

}
