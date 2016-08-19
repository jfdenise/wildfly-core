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
import org.jboss.as.cli.impl.Console;
import org.jboss.as.cli.impl.HelpSupport;
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
    private final Console console;

    public CliLegacyCommandBridge(String name, CliCommandContextImpl ctx, Console console)
            throws CommandLineParserException {
        this.ctx = ctx;
        this.name = name;
        this.console = console;
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
        // line ends with the separator.
        String[] split = line.trim().split(" ");
        String n = null;
        for (String s : split) {
            if (s.isEmpty()) {
                continue;
            } else {
                n = s;
                break;
            }
        }
        if (n == null) {
            return false;
        }
        return n.startsWith(name) && name.length() >= n.length();
    }

    @Override
    public String printHelp(String op) {
        return HelpSupport.printHelp(console, name);
    }

}
