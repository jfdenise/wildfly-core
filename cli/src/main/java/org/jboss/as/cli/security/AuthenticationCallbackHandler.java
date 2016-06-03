/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli.security;

import org.jboss.as.protocol.GeneralTimeoutHandler;
import org.jboss.sasl.callback.DigestHashCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.SaslException;
import java.io.IOException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.Console;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 * @author jdenise@redhat.com
 */
public class AuthenticationCallbackHandler implements CallbackHandler {

    // After the CLI has connected the physical connection may be re-established numerous times.
    // for this reason we cache the entered values to allow for re-use without pestering the end
    // user.
    private String realm = null;
    private boolean realmShown = false;

    private String username;
    private char[] password;
    private String digest;
    private final GeneralTimeoutHandler timeoutHandler;
    private Console console;
    private Thread callbackThread;

    public AuthenticationCallbackHandler(GeneralTimeoutHandler timeoutHandler,
            String username, char[] password,
            Console console) {
        // A local cache is used for scenarios where no values are specified on the command line
        // and the user wishes to use the connect command to establish a new connection.
        this.timeoutHandler = timeoutHandler;
        this.username = username;
        this.password = password;
        this.console = console;
    }

    public AuthenticationCallbackHandler(GeneralTimeoutHandler timeoutHandler,
            String username, String digest) {
        this.username = username;
        this.digest = digest;
        this.timeoutHandler = timeoutHandler;
    }

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        try {
            timeoutHandler.suspendAndExecute(() -> {
                try {
                    dohandle(callbacks);
                } catch (CommandLineException | IOException |
                        UnsupportedCallbackException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException ex) {
                    // Never swallow it
                    Thread.currentThread().interrupt();
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof UnsupportedCallbackException) {
                throw (UnsupportedCallbackException) e.getCause();
            }
            throw e;
        }

    }

    public void interruptConnectCallback() {
        if (callbackThread != null) {
            callbackThread.interrupt();
            callbackThread = null;
        }
    }

    private void dohandle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException, InterruptedException, CommandLineException {
        // Special case for anonymous authentication to avoid prompting user for their name.
        if (callbacks.length == 1 && callbacks[0] instanceof NameCallback) {
            ((NameCallback) callbacks[0]).setName("anonymous CLI user");
            return;
        }
        callbackThread = Thread.currentThread();
        try {
            for (Callback current : callbacks) {
                if (current instanceof RealmCallback) {
                    RealmCallback rcb = (RealmCallback) current;
                    String defaultText = rcb.getDefaultText();
                    realm = defaultText;
                    rcb.setText(defaultText); // For now just use the realm suggested.
                } else if (current instanceof RealmChoiceCallback) {
                    throw new UnsupportedCallbackException(current, "Realm choice not currently supported.");
                } else if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    if (username == null) {
                        showRealm();
                        username = console.promptForInput("Username: ", null);
                        if (username == null || username.length() == 0) {
                            throw new SaslException("No username supplied.");
                        }
                    }
                    ncb.setName(username);
                } else if (current instanceof PasswordCallback && digest == null) {
                    // If a digest had been set support for PasswordCallback is disabled.
                    PasswordCallback pcb = (PasswordCallback) current;
                    if (password == null) {
                        showRealm();
                        String temp = console.promptForInput("Password: ", '\u0000');
                        if (temp != null) {
                            password = temp.toCharArray();
                        }
                    }
                    pcb.setPassword(password);
                } else if (current instanceof DigestHashCallback && digest != null) {
                    // We don't support an interactive use of this callback so it must have been set in advance.
                    DigestHashCallback dhc = (DigestHashCallback) current;
                    dhc.setHexHash(digest);
                } else {
                    console.error("Unexpected Callback " + current.getClass().getName());
                    throw new UnsupportedCallbackException(current);
                }
            }
        } finally {
            callbackThread = null;
        }
    }

    private void showRealm() {
        if (realmShown == false && realm != null) {
            realmShown = true;
            console.println("Authenticating against security realm: " + realm);
        }
    }
}
