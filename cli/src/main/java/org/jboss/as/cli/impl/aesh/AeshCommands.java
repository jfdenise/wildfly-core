/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.ServiceLoader;
import org.aesh.command.impl.parser.CommandLineParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.AeshCommandRuntime;
import org.aesh.command.impl.AeshCommandRuntimeBuilder;
import org.aesh.command.Executor;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.command.Shell;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandExecutor.ExecutableBuilder;
import org.jboss.as.cli.impl.ReadlineConsole;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 * Aesh Commands.
 *
 * @author jdenise@redhat.com
 */
public class AeshCommands {

    public class CLIExecutor {

        private final Executor executor;

        CLIExecutor(Executor executor) {
            this.executor = executor;
        }

        public BatchCompliantCommand getBatchCompliant() {
            if (executor.getExecutable() instanceof BatchCompliantCommand) {
                return (BatchCompliantCommand) executor.getExecutable();
            }
            return null;
        }
    }

    private final CLICommandInvocationProvider invocationProvider;
    private final AeshCommandRuntime<CLICommandInvocation, AeshCompleteOperation> processor;
    private final CLICommandRegistry registry = new CLICommandRegistry();
    private final CLICompletionHandler completionHandler;

    public AeshCommands(CommandContext ctx) throws CliInitializationException {
        this(ctx, null, null);
    }

    public AeshCommands(CommandContext ctx, Completion<CompleteOperation> delegate, ReadlineConsole console) throws CliInitializationException {
        Shell shell = null;
        if (console != null) {
            shell = new ReadlineShell(console);
        }
        invocationProvider = new CLICommandInvocationProvider(ctx, registry, console, shell);
        AeshCommandRuntimeBuilder builder = AeshCommandRuntimeBuilder.builder();
        processor = builder.
                commandRegistry(registry).
                commandActivatorProvider(new CLICommandActivatorProvider(ctx)).
                commandInvocationProvider(invocationProvider).
                completerInvocationProvider(new CLICompleterInvocationProvider(ctx, registry)).
                converterInvocationProvider(new CLIConverterInvocationProvider(ctx)).
                optionActivatorProvider(new CLIOptionActivatorProvider(ctx)).
                validatorInvocationProvider(new CLIValidatorInvocationProvider(ctx)).
                build();
        completionHandler = new CLICompletionHandler(this, delegate);
        if (console != null) {
            console.setCompletionHandler(completionHandler);
            console.addCompleter(completionHandler);
        }
    }

    public CommandLineCompleter getCommandCompleter() {
        return completionHandler;
    }

    public CLICommandRegistry getRegistry() {
        return registry;
    }

    AeshContext getAeshContext() {
        return processor.getAeshContext();
    }

    public void complete(AeshCompleteOperation co) {
        processor.complete(co);
    }

    public CLIExecutor newExecutor(String line) throws CommandLineException {
        CLIExecutor exe;
        try {
            exe = new CLIExecutor(processor.buildExecutor(line));
        } catch (CommandNotFoundException ex) {
            return null;
        } catch (CommandLineParserException | OptionValidatorException | CommandValidatorException ex) {
            throw new CommandLineException(ex.getLocalizedMessage());
        }
        return exe;
    }

    public ExecutableBuilder newExecutableBuilder(CLIExecutor exe) {
        Executor executor = exe.executor;
        return (CommandContext ctx) -> {
            CLICommandInvocation ci = invocationProvider.
                    newCommandInvocation(executor.getCommandInvocation(), ctx);
            return () -> {
                try {
                    executor.getExecutable().execute(ci);
                } catch (CommandException ex) {
                    throw new CommandLineException(ex.getLocalizedMessage());
                } catch (InterruptedException ex) {
                    Thread.interrupted();
                    throw new CommandLineException(ex);
                }
            };
        };
    }

    public void registerExtraCommands() throws CommandLineException {
        ServiceLoader<Command> loader2 = ServiceLoader.load(Command.class);
        for (Command command : loader2) {
            getRegistry().addCommand(command);
        }
    }
}
