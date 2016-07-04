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
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jfdenise
 */
@CommandDefinition(name = "clear", description = "", aliases = {"cls"})
public class ClearCommand implements Command<CliCommandInvocation> {

    @Option(hasValue = false)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        try {
            commandInvocation.getShell().clear();
        } catch (IOException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
        return null;
    }
}
