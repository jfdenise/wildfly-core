/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command;

import java.io.IOException;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jfdenise
 */
@CommandDefinition(name = "clear", description = "", aliases = {"cls"})
public class ClearCommand implements Command<CliCommandInvocation> {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("clear"));
            return CommandResult.SUCCESS;
        }
        try {
            commandInvocation.getShell().clear();
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
        return CommandResult.SUCCESS;
    }
}
