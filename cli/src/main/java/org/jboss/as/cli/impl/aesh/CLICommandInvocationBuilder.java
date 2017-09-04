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
package org.jboss.as.cli.impl.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandRuntime;
import org.aesh.command.shell.Shell;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.impl.ReadlineConsole;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
public class CLICommandInvocationBuilder implements
        CommandInvocationBuilder<Command<CLICommandInvocation>, CLICommandInvocation> {

    private final ReadlineConsole console;
    private final CommandContextImpl ctx;
    private final CLICommandRegistry registry;
    private final Shell shell;

    CLICommandInvocationBuilder(CommandContextImpl ctx,
            CLICommandRegistry registry, ReadlineConsole console, Shell shell) {
        this.ctx = ctx;
        this.registry = registry;
        this.console = console;
        this.shell = shell;
    }

    @Override
    public CLICommandInvocation build(CommandRuntime<Command<CLICommandInvocation>, CLICommandInvocation> runtime,
            CommandInvocationConfiguration configuration) {
        return new CLICommandInvocationImpl(ctx.getContextualCommandContext(),
                registry, console, shell, runtime, configuration);
    }

}
