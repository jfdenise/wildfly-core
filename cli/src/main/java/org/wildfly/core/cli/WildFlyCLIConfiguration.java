/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.wildfly.core.cli;

/**
 * WildFly CLI configuration.
 *
 * @author jdenise@redhat.com
 */
import java.io.InputStream;
import java.io.OutputStream;
import org.jboss.as.cli.impl.CommandContextConfiguration;

/**
 * WildFly Configuration.
 *
 * @author jdenise@redhat.com
 */
// This interface has been added due to the fact that the
// CommandContextConfiguration is not a public API but is public and exposed in
// public API CommandContextFactory.
public interface WildFlyCLIConfiguration {

    public static final class Builder {

        private final CommandContextConfiguration.Builder builder
                = new CommandContextConfiguration.Builder();

        public Builder() {
        }

        // In order to convey the internal builder to the CliLauncher
        CommandContextConfiguration.Builder getBuilder() {
            return builder;
        }

        public WildFlyCLIConfiguration build() {
            return builder.build();
        }

        public Builder setController(String controller) {
            builder.setController(controller);
            return this;
        }

        public Builder setUsername(String username) {
            builder.setUsername(username);
            return this;
        }

        public Builder setPassword(char[] password) {
            builder.setPassword(password);
            return this;
        }

        public Builder setClientBindAddress(String clientBindAddress) {
            builder.setClientBindAddress(clientBindAddress);
            return this;
        }

        public Builder setConsoleInput(InputStream consoleInput) {
            builder.setConsoleInput(consoleInput);
            return this;
        }

        public Builder setConsoleOutput(OutputStream consoleOutput) {
            builder.setConsoleOutput(consoleOutput);
            return this;
        }

        public Builder setDisableLocalAuth(boolean disableLocalAuth) {
            builder.setDisableLocalAuth(disableLocalAuth);
            return this;
        }

        public Builder setConnectionTimeout(int connectionTimeout) {
            builder.setConnectionTimeout(connectionTimeout);
            return this;
        }

        public Builder setSilent(boolean silent) {
            builder.setSilent(silent);
            return this;
        }

        public Builder setErrorOnInteract(boolean errorOnInteract) {
            builder.setErrorOnInteract(errorOnInteract);
            return this;
        }
    }

    String getController();

    String getUsername();

    char[] getPassword();

    String getClientBindAddress();

    InputStream getConsoleInput();

    OutputStream getConsoleOutput();

    int getConnectionTimeout();

    boolean isDisableLocalAuth();

    boolean isSilent();

    Boolean isErrorOnInteract();
}
