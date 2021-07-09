/*
Copyright 2021 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.cli.impl.aesh.cmd;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 * Aborts a command execution.
 *
 */
@CommandDefinition(name = "fail", description = "Aborts an execution. If the CLI executes a CLI script, the CLI process exists with an err code of 1.")
public class FailCommand implements Command<CLICommandInvocation> {

    @Argument(required = true)
    private String message;

    @Option(hasValue = false, name = "help")
    private boolean help;

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        CommandContext commandContext = commandInvocation.getCommandContext();

        if (help) {
            commandContext.printLine(commandInvocation.getHelpInfo("connect"));
            return CommandResult.SUCCESS;
        }
        throw new CommandException(message);
    }
}
