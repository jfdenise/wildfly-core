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
package org.jboss.as.cli.console;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.as.cli.aesh.provider.CliCommandInvocationProvider;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocationProvider;
import org.jboss.as.cli.aesh.provider.CliConverterInvocationProvider;
import org.jboss.as.cli.aesh.provider.CliManProvider;
import org.jboss.as.cli.aesh.provider.CliOptionActivatorProvider;
import org.jboss.as.cli.aesh.provider.CliValidatorInvocationProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.settings.FileAccessPermission;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.parser.Parser;
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.completer.RolloutPlanCompleter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.aesh.provider.CliCommandActivatorProvider;
import org.jboss.as.cli.command.CdCommand;
import org.jboss.as.cli.command.ClearCommand;
import org.jboss.as.cli.command.CommandCommand;
import org.jboss.as.cli.command.Connect;
import org.jboss.as.cli.command.EchoCommand;
import org.jboss.as.cli.command.EchoDMRCommand;
import org.jboss.as.cli.command.HelpCommand;
import org.jboss.as.cli.command.history.HistoryCommand;
import org.jboss.as.cli.command.LsMapCommand;
import org.jboss.as.cli.command.PwdCommand;
import org.jboss.as.cli.command.Quit;
import org.jboss.as.cli.command.ReadCommand;
import org.jboss.as.cli.command.SetVariableCommand;
import org.jboss.as.cli.command.ShutdownCommand;
import org.jboss.as.cli.command.UnsetVariableCommand;
import org.jboss.as.cli.command.VersionCommand;
import org.jboss.as.cli.command.batch.BatchCommand;
import org.jboss.as.cli.command.compat.ClearBatch;
import org.jboss.as.cli.command.compat.DiscardBatch;
import org.jboss.as.cli.command.compat.EditLineBatch;
import org.jboss.as.cli.command.compat.HoldBackBatch;
import org.jboss.as.cli.command.compat.MoveLineBatch;
import org.jboss.as.cli.command.compat.ReadAttribute;
import org.jboss.as.cli.command.compat.ReadOperation;
import org.jboss.as.cli.command.compat.RemoveLineBatch;
import org.jboss.as.cli.command.compat.RunBatch;
import org.jboss.as.cli.command.generic.MainCommandParser;
import org.jboss.as.cli.command.generic.NodeType;
import org.jboss.as.cli.command.ifelse.ElseCommand;
import org.jboss.as.cli.command.ifelse.EndIfCommand;
import org.jboss.as.cli.command.ifelse.IfCommand;
import org.jboss.as.cli.command.operation.OperationSpecialCommand;
import org.jboss.as.cli.command.trycatch.CatchCommand;
import org.jboss.as.cli.command.trycatch.EndTryCommand;
import org.jboss.as.cli.command.trycatch.FinallyCommand;
import org.jboss.as.cli.command.trycatch.TryCommand;
import org.jboss.as.cli.embedded.EmbeddedControllerHandlerRegistrar;
import org.jboss.as.cli.handlers.jca.JDBCDriverNameProvider;
import org.jboss.as.cli.impl.CLIPrintStream;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.impl.Console;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.protocol.StreamUtils;

/**
 * @author jdenise@redhat.com
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class AeshCliConsole implements Console {

    private static class HistoryImpl implements CommandHistory {

        private final AeshConsole console;
        private final Settings settings;

        HistoryImpl(AeshConsole console, Settings settings) {
            this.console = console;
            this.settings = settings;
        }

        @Override
        public List<String> asList() {
            return console.getHistory().getAll();
        }

        @Override
        public boolean isUseHistory() {
            return console.getHistory().isEnabled();
        }

        @Override
        public void setUseHistory(boolean useHistory) {
            if (useHistory) {
                console.getHistory().enable();
            } else {
                console.getHistory().disable();
            }
        }

        @Override
        public void clear() {
            console.getHistory().clear();
        }

        @Override
        public int getMaxSize() {
            return settings.getHistorySize();
        }
    }

    class CliResultHandler implements ResultHandler {

        ResultHandler original;

        void setResultHandler(ResultHandler original) {
            this.original = original;
        }

        boolean isInteractive() {
            return interactive_execution;
        }

        private void wakeup() {
            synchronized (AeshCliConsole.this) {
                AeshCliConsole.this.notifyAll();
            }
        }

        @Override
        public void onSuccess() {
            if (original != null) {
                original.onSuccess();
            }
            if (need_resync) {
                wakeup();
            }
        }

        @Override
        public void onFailure(CommandResult result) {
            if (original != null) {
                original.onFailure(result);
            }
            if (need_resync) {
                wakeup();
            }
            commandFailure = true;
        }

        @Override
        public void onValidationFailure(CommandResult result, Exception exception) {
            if (original != null) {
                original.onValidationFailure(result, exception);
            }
            if (need_resync) {
                wakeup();
            }
            commandFailure = true;
            commandException = exception;
        }

        @Override
        public void onExecutionFailure(CommandResult result, CommandException exception) {
            if (original != null) {
                original.onExecutionFailure(result, exception);
            }
            if (need_resync) {
                wakeup();
            }
            commandFailure = true;
            commandException = exception;
        }

    }

    private interface CommandProcessor {

        void process() throws CommandException;
    }

    private static class SilentPrintStream extends PrintStream {

        public SilentPrintStream() {
            super(new SilentByteArrayOutputStream());
        }

        private static class SilentByteArrayOutputStream extends ByteArrayOutputStream {

            @Override
            public void write(int b) {
                // do nothing
            }

            @Override
            public void write(byte[] b, int off, int len) {
                // do nothing
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                // do nothing
            }

        }

    }

    private class LegacyCommandRegistry extends CommandRegistry {

        @Override
        public void registerHandler(CommandHandler handler,
                boolean tabComplete, String... names) throws RegisterHandlerException {
            super.registerHandler(handler, tabComplete, names);
            try {
                commandRegistry.registerLegacyHandler(handler, names);
            } catch (CommandLineException | CommandLineParserException ex) {
                throw new RegisterHandlerException(ex.getMessage());
            }
        }

        @Override
        public CommandHandler remove(String cmdName) {
            CommandHandler ch = super.remove(cmdName);
            commandRegistry.removeLegacyHandler(cmdName);
            return ch;
        }
    }

    private boolean silent;
    // Workaround for connect that must be done in Aesh console Thread
    private boolean need_resync;

    // When a command is executed without user input, the command is not interactive
    // This is required for ResultHandler.
    private boolean interactive_execution;
    private final Boolean errorOnInteract;

    protected AeshConsole console;
    private final CommandContextImpl ctx;
    private final CliCommandContextImpl commandContext;
    private CliCommandRegistry commandRegistry;
    private final CommandRegistry legacyRegistry = new LegacyCommandRegistry();
    private final CLIPrintStream printStream;
    private static final String PROVIDER = "JBOSS_CLI";

    private boolean commandFailure;
    private Exception commandException;
    private boolean interactive_connect;
    private final boolean echoCommand;
    private final Settings settings;
    AeshCliConsole(CommandContextImpl commandContext, boolean silent,
            Boolean errorOnInteract, Settings aeshSettings,
            InputStream consoleInput, OutputStream consoleOutput, boolean echoCommand)
            throws CommandLineParserException, CommandLineException {
        this.ctx = commandContext;
        this.commandContext = new CliCommandContextImpl(ctx);
        this.printStream = consoleOutput == null ? new CLIPrintStream()
                : new CLIPrintStream(consoleOutput);
        this.silent = silent;
        this.errorOnInteract = errorOnInteract == null ? false : errorOnInteract;
        settings = aeshSettings == null
                ? createSettings(commandContext.getConfig(),
                        consoleInput,
                        printStream) : aeshSettings;
        setupConsole(settings);
        this.echoCommand = echoCommand;
    }

    @Override
    public void start() {
        console.start();
    }

    @Override
    public void stop() {
        console.stop();
    }

    @Override
    public void interrupt() {
        stop();
    }

    CliResultHandler newResultHandler() {
        return new CliResultHandler();
    }

    private void setupConsole(Settings settings) throws CommandLineParserException,
            CommandLineException {

        CommandInvocationServices services = new CommandInvocationServices();
        CliCommandInvocationProvider prov = new CliCommandInvocationProvider(commandContext, this);
        services.registerProvider(PROVIDER, prov);

        commandRegistry = createCommandRegistry();

        commandRegistry.addSpecialCommand(new CliSpecialCommandBuilder().name(":").
                context(ctx).
                registry(commandRegistry).
                resultHandler(newResultHandler()).
                activator((c) -> ctx.getModelControllerClient() != null).
                executor(new OperationSpecialCommand(ctx, commandContext, this)).create());

        registerExtraCommands();

        CliOptionActivatorProvider activatorProvider = new CliOptionActivatorProvider(commandContext);

        CliCommandActivatorProvider cmdActivatorProvider = new CliCommandActivatorProvider(commandContext);

        console = new AeshConsoleBuilder()
                .commandRegistry(commandRegistry)
                .settings(settings)
                .commandInvocationProvider(services)
                .completerInvocationProvider(new CliCompleterInvocationProvider(commandContext, commandRegistry))
                .commandNotFoundHandler(new CliCommandNotFound(newResultHandler()))
                .converterInvocationProvider(new CliConverterInvocationProvider(commandContext))
                .optionActivatorProvider(activatorProvider)
                .commandActivatorProvider(cmdActivatorProvider)
                .validatorInvocationProvider(new CliValidatorInvocationProvider(commandContext))
                .manProvider(new CliManProvider())
                .create();
        console.setCurrentCommandInvocationProvider(PROVIDER);
    }

    private void registerExtraCommands() throws CommandLineException, CommandLineParserException {
        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);
        for (Command command : loader) {
            commandRegistry.addCommand(command);
        }
    }

    private Settings createSettings(CliConfig config, InputStream consoleInput,
            PrintStream consoleOutput) {
        SettingsBuilder settings = new SettingsBuilder();
        if (consoleInput != null) {
            settings.inputStream(consoleInput);
        }

        settings.outputStream(consoleOutput);

        settings.enableExport(false);

        settings.disableHistory(!config.isHistoryEnabled());
        settings.historyFile(new File(config.getHistoryFileDir(), config.getHistoryFileName()));
        settings.historySize(config.getHistoryMaxSize());

        // Modify Default History File Permissions
        FileAccessPermission permissions = new FileAccessPermission();
        permissions.setReadableOwnerOnly(true);
        permissions.setWritableOwnerOnly(true);
        settings.historyFilePermission(permissions);

        settings.parseOperators(false);

        settings.interruptHook((org.jboss.aesh.console.Console c, Action action) -> {
            try {
                ctx.terminateSession();
            } finally {
                stop();
            }
        });

        return settings.create();
    }

    private CliCommandRegistry createCommandRegistry()
            throws CommandLineException, CommandLineParserException {
        CliCommandRegistry clireg = new CliCommandRegistry(this,
                ctx, commandContext);
        clireg.addCommand(new BatchCommand());
        clireg.addCommand(new CdCommand());
        clireg.addCommand(new ClearCommand());
        clireg.addCommand(new Connect());
        clireg.addCommand(new CommandCommand());
        clireg.addCommand(new EchoCommand());
        clireg.addCommand(new EchoDMRCommand());
        clireg.addCommand(new HelpCommand(clireg));
        clireg.addCommand(new HistoryCommand());
        // ls is a dynamic command
        clireg.addCommand(new AeshCommandContainer(
                new AeshCommandLineParser<>(
                        new LsMapCommand().getProcessedCommand(ctx))));
        clireg.addCommand(new PwdCommand());
        clireg.addCommand(new Quit());
        clireg.addCommand(new ReadCommand());
        clireg.addCommand(new SetVariableCommand());
        clireg.addCommand(new ShutdownCommand(ctx, ctx.getEmbeddedServerReference()));
        clireg.addCommand(new UnsetVariableCommand());
        clireg.addCommand(new VersionCommand());

        //embedded
        EmbeddedControllerHandlerRegistrar.registerEmbeddedCommands(clireg,
                ctx.getEmbeddedServerReference());

        // try catch
        clireg.addCommand(new TryCommand());
        clireg.addCommand(new CatchCommand());
        clireg.addCommand(new FinallyCommand());
        clireg.addCommand(new EndTryCommand());

        // if
        clireg.addCommand(new IfCommand());
        clireg.addCommand(new ElseCommand());
        clireg.addCommand(new EndIfCommand());

        // Add deprecated, for BWCompat only
        clireg.addCommand(new ClearBatch());
        clireg.addCommand(new DiscardBatch());
        clireg.addCommand(new EditLineBatch());
        clireg.addCommand(new HoldBackBatch());
        clireg.addCommand(new MoveLineBatch());
        clireg.addCommand(new ReadAttribute());
        clireg.addCommand(new ReadOperation());
        clireg.addCommand(new RemoveLineBatch());
        clireg.addCommand(new RunBatch());

        try {
            // Add some Generic commands
            MainCommandParser dataSourceParser = new MainCommandParser("data-source",
                    new NodeType("/subsystem=datasources/data-source"),
                    null,
                    ctx,
                    false);
            final DefaultCompleter driverNameCompleter = new DefaultCompleter(JDBCDriverNameProvider.INSTANCE);
            dataSourceParser.addCustomCompleter(Util.DRIVER_NAME,
                    new org.jboss.as.cli.aesh.completer.DefaultCompleter(driverNameCompleter));
            // XXX JFDENISE TODO
            //dataSourceParser.addCustomSubCommand(new DataSourceAddCompositeSubCommand(Util.ADD,
            //        new NodeType("/subsystem=datasources/data-source"), null));
            clireg.addCommand(new AeshCommandContainer(dataSourceParser));

            MainCommandParser xdataSourceParser = new MainCommandParser("xa-data-source",
                    new NodeType("/subsystem=datasources/xa-data-source"),
                    null,
                    ctx,
                    false);
            xdataSourceParser.addCustomCompleter(Util.DRIVER_NAME,
                    new org.jboss.as.cli.aesh.completer.DefaultCompleter(driverNameCompleter));
            // XXX JFDENISE TODO
            //dataSourceParser.addCustomSubCommand(new XADataSourceAddCompositeSubCommand(Util.ADD,
            //        new NodeType("/subsystem=datasources/data-source"), null));
            clireg.addCommand(new AeshCommandContainer(xdataSourceParser));

            MainCommandParser rolloutParser = new MainCommandParser("rollout-plan",
                    new NodeType("/management-client-content=rollout-plans/rollout-plan"),
                    null,
                    ctx,
                    false);
            rolloutParser.addCustomConverter("content", HeadersConverter.INSTANCE);
            rolloutParser.addCustomCompleter("content", RolloutPlanCompleter.INSTANCE);
            clireg.addCommand(new AeshCommandContainer(rolloutParser));

        } catch (CommandLineParserException ex) {
            throw new RuntimeException(ex);
        }

        return clireg;
    }

    @Override
    public void addCompleter(CommandLineCompleter completer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isUseHistory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CommandHistory getHistory() {
        return new HistoryImpl(console, settings);
    }

    @Override
    public void setCompletion(boolean complete) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearScreen() {
        console.clear();
    }

    @Override
    public void printColumns(Collection<String> list) {
        if (!silent) {
            String[] newList = new String[list.size()];
            list.toArray(newList);
            if (interactive_execution) {
                console.getShell().out().println(
                        Parser.formatDisplayList(newList,
                                console.getShell().getSize().getHeight(),
                                console.getShell().getSize().getWidth()));
            } else {
                for (String item : list) {
                    println(item);
                }
            }
        }
    }

    @Override
    public void print(String line) {
        if (!silent) {
            console.getShell().out().print(line);
        }
    }

    @Override
    public void printNewLine() {
        if (!silent) {
            console.getShell().out().println();
        }
    }

    @Override
    public void println(String msg) {
        if (!silent) {
            console.getShell().out().println(msg);
        }
    }

    private static void interactionDisabled() throws CommandLineException {
        throw new CommandLineException("Invalid Usage. Prompt attempted in non-interactive mode. Please check commands or change CLI mode.");
    }

    @Override
    public int getTerminalWidth() {
        return console.getShell().getSize().getWidth();
    }

    @Override
    public int getTerminalHeight() {
        return console.getShell().getSize().getHeight();
    }

    @Override
    public boolean isCompletionEnabled() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void controlled() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isControlled() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void continuous() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setCallback(ConsoleCallback consoleCallback) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean running() {
        return console.isRunning();
    }

    @Override
    public void setPrompt(String prompt) {
        setPrompt(prompt, null);
    }

    @Override
    public void setPrompt(String prompt, Character mask) {
        if (!prompt.equals(console.getPrompt().getPromptAsString())
                && isInteractive()) {
            // Setting the prompt has only an effect when done from within
            // the Aesh Command thread. This means from a Command.execute method
            console.setPrompt(new Prompt(prompt, mask));
        }
    }

    private boolean isInteractive() {
        return interactive_connect || interactive_execution;
    }

    @Override
    public void redrawPrompt() {
        setPrompt(console.getPrompt().getPromptAsString());
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public void captureOutput(PrintStream captor) {
        printStream.captureOutput(captor);
    }

    @Override
    public void releaseOutput() {
        printStream.releaseOutput();
    }

    @Override
    public void interact(boolean connect) throws CliInitializationException {
        if (connect) {
            try {
                // Synchronously connect to the server
                // doing so we will be able to change the console to interactive
                // when connection has been established.
                interactive_connect = true;
                try {
                    connect();
                } finally {
                    interactive_connect = false;
                }
                startInteractiveConsole();
            } catch (CommandException ex) {
                throw new CliInitializationException("Failed to connect to the controller", ex);
            }
        } else if (ctx.getConnectionInfo() == null) {
            print("You are disconnected at the moment. Type 'connect' to connect to the server or"
                    + " 'help' for the list of supported commands.");
            printNewLine();
            startInteractiveConsole();
        } else {
            // Warning, jboss.cli.rc can already have established the connection.
            // In this case, the prompt will be displayed when the user press return.
            // This is a corner case of having the connect of an interactive session
            // being doone in non interactive mode (from the cli.rc file).
            startInteractiveConsole();
        }

    }

    @Override
    public String promptForInput(String prompt)
            throws IOException, InterruptedException, CommandLineException {
        return promptForInput(prompt, null);
    }

    @Override
    public String promptForInput(String prompt, Character mask)
            throws IOException, InterruptedException, CommandLineException {
        if (errorOnInteract) {
            interactionDisabled();
        }

        ConsoleBuffer consoleBuffer = new AeshConsoleBufferBuilder()
                .shell(console.getShell())
                .prompt(new Prompt(prompt, mask))
                .create();
        InputProcessor inputProcessor = new AeshInputProcessorBuilder()
                .consoleBuffer(consoleBuffer)
                .create();

        consoleBuffer.displayPrompt();
        String result;
        do {
            result = inputProcessor.parseOperation(console.
                    getConsoleCallback().getInput());
        } while (result == null);
        return result;
    }

    @Override
    public void error(String msg) {
        if (!silent) {
            console.getShell().err().println(msg);
        }
    }

    @Override
    public CommandRegistry getLegacyCommandRegistry() {
        return legacyRegistry;
    }

    @Override
    public CliCommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    private StringBuilder lineBuffer;
    private StringBuilder origLineBuffer;
    private boolean connect;

    @Override
    public void executeCommand(String line) throws CommandException {
        interactive_execution = false;
        commandException = null;
        commandFailure = false;
        try {
            if (line.isEmpty() || line.charAt(0) == '#') {
                return; // ignore comments
            }

            int i = line.length() - 1;
            while (i > 0 && line.charAt(i) <= ' ') {
                if (line.charAt(--i) == '\\') {
                    break;
                }
            }
            String echoLine = line;
            if (line.charAt(i) == '\\') {
                if (lineBuffer == null) {
                    lineBuffer = new StringBuilder();
                    origLineBuffer = new StringBuilder();
                }
                lineBuffer.append(line, 0, i);
                lineBuffer.append(' ');
                origLineBuffer.append(line, 0, i);
                origLineBuffer.append('\n');
                return;
            } else if (lineBuffer != null) {
                lineBuffer.append(line);
                origLineBuffer.append(line);
                echoLine = origLineBuffer.toString();
                line = lineBuffer.toString();
                lineBuffer = null;
            }

            // XXX Bridged commands have no output...
            if (echoCommand && !connect
                    && !commandContext.getLegacyCommandContext().isWorkflowMode()) {
                println(commandContext.getLegacyCommandContext().getPrompt() + echoLine);
            }

            // Needed in case we have SSL/AUTH callback or any other user interaction
            if (commandRegistry.isInteractive(line)) {
                executeResync(line);
            } else {
                execute(line);
            }
        } finally {
            interactive_execution = true;
            commandException = null;
            commandFailure = false;
        }
    }

    // Execute synchronous operation.
    private void execute(String command) throws CommandException {
        if (!command.isEmpty() && command.charAt(0) != '#') {
            try {
                console.getConsoleCallback().execute(new ConsoleOperation(ControlOperator.APPEND_OUT, command));
                if (commandFailure) {
                    CommandException ex;
                    if (commandException != null) {
                        if (commandException instanceof CommandException) {
                            ex = (CommandException) commandException;
                        } else {
                            ex = new CommandException(commandException.getMessage(),
                                    commandException);
                        }
                    } else {
                        ex = new CommandException("Failure executing " + command);
                    }
                    throw ex;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }
    }

    // Command that need to be executed in the AeshConsole thread.
    // This is a workaround for NPE if console started in same thread as prompting
    // (e.g:SSL, AUTH callbacks)
    private void executeResync(String command) throws CommandException {
        need_resync = true;
        try {
            if (!console.isRunning()) {
                startNonInteractiveConsole();
            }
            console.execute(command);
            synchronized (this) {
                try {
                    wait();
                    if (commandFailure) {
                        CommandException ex;
                        if (commandException != null) {
                            if (commandException instanceof CommandException) {
                                ex = (CommandException) commandException;
                            } else {
                                ex = new CommandException(commandException.getMessage(),
                                        commandException);
                            }
                        } else {
                            ex = new CommandException("Failure executing " + command);
                        }
                        throw ex;
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        } finally {
            need_resync = false;
        }
    }

    private void connect() throws CommandException {
        try {
            connect = true;
            executeCommand("connect");
        } finally {
            connect = false;
        }
    }

    @Override
    public void process(List<String> commands, boolean connect) throws CommandException {
        processNonInteractive(() -> {
            for (String command : commands) {
                executeCommand(command);
            }
        }, connect);
    }

    private void startNonInteractiveConsole() {
        startConsole(false, "");
    }

    private void startInteractiveConsole() {
        startConsole(true, ctx.getPrompt());
    }

    private void startConsole(boolean interactive, String prompt) {
        console.setEcho(interactive);
        console.setPrompt(new Prompt(prompt));
        if (!console.isRunning()) {
            console.start();
        }
    }

    private void processNonInteractive(CommandProcessor processor, boolean connect) throws CommandException {
        if (connect) {
            connect();
        }

        try {
            processor.process();
        } finally {
            ctx.terminateSession();
        }
    }

    @Override
    public void processFile(File file, boolean connect) throws CommandException {
        processNonInteractive(() -> {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                while (line != null) {
                    executeCommand(line.trim());
                    line = reader.readLine();
                }
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to process file '" + file.getAbsolutePath() + "'", e);
            } finally {
                StreamUtils.safeClose(reader);
            }
        }, connect);

    }

    @Override
    public CliCommandContext getCliCommandContext() {
        return commandContext;
    }

}
