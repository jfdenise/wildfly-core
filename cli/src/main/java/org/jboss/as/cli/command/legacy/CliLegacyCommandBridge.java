/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command.legacy;

import java.util.List;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.console.CliSpecialCommand.CliSpecialExecutor;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;

/**
 * A Class to bridge legacy CommandHandler with Aesh Command.
 *
 * @author jdenise@redhat.com
 */
public class CliLegacyCommandBridge implements CliSpecialExecutor {

    @Override
    public CommandContainerResult execute(CommandContext commandContext,
            String originalInput) throws CommandLineException {

        commandContext.handle(originalInput);

        return new CommandContainerResult(new NullResultHandler(),
                CommandResult.SUCCESS);
    }

    private final CliCommandContextImpl ctx;
    private final String name;
    private final DefaultCallbackHandler line = new DefaultCallbackHandler(false);

    public CliLegacyCommandBridge(String name, CliCommandContextImpl ctx)
            throws CommandLineParserException {
        this.ctx = ctx;
        this.name = name;
    }

    @Override
    public int complete(CommandContext commandContext, String buffer, int i, List<String> candidates) {
        line.reset();
        try {
            line.parse(ctx.getLegacyCommandContext().getCurrentNodePath(),
                    buffer, ctx.getLegacyCommandContext());
        } catch (CommandFormatException ex) {
            // XXX OK, no completion.
            return -1;
        }
        ctx.setParsedCommandLine(line);
        return ctx.getLegacyCommandContext().getDefaultCommandCompleter().
                complete(ctx.getLegacyCommandContext(),
                        buffer, buffer.length(), candidates);
    }

    @Override
    public boolean accept(String line) {
        return line.startsWith(name) && name.length() >= line.length();
    }

}
