/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.console;

import java.util.List;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.console.CliSpecialCommand.CliSpecialExecutor;

/**
 * A Class to bridge legacy CommandHandler with Aesh Command.
 *
 * @author jdenise@redhat.com
 */
class CliLegacyCommandBridge implements CliSpecialExecutor {

    @Override
    public CommandContainerResult execute(CommandContext commandContext, String originalInput) throws CommandLineException {
        try {
            ctx.handle(originalInput);
            console.setPrompt(ctx.getPrompt());
        } catch (CommandLineException ex) {
            throw new RuntimeException(ex);
        }

        return new CommandContainerResult(new NullResultHandler(),
                CommandResult.SUCCESS);
    }

    private final CommandContext ctx;
    private final String name;
    private final Console console;

    CliLegacyCommandBridge(Console console, String name, CommandContext ctx)
            throws CommandLineParserException {
        this.ctx = ctx;
        this.name = name;
        this.console = console;
    }

    @Override
    public int complete(CommandContext commandContext, String buffer, int i, List<String> candidates) {
        return ctx.getDefaultCommandCompleter().complete(ctx,
                buffer, 0, candidates);
    }

    @Override
    public boolean accept(String line) {
        return line.startsWith(name);
    }

}
