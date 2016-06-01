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
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.AeshConsoleBufferBuilder;
import org.jboss.aesh.console.AeshInputProcessorBuilder;
import org.jboss.aesh.console.ConsoleBuffer;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.aesh.console.InputProcessor;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.container.AeshCommandContainerBuilder;
import org.jboss.aesh.console.settings.FileAccessPermission;
import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.parser.Parser;
import org.jboss.as.cli.CliCommandContext;
import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.command.operation.OperationSpecialCommand;
import org.jboss.as.cli.impl.CLIPrintStream;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.impl.Console;
import org.jboss.as.protocol.StreamUtils;

/**
 * @author jdenise@redhat.com
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
class AeshCliConsole implements Console {

    private interface CommandProcessor {

        void process() throws CommandLineException;
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
        public void registerHandler(CommandHandler handler, boolean tabComplete, String... names) throws RegisterHandlerException {
            super.registerHandler(handler, tabComplete, names);
            try {
                commandRegistry.registerLegacyHandler(handler, names);
            } catch (CommandLineParserException ex) {
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
    private final boolean interactive;
    private final Boolean errorOnInteract;

    protected AeshConsole console;
    private final CommandContextImpl ctx;
    private final CliCommandContext commandContext;
    private CliCommandRegistry commandRegistry;
    private final CommandRegistry legacyRegistry = new LegacyCommandRegistry();
    private final CLIPrintStream printStream;
    private static final String PROVIDER = "JBOSS_CLI";
    private final AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();

    AeshCliConsole(CommandContextImpl commandContext, boolean silent, Boolean errorOnInteract, boolean interactive,
            Settings aeshSettings,
            InputStream consoleInput, OutputStream consoleOutput)
            throws CommandLineParserException, CommandLineException {
        this.ctx = commandContext;
        this.commandContext = new CliCommandContextImpl(ctx);
        this.printStream = consoleOutput == null ? new CLIPrintStream()
                : new CLIPrintStream(consoleOutput);
        this.interactive = interactive;
        this.silent = silent;
        this.errorOnInteract = errorOnInteract == null ? false : errorOnInteract;
        Settings settings = aeshSettings == null
                ? createSettings(commandContext.getConfig(),
                        consoleInput,
                        printStream) : aeshSettings;
        setupConsole(settings);
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

    }

    private void setupConsole(Settings settings) throws CommandLineParserException,
            CommandLineException {

        CommandInvocationServices services = new CommandInvocationServices();
        services.registerProvider(PROVIDER, new CliCommandInvocationProvider(commandContext));

        commandRegistry = createCommandRegistry();

        commandRegistry.addSpecialCommand(new CliSpecialCommandBuilder().name(":").
                context(ctx).
                executor(new OperationSpecialCommand(ctx, commandContext)).create());

        registerExtraCommands();

        CliOptionActivatorProvider activatorProvider = new CliOptionActivatorProvider(commandContext);

        console = new AeshConsoleBuilder()
                .commandRegistry(commandRegistry)
                .settings(settings)
                .commandInvocationProvider(services)
                .completerInvocationProvider(new CliCompleterInvocationProvider(commandContext, commandRegistry))
                .commandNotFoundHandler(new CliCommandNotFound())
                .converterInvocationProvider(new CliConverterInvocationProvider(commandContext))
                .optionActivatorProvider(activatorProvider)
                .validatorInvocationProvider(new CliValidatorInvocationProvider(commandContext))
                .manProvider(new CliManProvider())
                .prompt(interactive ? new Prompt("[disconnected /] ") : null)
                .create();

        console.setCurrentCommandInvocationProvider(PROVIDER);
    }

    private void registerExtraCommands() throws CommandLineException, CommandLineParserException {
        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);
        for (Command command : loader) {
            commandRegistry.addCommand(containerBuilder.create(command));
        }
    }

    private Settings createSettings(CliConfig config, InputStream consoleInput,
            PrintStream consoleOutput) {
        SettingsBuilder settings = new SettingsBuilder();
        if (consoleInput != null) {
            settings.inputStream(consoleInput);
        }
        if (silent) {
            settings.outputStream(new SilentPrintStream());
        } else {
            settings.outputStream(consoleOutput);
        }

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

    private CliCommandRegistry createCommandRegistry() throws CommandLineException {
        CliCommandRegistry clireg = new CliCommandRegistry(ctx, commandContext);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            console.getShell().out().println(
                    Parser.formatDisplayList(newList,
                            console.getShell().getSize().getHeight(),
                            console.getShell().getSize().getWidth()));
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void controlled() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isControlled() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void continuous() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCallback(ConsoleCallback consoleCallback) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean running() {
        //XXX JFDENISE TODO
        return true;
    }

    @Override
    public void setPrompt(String prompt) {
        console.setPrompt(new Prompt(prompt));
    }

    @Override
    public void setPrompt(String prompt, Character mask) {
        if (!prompt.equals(console.getPrompt().getPromptAsString())) {
            console.setPrompt(new Prompt(prompt, mask));
        }
    }

    @Override
    public void redrawPrompt() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                //start();
                connect();
            } catch (CommandLineException ex) {
                throw new CliInitializationException("Failed to connect to the controller", ex);
            }
        } else {
            print("You are disconnected at the moment. Type 'connect' to connect to the server or"
                    + " 'help' for the list of supported commands.");
            printNewLine();
        }
        start();
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

    @Override
    public void executeCommand(String command) throws CommandLineException {
        try {
            if (!command.isEmpty() && command.charAt(0) != '#') {
                console.executeCommand(command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandLineException(e);
        }
    }

    private void connect() throws CommandLineException {
        executeCommand("connect");
    }

    @Override
    public void process(List<String> commands, boolean connect) throws CommandLineException {
        processNonInteractive(() -> {
            for (String command : commands) {
                executeCommand(command);
            }
        }, connect);
    }

    private void processNonInteractive(CommandProcessor processor, boolean connect) throws CommandLineException {
        if (console.isRunning()) {
            throw new RuntimeException("Console is already running");
        }
        //console.start();

        if (connect) {
            connect();
        }

        try {
            processor.process();
        } finally {
            ctx.terminateSession();
            //console.stop();
        }
    }

    @Override
    public void processFile(File file, boolean connect) throws CommandLineException {
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
