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
package org.jboss.as.cli.impl.aesh.commands.deployment;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.aesh.commands.activator.ControlledCommandActivator;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentActivators.NameActivator;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentActivators.UnmanagedActivator;
import org.wildfly.core.cli.command.aesh.FileConverter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.aesh.FileCompleter;

/**
 * XXX jfdenise, all fields are public to be accessible from legacy view. To be
 * made private when removed.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deploy-file", description = "", activator = ControlledCommandActivator.class)
public class DeploymentFileCommand extends DeploymentContentSubCommand implements DMRCommand {

    @Option(hasValue = false, activator = UnmanagedActivator.class, required = false)
    public boolean unmanaged;

    @Option(hasValue = true, required = false, completer
            = DeploymentRedeployCommand.NameCompleter.class,
            activator = NameActivator.class)
    public String name;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',',
            completer = FileCompleter.class, converter = FileConverter.class)
    public List<File> file;

    public DeploymentFileCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    protected void checkArgument() throws CommandException {
        if (file == null || file.isEmpty()) {
            throw new CommandException("No deployment file");
        }
        File f = file.get(0);
        if (!f.exists()) {
            throw new CommandException("Path " + f.getAbsolutePath()
                    + " doesn't exist.");
        }
        if (!unmanaged && f.isDirectory()) {
            throw new CommandException(f.getAbsolutePath() + " is a directory.");
        }
    }

    @Override
    protected String getName() {
        if (name != null) {
            return name;
        }
        File f = file.get(0);
        return f.getName();
    }

    @Override
    protected void addContent(CommandContext context, ModelNode content) throws OperationFormatException {
        File f = file.get(0);
        if (unmanaged) {
            content.get(Util.PATH).set(f.getAbsolutePath());
            content.get(Util.ARCHIVE).set(f.isFile());
        } else if (context.getBatchManager().isBatchActive()) {
            Attachments attachments = context.getBatchManager().getActiveBatch().getAttachments();
            int index = attachments.addFileAttachment(f.getAbsolutePath());
            content.get(Util.INPUT_STREAM_INDEX).set(index);
        } else {
            content.get(Util.INPUT_STREAM_INDEX).set(0);
        }
    }

    @Override
    protected List<String> getServerGroups(CommandContext ctx)
            throws CommandFormatException {
        return DeploymentCommand.getServerGroups(ctx, ctx.getModelControllerClient(),
                allServerGroups, serverGroups, file.get(0));
    }

    @Override
    protected String getCommandName() {
        return "deploy-file";
    }

    @Override
    protected ModelNode execute(CommandContext ctx, ModelNode request)
            throws IOException {
        ModelNode result;
        if (!unmanaged) {
            OperationBuilder op = new OperationBuilder(request);
            op.addFileAsAttachment(file.get(0));
            request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
            try (Operation operation = op.build()) {
                result = ctx.getModelControllerClient().execute(operation);
            }
        } else {
            result = ctx.getModelControllerClient().execute(request);
        }
        return result;
    }
}
