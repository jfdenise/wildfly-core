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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.NoBatchActivator;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.aesh.converter.FileConverter;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "run-file", description = "", activator = NoBatchActivator.class)
public class BatchRunFileCommand extends BatchRunCommand {

    // XXX JFDENISE AESH-401
    @Arguments(converter = FileConverter.class,
            completer = FileCompleter.class) // required = true
    public List<File> arg;

    @Override
    public ModelNode newRequest(CliCommandContext ctx) throws CommandLineException {
        final BatchManager batchManager = ctx.getLegacyCommandContext().getBatchManager();
        if (batchManager.isBatchActive()) {
            throw new CommandLineException("Batch already active, can't start new batch");
        }

        if(arg == null || arg.isEmpty()) {
            throw new CommandLineException("No batch file to run");
        }

        File file = arg.get(0);

        if (file == null || !file.exists()) {
            throw new CommandLineException("File " + file.getAbsolutePath() + " does not exist.");
        }

        final File currentDir = ctx.getLegacyCommandContext().getCurrentDir();
        final File baseDir = file.getParentFile();
        if (baseDir != null) {
            ctx.getLegacyCommandContext().setCurrentDir(baseDir);
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            batchManager.activateNewBatch();
            while (line != null) {
                ctx.executeCommand(line);
                line = reader.readLine();
            }
            final ModelNode request = batchManager.getActiveBatch().toRequest();
            if (headers != null) {
                request.get(Util.OPERATION_HEADERS).set(headers);
            }
            return request;
        } catch (IOException e) {
            throw new CommandLineException("Failed to read file "
                    + file.getAbsolutePath(), e);
        } catch (CommandException e) {
            throw new CommandLineException("Failed to create batch from "
                    + file.getAbsolutePath(), e);
        } finally {
            if (baseDir != null) {
                ctx.getLegacyCommandContext().setCurrentDir(currentDir);
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        try {
        return super.execute(commandInvocation);
        } finally {
             BatchManager batchManager = commandInvocation.getCommandContext().
                     getLegacyCommandContext().getBatchManager();
            batchManager.discardActiveBatch();
        }
    }

}
