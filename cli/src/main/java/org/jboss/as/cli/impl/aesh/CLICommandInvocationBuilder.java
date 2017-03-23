/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.impl.aesh;

import org.aesh.command.CommandRuntime;
import org.aesh.command.Shell;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationBuilder;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.impl.ReadlineConsole;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
public class CLICommandInvocationBuilder implements CommandInvocationBuilder<CLICommandInvocation> {

    private final ReadlineConsole console;
    private final CommandContext ctx;
    private final CLICommandRegistry registry;
    private final Shell shell;

    CLICommandInvocationBuilder(CommandContext ctx, CLICommandRegistry registry, ReadlineConsole console, Shell shell) {
        this.ctx = ctx;
        this.registry = registry;
        this.console = console;
        this.shell = shell;
    }

    @Override
    public CommandInvocation build(CommandRuntime<CLICommandInvocation> runtime,
            CommandInvocationConfiguration configuration) {
        return new CLICommandInvocationImpl(ctx, registry, console, shell, runtime, configuration);
    }

}
