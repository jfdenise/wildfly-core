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
package org.jboss.as.cli.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "display", description = "")
public class AttachmentDisplayCommand implements Command<CliCommandInvocation>, BatchCompliantCommand {

    public static class OperationCompleter implements OptionCompleter<CliCompleterInvocation> {

        private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(false);

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            List<String> candidates = new ArrayList<>();
            String buff = completerInvocation.getGivenCompleteValue();
            OperationRequestAddress address = completerInvocation.getCommandContext().
                    getLegacyCommandContext().getCurrentNodePath();
            if (buff == null || buff.trim().isEmpty()) {
                candidates.add(":");
                if (address == null || address.isEmpty()) {
                    candidates.add("/");
                }
            } else {
                parsedCmd.reset();
                try {
                    parsedCmd.parse(address,
                            buff, false,
                            completerInvocation.getCommandContext().getLegacyCommandContext());
                } catch (CommandFormatException ex) {
                    // XXX OK.
                    return;
                }
                int offset = OperationRequestCompleter.INSTANCE.
                        complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                                parsedCmd,
                                buff,
                                0, candidates);
                completerInvocation.setOffset(buff.length() - offset);
            }
            Collections.sort(candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setAppendSpace(false);
        }
    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(hasValue = true, completer = OperationCompleter.class)
    private String operation;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("attachment display"));
            return CommandResult.SUCCESS;
        }
        final ModelControllerClient client = commandInvocation.getCommandContext().getModelControllerClient();
        OperationResponse response;
        ModelNode request;
        try {
            request = buildRequest(commandInvocation.getCommandContext());
        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        }
        OperationBuilder builder = new OperationBuilder(request, true);
        try {
            response = client.executeOperation(builder.build(),
                    OperationMessageHandler.DISCARD);
        } catch (Exception e) {
            throw new CommandException("Failed to perform operation: "
                    + e.getLocalizedMessage());
        }
        if (!Util.isSuccess(response.getResponseNode())) {
            throw new CommandException(Util.getFailureDescription(response.getResponseNode()));
        }

        buildHandler(commandInvocation.getCommandContext()).
                handleResponse(response.getResponseNode(), response);

        return CommandResult.SUCCESS;
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(CliCommandContext commandContext, Attachments attachments) throws CommandException {
        AttachmentResponseHandler handler = buildHandler(commandContext);
        return (ModelNode step, OperationResponse response) -> {
            handler.handleResponse(step, response);
        };
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
        return context.getLegacyCommandContext().buildRequest(operation);
    }

    AttachmentResponseHandler buildHandler(CliCommandContext commandContext) {
        return new AttachmentResponseHandler((String t) -> {
            commandContext.println(t);
        }, null, false, false);
    }
}
