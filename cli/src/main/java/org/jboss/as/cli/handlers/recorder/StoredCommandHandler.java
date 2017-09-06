/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.handlers.recorder;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.impl.StoredCommands.StoredCommand;

/**
 *
 * @author jdenise@redhat.com
 */
public class StoredCommandHandler extends CommandHandlerWithHelp {

    private final StoredCommand command;

    public StoredCommandHandler(StoredCommand command) {
        super(command.getName(), false);
        this.command = command;
    }

    @Override
    protected void printHelp(CommandContext ctx) throws CommandLineException {
        ctx.printLine("stored command " + command.getName() + " commands:");
        for(String cmd : command.getCommands()) {
            ctx.printLine(cmd);
        }
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return true;
    }

    @Override
    public boolean isBatchMode(CommandContext ctx) {
        return false;
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        for (String cmd : command.getCommands()) {
            ctx.handle(cmd);
        }
    }

}
