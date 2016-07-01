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
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.aesh.converter.FileConverter;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "run-file", description = "")
public class BatchRunFileCommand extends BatchRunCommand {

    @Option(name = "file", converter = FileConverter.class,
            completer = FileCompleter.class)
    private File file;

    @Override
    public ModelNode newRequest(CommandContext ctx) throws CommandLineException {
        final BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            throw new CommandLineException("Batch already active, can't start new batch");
        }
        if (!file.exists()) {
            throw new CommandLineException("File " + file.getAbsolutePath() + " does not exist.");
        }

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
            while (line != null) {
                ctx.handle(line);
                line = reader.readLine();
            }
            final ModelNode request = batchManager.getActiveBatch().toRequest();
            return request;
        } catch (IOException e) {
            throw new CommandLineException("Failed to read file "
                    + file.getAbsolutePath(), e);
        } catch (CommandLineException e) {
            throw new CommandLineException("Failed to create batch from "
                    + file.getAbsolutePath(), e);
        } finally {
            batchManager.discardActiveBatch();
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
    }

}
