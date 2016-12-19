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
package org.jboss.as.cli.impl.aesh.commands;

import java.io.File;
import org.aesh.cl.CommandDefinition;
import org.aesh.cl.Option;
import org.aesh.console.command.CommandException;
import org.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.wildfly.core.cli.command.aesh.activator.HiddenActivator;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.FileCompleter;
import org.wildfly.core.cli.command.aesh.activator.DefaultExpectedOptionsActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "save", description = "")
public class AttachmentSaveCommand extends AttachmentDisplayCommand {

    public static final class FileActivator extends DefaultExpectedOptionsActivator {

        public FileActivator() {
            super("operation");
        }
    }

    public static final class OverwriteActivator extends DefaultExpectedOptionsActivator {

        public OverwriteActivator() {
            super("file");
        }
    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(hasValue = true, completer = FileCompleter.class, activator = FileActivator.class)
    private String file;

    @Option(hasValue = false, activator = OverwriteActivator.class)
    private boolean overwrite;

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("attachment save"));
            return CommandResult.SUCCESS;
        }
        return super.execute(commandInvocation);
    }

    @Override
    AttachmentResponseHandler buildHandler(CommandContext commandContext) {
        return new AttachmentResponseHandler((String t) -> {
            commandContext.printLine(t);
        }, new File(file), true, overwrite);
    }
}
