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
package org.jboss.as.cli.command.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.command.AttachmentSaveCommand.FileActivator;
import org.jboss.as.cli.command.deployment.DeploymentActivators.NameActivator;
import org.jboss.as.cli.command.deployment.DeploymentActivators.UnmanagedActivator;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.spi.MountHandle;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deploy-file", description = "")
public class DeploymentFileCommand extends DeploymentContentSubCommand implements DMRCommand {

    private static final String CLI_ARCHIVE_SUFFIX = ".cli";

    @Option(hasValue = false, activator = UnmanagedActivator.class, required = false)
    protected boolean unmanaged;

    @Option(hasValue = true, required = false)
    protected String script;

    @Option(hasValue = true, required = false, completer
            = DeploymentNameCommand.NameCompleter.class,
            activator = NameActivator.class)
    protected String name;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',', activator = FileActivator.class,
            completer = FileCompleter.class)
    protected List<String> file;

    DeploymentFileCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    protected void checkArgument() throws CommandException {
        if (file == null || file.isEmpty()) {
            throw new CommandException("No deployment file");
        }
        File f = new File(file.get(0));
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
        File f = new File(file.get(0));
        return f.getName();
    }

    @Override
    protected void addContent(ModelNode content) throws OperationFormatException {
        File f = new File(file.get(0));
        if (unmanaged) {
            content.get(Util.PATH).set(f.getAbsolutePath());
            content.get(Util.ARCHIVE).set(f.isFile());
        } else {
            content.get(Util.INPUT_STREAM_INDEX).set(0);
        }
    }

    @Override
    protected List<String> getServerGroups(CommandContext ctx)
            throws CommandFormatException {
        return DeploymentCommand.getServerGroups(ctx, ctx.getModelControllerClient(),
                allServerGroups, serverGroups, new File(file.get(0)));
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
            op.addFileAsAttachment(new File(file.get(0)));
            request.get(Util.CONTENT).get(0).get(Util.INPUT_STREAM_INDEX).set(0);
            try (Operation operation = op.build()) {
                result = ctx.getModelControllerClient().execute(operation);
            }
        } else {
            result = ctx.getModelControllerClient().execute(request);
        }
        return result;
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context)
            throws CommandFormatException {
        CommandContext ctx = context.getLegacyCommandContext();
        File f = new File(file.get(0));
        boolean isArchive = isCliArchive(f);
        if (isArchive) {
            if (force) {
                throw new CommandFormatException("archive can't be used with --force");
            }
            if (disabled) {
                throw new CommandFormatException("archive can't be used with --disabled");
            }
            if (serverGroups != null || allServerGroups) {
                throw new OperationFormatException("--server-groups and --all-server-groups "
                        + " can't be used in combination with a CLI archive.");
            }

            TempFileProvider tempFileProvider;
            MountHandle root;
            try {
                tempFileProvider = TempFileProvider.create("cli",
                        Executors.newSingleThreadScheduledExecutor(), true);
                root = extractArchive(f, tempFileProvider);
            } catch (IOException e) {
                throw new OperationFormatException("Unable to extract archive '"
                        + f.getAbsolutePath() + "' to temporary location");
            }

            final File currentDir = ctx.getCurrentDir();
            ctx.setCurrentDir(root.getMountSource());
            String holdbackBatch = activateNewBatch(ctx);

            try {
                if (script == null) {
                    script = "deploy.scr";
                }

                File scriptFile = new File(ctx.getCurrentDir(), script);
                if (!scriptFile.exists()) {
                    throw new CommandFormatException("ERROR: script '"
                            + script + "' not found.");
                }

                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(scriptFile));
                    String line = reader.readLine();
                    while (!ctx.isTerminated() && line != null) {
                        ctx.handle(line);
                        line = reader.readLine();
                    }
                } catch (FileNotFoundException e) {
                    throw new CommandFormatException("ERROR: script '"
                            + script + "' not found.");
                } catch (IOException e) {
                    throw new CommandFormatException("Failed to read the next command from "
                            + scriptFile.getName() + ": " + e.getMessage(), e);
                } catch (CommandLineException e) {
                    throw new CommandFormatException(e.getMessage(), e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                }

                return ctx.getBatchManager().getActiveBatch().toRequest();
            } finally {
                // reset current dir in context
                ctx.setCurrentDir(currentDir);
                discardBatch(ctx, holdbackBatch);

                VFSUtils.safeClose(root, tempFileProvider);
            }
        }

        return super.buildRequest(context);
    }

    private boolean isCliArchive(File f) {
        return !(f == null || f.isDirectory()
                || !f.getName().endsWith(CLI_ARCHIVE_SUFFIX));
    }

    private MountHandle extractArchive(File archive,
            TempFileProvider tempFileProvider) throws IOException {
        return ((MountHandle) VFS.mountZipExpanded(archive, VFS.getChild("cli"),
                tempFileProvider));
    }

    private String activateNewBatch(CommandContext ctx) {
        String currentBatch = null;
        BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            currentBatch = "batch" + System.currentTimeMillis();
            batchManager.holdbackActiveBatch(currentBatch);
        }
        batchManager.activateNewBatch();
        return currentBatch;
    }

    private void discardBatch(CommandContext ctx, String holdbackBatch) {
        BatchManager batchManager = ctx.getBatchManager();
        batchManager.discardActiveBatch();
        if (holdbackBatch != null) {
            batchManager.activateHeldbackBatch(holdbackBatch);
        }
    }
}
