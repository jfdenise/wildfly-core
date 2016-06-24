/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.core.cli;

import java.io.IOException;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.parsing.operation.OperationFormat;
import org.jboss.dmr.ModelNode;

/**
 * This class allows to send CLI commands to WildFly server.
 *
 * @author jdenise@redhat.com
 */
public final class WildFlyCLI {

    /**
     * The Result class provides all information about an executed CLI command.
     */
    public class Result {

        private final String cliCommand;
        private ModelNode request;
        private ModelNode response;

        private boolean isSuccess = false;

        Result(String cliCommand, ModelNode request, ModelNode response) {
            this.cliCommand = cliCommand;
            this.request = request;
            this.response = response;
            this.isSuccess = response.get("outcome").asString().equals("success");
        }

        Result(String cliCommand) {
            this.cliCommand = cliCommand;
            this.isSuccess = true;
        }

        /**
         * Return the original command as a String.
         *
         * @return The original CLI command.
         */
        public String getCliCommand() {
            return this.cliCommand;
        }

        /**
         * If the command resulted in a server-side operation, return the
         * ModelNode representation of the operation.
         *
         * @return The request as a ModelNode, or <code>null</code> if this was
         * a local command.
         */
        public ModelNode getRequest() {
            return this.request;
        }

        /**
         * If the command resulted in a server-side operation, return the
         * ModelNode representation of the response.
         *
         * @return The server response as a ModelNode, or <code>null</code> if
         * this was a local command.
         */
        public ModelNode getResponse() {
            return this.response;
        }

        /**
         * Return true if the command was successful. For a server-side
         * operation, this is determined by the outcome of the operation on the
         * server side.
         *
         * @return <code>true</code> if the command was successful,
         * <code>false</code> otherwise.
         */
        public boolean isSuccess() {
            return this.isSuccess;
        }
    }
    private final CommandContextConfiguration.Builder builder;
    private CommandContextImpl context;
    // This parsedCommand is required in order to determinate if the command
    // is an operation or a command
    private final DefaultCallbackHandler parser
            = new DefaultCallbackHandler(true);

    /**
     * Create a Command line with default arguments.
     */
    public WildFlyCLI() {
        this(new WildFlyCLIConfiguration.Builder());
    }

    /**
     * Create a Command line with a configuration.
     *
     * @param configBuilder The configuration builder.
     */
    public WildFlyCLI(WildFlyCLIConfiguration.Builder configBuilder) {
        builder = configBuilder.getBuilder();
    }

    /**
     * Terminate the cli instance.
     */
    public void terminate() {
        if (context != null) {
            context.terminateSession();
        }
    }

    /**
     * Execute a WildFly CLI tool command.
     *
     * @param cmd The command to execute.
     * @return The command execution result.
     * @throws RuntimeException if something went wrong.
     */
    public Result execute(String cmd) {
        try {
            if (context == null) {
                context = (CommandContextImpl) CommandContextFactory.getInstance().newCommandContext(builder.build());
            } else if (context.isTerminated()) {
                throw new RuntimeException("CLI instance is terminated, can't execute command");
            }

            // This is required in order to have low level operation put in batch
            // This would not be required if we were not returning the response.
            if (context.isWorkflowMode() || context.isBatchMode()) {
                context.getConsole().executeCommand(cmd);
                return new Result(cmd);
            }
            parser.reset();
            // The intent here is to return a Response when this is doable.
            parser.parse(context.getCurrentNodePath(), cmd, context);
            if (parser.getFormat() == OperationFormat.INSTANCE) {
                ModelNode request = context.buildRequest(cmd);
                ModelNode response = context.getModelControllerClient().execute(request);
                return new Result(cmd, request, response);
            } else {
                context.getConsole().executeCommand(cmd);
                return new Result(cmd);
            }
        } catch (CommandLineException cfe) {
            throw new IllegalArgumentException("Error handling command: "
                    + cmd, cfe);
        } catch (IOException ioe) {
            throw new IllegalStateException("Unable to send command "
                    + cmd + " to server.", ioe);
        }
    }
}
