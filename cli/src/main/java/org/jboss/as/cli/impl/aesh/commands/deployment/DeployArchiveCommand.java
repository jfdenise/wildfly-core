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

import org.jboss.as.cli.impl.aesh.commands.deployment.security.CommandWithPermissions;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.Permissions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.Attachments.ConsumeListener;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.wildfly.core.cli.command.aesh.activator.HideOptionActivator;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.AccessRequirements;
import org.jboss.as.cli.impl.aesh.commands.security.ControlledCommandActivator;
import org.wildfly.core.cli.command.aesh.FileConverter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.spi.MountHandle;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.FileCompleter;
import org.jboss.as.cli.impl.aesh.commands.deprecated.LegacyBridge;

/**
 * XXX jfdenise, all fields are public to be accessible from legacy view. To be
 * made private when removed.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deploy-archive", description = "", activator = ControlledCommandActivator.class)
public class DeployArchiveCommand extends CommandWithPermissions implements Command<CLICommandInvocation>, BatchCompliantCommand, LegacyBridge {

    private static final String CLI_ARCHIVE_SUFFIX = ".cli";

    @Deprecated
    @Option(hasValue = false, activator = HideOptionActivator.class)
    private boolean help;

    @Option(hasValue = true, required = false)
    public String script;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',',
            completer = FileCompleter.class, converter = FileConverter.class)
    public List<File> file;

    public DeployArchiveCommand(CommandContext ctx, Permissions permissions) {
        super(ctx, AccessRequirements.deployArchiveAccess(permissions), permissions);
    }

    @Deprecated
    public DeployArchiveCommand(CommandContext ctx) {
        this(ctx, null);
    }

    protected String getAction() {
        return "deploy-archive";
    }

    protected String getDefaultScript() {
        return "deploy.scr";
    }

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("deployment " + getAction()));
            return CommandResult.SUCCESS;
        }
        CommandContext ctx = commandInvocation.getCommandContext();
        return execute(ctx);
    }

    public CommandResult execute(CommandContext ctx) throws CommandException {
        checkArgument();

        Attachments attachments = new Attachments();
        try {
            ModelNode request = buildRequest(ctx,
                    attachments);
            OperationBuilder op = new OperationBuilder(request, true);
            for (String f : attachments.getAttachedFiles()) {
                op.addFileAsAttachment(new File(f));
            }
            ModelNode result;
            try (Operation operation = op.build()) {
                result = ctx.getModelControllerClient().execute(operation);
            }
            if (!Util.isSuccess(result)) {
                throw new CommandException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandException("Failed to deploy archive", e);
        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        } finally {
            attachments.done();
        }
        return CommandResult.SUCCESS;

    }

    private void checkArgument() throws CommandException {
        if (file == null || file.isEmpty()) {
            throw new CommandException("No archive file");
        }
        File f = file.get(0);
        if (!f.exists()) {
            throw new CommandException("Path " + f.getAbsolutePath()
                    + " doesn't exist.");
        }
        if (!isCliArchive(f)) {
            throw new CommandException("Not a CLI archive " + f.getAbsolutePath());
        }
    }

    @Override
    public ModelNode buildRequest(CommandContext context)
            throws CommandFormatException {
        return buildRequest(context, null);
    }

    /**
     * null attachments means that the command is in a batch, non null means
     * command executed.
     *
     * Inside a batch, the attachments must be added to the existing batch and
     * NOT to the temporary batch created to build the composite request.
     * Outside of a batch, the attachments MUST be added to the passed non null
     * attachments.
     *
     * @param context
     * @param attachments
     * @return
     * @throws CommandFormatException
     */
    @Override
    public ModelNode buildRequest(CommandContext context, Attachments attachments)
            throws CommandFormatException {
        CommandContext ctx = context;
        File f = file.get(0);
        TempFileProvider tempFileProvider;
        MountHandle root;
        try {
            String name = "cli-" + System.currentTimeMillis();
            tempFileProvider = TempFileProvider.create(name,
                    Executors.newSingleThreadScheduledExecutor(), true);
            root = extractArchive(f, tempFileProvider, name);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OperationFormatException("Unable to extract archive '"
                    + f.getAbsolutePath() + "' to temporary location");
        }
        ConsumeListener cl = (a) -> {
            VFSUtils.safeClose(root, tempFileProvider);
        };
        if (attachments != null) {
            attachments.addConsumer(cl);
        }
        final File currentDir = ctx.getCurrentDir();
        ctx.setCurrentDir(root.getMountSource());
        String holdbackBatch = activateNewBatch(ctx);

        try {
            if (script == null) {
                script = getDefaultScript();
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
                    context.handle(line);
                    line = reader.readLine();
                }
            } catch (FileNotFoundException e) {
                throw new CommandFormatException("ERROR: script '"
                        + script + "' not found.");
            } catch (IOException e) {
                throw new CommandFormatException("Failed to read the next command from "
                        + scriptFile.getName() + ": " + e.getMessage(), e);
            } catch (CommandLineException ex) {
                throw new CommandFormatException(ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
            return ctx.getBatchManager().getActiveBatch().toRequest();
        } catch (CommandFormatException cfex) {
            cl.consumed(attachments);
            throw cfex;
        } finally {
            // reset current dir in context
            ctx.setCurrentDir(currentDir);
            discardBatch(ctx, holdbackBatch, attachments, cl);
        }
    }

    public static boolean isCliArchive(File f) {
        return f != null && !f.isDirectory()
                && f.getName().endsWith(CLI_ARCHIVE_SUFFIX);
    }

    static MountHandle extractArchive(File archive,
            TempFileProvider tempFileProvider, String name) throws IOException {
        return ((MountHandle) VFS.mountZipExpanded(archive, VFS.getChild(name),
                tempFileProvider));
    }

    static String activateNewBatch(CommandContext ctx) {
        String currentBatch = null;
        BatchManager batchManager = ctx.getBatchManager();
        Attachments attachments = null;
        if (batchManager.isBatchActive()) {
            Batch current = batchManager.getActiveBatch();
            attachments = current.getAttachments();
            currentBatch = "batch" + System.currentTimeMillis();
            batchManager.holdbackActiveBatch(currentBatch);
        }
        batchManager.activateNewBatch();
        Batch archiveBatch = batchManager.getActiveBatch();
        // transfer all attachments to new batch in order to have proper index
        // computation.
        if (attachments != null) {
            for (String f : attachments.getAttachedFiles()) {
                archiveBatch.getAttachments().addFileAttachment(f);
            }
        }
        return currentBatch;
    }

    static void discardBatch(CommandContext ctx, String holdbackBatch, Attachments attachments, ConsumeListener listener) {
        BatchManager batchManager = ctx.getBatchManager();
        // Get the files attached by the CLI script.
        // Must then add them to the passed attachemnts if non null.
        // If null, then we should have an heldback batch to which we need to add them
        Attachments archiveAttachments = batchManager.getActiveBatch().getAttachments();
        if (attachments != null) {
            for (String f : archiveAttachments.getAttachedFiles()) {
                attachments.addFileAttachment(f);
            }
        }
        batchManager.discardActiveBatch();
        if (holdbackBatch != null) {
            batchManager.activateHeldbackBatch(holdbackBatch);
            Attachments activeAttachments = batchManager.getActiveBatch().getAttachments();
            // We must transfer all attachments that have been added in addition to the one
            // that have been transfered when creating the archive batch.
            for (int i = activeAttachments.getAttachedFiles().size();
                    i < archiveAttachments.getAttachedFiles().size(); i++) {
                activeAttachments.addFileAttachment(archiveAttachments.getAttachedFiles().get(i));
            }
            activeAttachments.addConsumer(listener);
        }

    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(CommandContext commandContext, Attachments attachments) {
        return null;
    }
}