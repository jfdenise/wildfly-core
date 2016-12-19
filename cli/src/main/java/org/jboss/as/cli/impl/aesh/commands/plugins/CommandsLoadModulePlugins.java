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
package org.jboss.as.cli.impl.aesh.commands.plugins;

import java.io.File;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.FileCompleter;
import org.wildfly.core.cli.command.aesh.FileConverter;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "load-module-plugins", description = "", activator = ModularActivator.class)
public class CommandsLoadModulePlugins implements Command<CLICommandInvocation> {

    @Option(name = "path", completer = FileCompleter.class, required = false, converter = FileConverter.class)
    private File path;

    @Option(name = "name", required = true)
    private String name;

    private final CommandContextImpl ctx;

    CommandsLoadModulePlugins(CommandContextImpl ctx) {
        this.ctx = ctx;
    }

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            ctx.loadPlugins(path, name);
        } catch (CommandLineException | ModuleLoadException ex) {
            throw new CommandException(ex);
        }
        return CommandResult.SUCCESS;
    }
}
