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
package org.jboss.as.cli.command.batch;

import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.command.CliCommandInvocation;
import org.jboss.as.cli.command.CommandUtil;
import org.jboss.as.cli.aesh.completer.HeadersCompleter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.command.DMRCommand;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "run", description = "")
public class BatchRunCommand implements Command<CliCommandInvocation>, DMRCommand {

    @Option(name = "help", hasValue = false)
    private boolean help;

    @Option(name = "verbose", hasValue = false)
    private boolean verbose;

    @Option(name = "headers", completer = HeadersCompleter.class,
            converter = HeadersConverter.class)
    private ModelNode headers;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (help) {
            commandInvocation.getCommandContext().printLine("Aesh should have hooks for help!");
            return null;
        }
        boolean failed = false;
        ModelNode response;
        CommandContext context = commandInvocation.getCommandContext();
        try {
            final ModelNode request = buildRequest(context);
            if (headers != null) {
                request.get(Util.OPERATION_HEADERS).set(headers);
            }
            final ModelControllerClient client = context.getModelControllerClient();
            try {
                response = client.execute(request);
            } catch (Exception e) {
                throw new CommandFormatException("Failed to perform operation: "
                        + e.getLocalizedMessage());
            }
            if (!Util.isSuccess(response)) {
                throw new CommandFormatException(Util.getFailureDescription(response));
            }
        } catch (CommandFormatException e) {
            failed = true;
            throw new RuntimeException("The batch failed with the following error "
                    + "(you are remaining in the batch editing mode to have a "
                    + "chance to correct the error) " + e.getMessage(), e);
        } catch (CommandLineException e) {
            failed = true;
            throw new RuntimeException("The batch failed with the following error "
                    + e.getMessage(), e);
        } finally {
            if (!failed) {
                if (context.getBatchManager().isBatchActive()) {
                    context.getBatchManager().discardActiveBatch();
                }
            }
        }
        if (verbose) {
            commandInvocation.getShell().out().println(response.toString());
        } else {
            commandInvocation.getShell().out().println("The batch executed successfully");
            CommandUtil.displayResponseHeaders(commandInvocation.getShell(), response);
        }
        return null;
    }

    public ModelNode newRequest(CommandContext context) throws CommandLineException {
        final BatchManager batchManager = context.getBatchManager();
        if (batchManager.isBatchActive()) {
            final Batch batch = batchManager.getActiveBatch();
            List<BatchedCommand> currentBatch = batch.getCommands();
            if (currentBatch.isEmpty()) {
                batchManager.discardActiveBatch();
                throw new CommandLineException("The batch is empty.");
            }
            ModelNode request = batch.toRequest();
            return request;
        }
        // XXX JF DENISE, HIDE WHEN NOT IN BATCH MODE
        throw new CommandLineException("Command can be executed only in the batch mode.");
    }

    @Override
    public ModelNode buildRequest(CommandContext context) throws CommandFormatException {
        try {
            return newRequest(context);
        } catch (CommandLineException ex) {
            throw new CommandFormatException(ex);
        }
    }

}
