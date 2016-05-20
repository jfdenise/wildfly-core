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
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.command.CliCommandInvocation;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.aesh.converter.FileConverter;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "load-file", description = "")
public class BatchLoadFileCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false)
    private boolean help;

    @Arguments(converter = FileConverter.class,
            completer = FileCompleter.class)
    private List<File> files;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (help) {
            commandInvocation.getCommandContext().printLine("Aesh should have hooks for help!");
            return null;
        }
        if (commandInvocation.getCommandContext().getBatchManager().isBatchActive()) {
            throw new RuntimeException("Can't start a new batch while in batch mode.");
        }
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("File name must be provided");
        }
        File file = files.get(0);
        return handle(commandInvocation, file);
    }

    // Should be private but package for BatchCommand to support Deprecated arg
    static CommandResult handle(CliCommandInvocation commandInvocation, File file) {
        if (commandInvocation.getCommandContext().getBatchManager().isBatchActive()) {
            throw new RuntimeException("Can't start a new batch while in batch mode.");
        }

        CommandContext ctx = commandInvocation.getCommandContext();
        BatchManager batchManager = ctx.getBatchManager();
        final File currentDir = ctx.getCurrentDir();
        final File baseDir = file.getParentFile();
        if (baseDir != null) {
            ctx.setCurrentDir(baseDir);
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            batchManager.activateNewBatch();
            final Batch batch = batchManager.getActiveBatch();
            while (line != null) {
                batch.add(ctx.toBatchedCommand(line));
                line = reader.readLine();
            }
        } catch (IOException e) {
            batchManager.discardActiveBatch();
            throw new RuntimeException("Failed to read file "
                    + file.getAbsolutePath(), e);
        } catch (CommandFormatException e) {
            batchManager.discardActiveBatch();
            throw new RuntimeException("Failed to create batch from "
                    + file.getAbsolutePath(), e);
        } finally {
            if (baseDir != null) {
                ctx.setCurrentDir(currentDir);
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
