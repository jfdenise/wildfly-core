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
package org.jboss.as.cli.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.OperationCommand.HandledRequest;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand.BatchResponseHandler;
import org.wildfly.core.cli.command.CommandRedirection;
import org.wildfly.core.cli.command.CommandRedirection.Registration;

/**
 *
 * @author jfdenise
 */
// XXX JFDENISE, is public for now, will be package when moved to right package
public class CliCommandContextImpl implements CliCommandContext {

    private final CommandContextImpl context;
    private Registration registration;

    public CliCommandContextImpl(CommandContextImpl context) {
        this.context = context;
    }

    @Override
    public boolean isDomainMode() {
        return context.isDomainMode();
    }

    public CommandContextImpl getCommandContextImpl() {
        return context;
    }

    public void addBatchOperation(ModelNode request, String originalInput,
            BatchResponseHandler handler) {
        HandledRequest req = new HandledRequest(request, handler == null ? null
                : (ModelNode step, OperationResponse response) -> {
                    try {
                        handler.handleResponse(step, response);
                    } catch (CommandException ex) {
                        throw new CommandLineException(ex);
                    }
                });
        context.addBatchOperation(originalInput, req);
    }

    public void handleOperation(DefaultCallbackHandler operationParser) throws CommandLineException {
        context.handleOperation(operationParser);
    }

    @Override
    public void connectController(String url) throws CommandLineException, InterruptedException {
        List<CommandLineException> holder = new ArrayList<>();
        Thread thr = new Thread(() -> {
            try {
                context.connectController(url);
            } catch (CommandLineException ex) {
                holder.add(ex);
            }
        });
        thr.start();
        try {
            // We will be possibly interrupted by console if Ctrl-C typed.
            thr.join();
        } catch (InterruptedException ex) {
            context.interruptConnect();
            thr.interrupt();
            throw ex;
        }
        if (!holder.isEmpty()) {
            throw holder.get(0);
        }
    }

    @Override
    public ModelControllerClient getModelControllerClient() {
        return context.getModelControllerClient();
    }

    @Override
    public void exit() {
        // Exit should be enough, but Aesh Console is not properly cleaned
        // in shutdown handler.
        context.terminateSession();
        System.exit(1);
    }

    @Override
    public CommandContext getLegacyCommandContext() {
        return context;
    }

    @Override
    public void setCurrentNodePath(OperationRequestAddress address) {
        context.setCurrentNodePath(address);
    }

    @Override
    public boolean isConnected() {
        return context.getModelControllerClient() != null;
    }

    @Override
    public void executeCommand(String line) throws CommandException {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return; // ignore comments
        }
        context.getConsole().executeCommand(line);
    }

    @Override
    public void registerRedirection(CommandRedirection redirection) throws CommandException {
        if (registration != null) {
            throw new CommandException("Another redirection is currently active.");
        }
        registration = new Registration() {
            @Override
            public boolean isActive() {
                return registration == this;
            }

            @Override
            public void unregister() throws CommandException {
                ensureActive();
                registration = null;
            }

            private void ensureActive() throws CommandException {
                if (!isActive()) {
                    throw new CommandException("The redirection is not registered any more.");
                }
            }

            @Override
            public CommandRedirection getRedirection() {
                return redirection;
            }

        };
        redirection.set(registration);
    }

    @Override
    public CommandRedirection getCommandRedirection() {
        if (registration != null) {
            return registration.getRedirection();
        }
        return null;
    }

    @Override
    public ModelNode execute(ModelNode mn, String description) throws CommandException, IOException {
        try {
            // XXX JFDENISE, for now delegates to the commandcontext that has a valid timeout handling.
            // This is only used by the of condition execution.
            return context.execute(mn, description);
        } catch (CommandLineException ex) {
            if (ex.getCause() instanceof CommandException) {
                throw (CommandException) ex.getCause();
            } else {
                throw new CommandException(ex);
            }
        }
    }

    public CommandExecutor getCommandExecutor() {
        return context.getCommandExecutor();
    }

    @Override
    public int getCommandTimeout() {
        return context.getCommandTimeout();
    }

    @Override
    public void setCommandTimeout(int timeout) {
        context.setCommandTimeout(timeout);
    }

    /**
     * Reset the timeout value.
     *
     * @param value The enumerated timeout reset value.
     */
    @Override
    public void resetCommandTimeout(TIMEOUT_RESET_VALUE value) {
        if (value == TIMEOUT_RESET_VALUE.CONFIG) {
            context.resetTimeout(CommandContext.TIMEOUT_RESET_VALUE.CONFIG);
        } else if (value == TIMEOUT_RESET_VALUE.DEFAULT) {
            context.resetTimeout(CommandContext.TIMEOUT_RESET_VALUE.DEFAULT);
        }
    }

    @Override
    public void println(String msg) {
        context.getConsole().println(msg);
    }
}
