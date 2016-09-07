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
package org.wildfly.core.cli.command;

import java.io.IOException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky, jdenise
 */
public interface CliCommandContext {

    boolean isDomainMode();

    void connectController(String url)
            throws CommandLineException, InterruptedException;

    ModelControllerClient getModelControllerClient();

    void exit();

    CommandContext getLegacyCommandContext();

    void setCurrentNodePath(OperationRequestAddress get);

    boolean isConnected();

    void executeCommand(String line) throws CommandException;

    ModelNode execute(ModelNode mn, String description) throws CommandException, IOException;

    int getCommandTimeout();

    void setCommandTimeout(int timeout);

    /**
     * The command timeout reset value.
     */
    enum TIMEOUT_RESET_VALUE {
        CONFIG,
        DEFAULT;
    }

    /**
     * Reset the timeout value.
     *
     * @param value The enumerated timeout reset value.
     */
    void resetCommandTimeout(TIMEOUT_RESET_VALUE value);

    void registerRedirection(CommandRedirection redirection) throws CommandException;

    CommandRedirection getCommandRedirection();

    void println(String msg);
}
