/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.patching.cli;

import java.io.File;
import java.util.List;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.option.Arguments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.patching.tool.PatchOperationBuilder;
import org.wildfly.core.cli.command.aesh.FileCompleter;
import org.wildfly.core.cli.command.aesh.FileConverter;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "apply", description = "")
public class PatchApply extends PatchOverrideCommand {

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',',
            completer = FileCompleter.class, converter = FileConverter.class)
    private List<File> path;

    public PatchApply(String action) {
        super("apply");
    }

    @Override
    protected PatchOperationBuilder createUnconfiguredOperationBuilder(CommandContext ctx) throws CommandException {
        if (path == null || path.isEmpty()) {
            throw new CommandException("A path is required.");
        }

        final File f = path.get(0);
        if (!f.exists()) {
            // i18n is never used for CLI exceptions
            throw new CommandException("Path " + f.getAbsolutePath() + " doesn't exist.");
        }
        if (f.isDirectory()) {
            throw new CommandException(f.getAbsolutePath() + " is a directory.");
        }
        return PatchOperationBuilder.Factory.patch(f);
    }

}
