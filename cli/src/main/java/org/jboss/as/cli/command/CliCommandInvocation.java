/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.command;

import java.util.Collection;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.terminal.Shell;
import org.jboss.as.cli.CliCommandContext;
import org.jboss.as.cli.impl.Console;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliCommandInvocation implements CommandInvocation {

    private final CommandInvocation commandInvocation;
    private final CliCommandContext ctx;
    private final Console console;
    public CliCommandInvocation(CliCommandContext ctx,
            CommandInvocation commandInvocation, Console console) {
        this.ctx = ctx;
        this.commandInvocation = commandInvocation;
        this.console = console;
    }

    public final CliCommandContext getCommandContext() {
        return ctx;
    }

    @Override
    public ControlOperator getControlOperator() {
        return commandInvocation.getControlOperator();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return commandInvocation.getCommandRegistry();
    }

    @Override
    public Shell getShell() {
        return commandInvocation.getShell();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        commandInvocation.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return commandInvocation.getPrompt();
    }

    @Override
    public String getHelpInfo(String s) {
        return commandInvocation.getHelpInfo(s);
    }

    @Override
    public void stop() {
        commandInvocation.stop();
    }

    @Override
    public AeshContext getAeshContext() {
        return commandInvocation.getAeshContext();
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        return commandInvocation.getInput();
    }

    @Override
    public int getPid() {
        return commandInvocation.getPid();
    }

    @Override
    public void putProcessInBackground() {
        commandInvocation.putProcessInBackground();
    }

    @Override
    public void putProcessInForeground() {
        commandInvocation.putProcessInForeground();
    }

    @Override
    public void executeCommand(String input) {
        commandInvocation.executeCommand(input);
    }

    @Override
    public String getInputLine() throws InterruptedException {
        return commandInvocation.getInputLine();
    }

    @Override
    public void print(String msg) {
        console.print(msg);
    }

    @Override
    public void println(String msg) {
        console.println(msg);
    }

    public void printColumns(Collection<String> col) {
        console.printColumns(col);
    }

    public void clearScreen() {
        console.clearScreen();
    }

    @Override
    public boolean isEchoing() {
        return commandInvocation.isEchoing();
    }

    @Override
    public void setEcho(boolean interactive) {
        commandInvocation.setEcho(interactive);
    }

}
