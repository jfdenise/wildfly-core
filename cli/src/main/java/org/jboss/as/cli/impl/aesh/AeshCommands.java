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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceLoader;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.Executor;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.command.Shell;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandRuntime;
import org.aesh.command.Execution;
import org.aesh.command.operator.OperatorType;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.io.FileResource;
import org.aesh.io.Resource;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandExecutor.ExecutableBuilder;
import org.jboss.as.cli.impl.OperationCommandContainer;
import org.jboss.as.cli.impl.ReadlineConsole;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 * Aesh Commands.
 *
 * @author jdenise@redhat.com
 */
public class AeshCommands {

    private class BridgedContext implements AeshContext {

        private final CommandContext ctx;

        private BridgedContext(CommandContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Resource getCurrentWorkingDirectory() {
            return new FileResource(ctx.getCurrentDir());
        }

        @Override
        public void setCurrentWorkingDirectory(Resource cwd) {
            ctx.setCurrentDir(new File(cwd.getAbsolutePath()));
        }
    }

    public class CLIExecutor {

        private final List<CLIExecution> executions;

        CLIExecutor(Executor<CLICommandInvocation> executor) {
            List<CLIExecution> lst = new ArrayList<>();
            for (Execution<CLICommandInvocation> ex : executor.getExecutions()) {
                lst.add(new CLIExecution(ex));
            }
            executions = Collections.unmodifiableList(lst);
        }

        public List<CLIExecution> getExecutions() {
            return executions;
        }
    }

    public class CLIExecution {

        private final Execution<CLICommandInvocation> execution;

        CLIExecution(Execution<CLICommandInvocation> execution) {
            this.execution = execution;
        }

        public CLICommandInvocation getInvocation() {
            return execution.getCommandInvocation();
        }

        public BatchCompliantCommand getBatchCompliant() {
            if (execution.getCommand() instanceof BatchCompliantCommand) {
                return (BatchCompliantCommand) execution.getCommand();
            }
            return null;
        }

        public DMRCommand getDMRCompliant() {
            if (execution.getCommand() instanceof DMRCommand) {
                return (DMRCommand) execution.getCommand();
            }
            return null;
        }
    }

    private final CLICommandInvocationBuilder invocationBuilder;
    private final CommandRuntime<CLICommandInvocation> processor;
    private final CLICommandRegistry registry;
    private final CLICompletionHandler completionHandler;

    public AeshCommands(CommandContext ctx, OperationCommandContainer op) throws CliInitializationException {
        this(ctx, null, null, op);
    }

    public AeshCommands(CommandContext ctx, Completion<CompleteOperation> delegate,
            ReadlineConsole console, OperationCommandContainer op) throws CliInitializationException {
        registry = new CLICommandRegistry(ctx, op);
        Shell shell = null;
        if (console != null) {
            shell = new ReadlineShell(console);
        }
        invocationBuilder = new CLICommandInvocationBuilder(ctx, registry, console, shell);
        AeshCommandRuntimeBuilder builder = AeshCommandRuntimeBuilder.builder();
        processor = builder.
                commandRegistry(registry).
                operators(EnumSet.of(OperatorType.REDIRECT_OUT, OperatorType.END)).
                parseBrackets(true).
                aeshContext(new BridgedContext(ctx)).
                commandActivatorProvider(new CLICommandActivatorProvider(ctx)).
                commandInvocationBuilder(invocationBuilder).
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

    private void buildOperationBridge() {

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

    public CLIExecutor newExecutor(String line) throws CommandLineException, IOException {
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

    public ExecutableBuilder newExecutableBuilder(CLIExecution exe) {
        return (CommandContext ctx) -> {
            return () -> {
                try {
                    exe.execution.execute();
                } catch (CommandException | CommandValidatorException ex) {
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