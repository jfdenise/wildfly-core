/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliEvent;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.ConnectionInfo;
import org.jboss.as.cli.ControllerAddress;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.OperationCommand.HandledRequest;
import org.jboss.as.cli.RequestWithAttachments;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.batch.impl.DefaultBatchManager;
import org.jboss.as.cli.batch.impl.DefaultBatchedCommand;
import org.jboss.as.cli.handlers.EchoVariableHandler;
import org.jboss.as.cli.handlers.OperationRequestHandler;
import org.jboss.as.cli.handlers.SetVariableHandler;
import org.jboss.as.cli.handlers.UnsetVariableHandler;
import org.jboss.as.cli.handlers.batch.BatchClearHandler;
import org.jboss.as.cli.handlers.batch.BatchDiscardHandler;
import org.jboss.as.cli.handlers.batch.BatchEditLineHandler;
import org.jboss.as.cli.handlers.batch.BatchHandler;
import org.jboss.as.cli.handlers.batch.BatchHoldbackHandler;
import org.jboss.as.cli.handlers.batch.BatchListHandler;
import org.jboss.as.cli.handlers.batch.BatchMoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRemoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRunHandler;
import org.jboss.as.cli.handlers.ifelse.ElseHandler;
import org.jboss.as.cli.handlers.ifelse.EndIfHandler;
import org.jboss.as.cli.handlers.ifelse.IfHandler;
import org.jboss.as.cli.handlers.loop.DoneHandler;
import org.jboss.as.cli.handlers.loop.ForHandler;
import org.jboss.as.cli.handlers.trycatch.CatchHandler;
import org.jboss.as.cli.handlers.trycatch.EndTryHandler;
import org.jboss.as.cli.handlers.trycatch.FinallyHandler;
import org.jboss.as.cli.handlers.trycatch.TryHandler;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.NodePathFormatter;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.cli.parsing.operation.OperationFormat;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 *
 * @author jfdenise
 */
public class BootCommandContext implements CommandContext, ModelControllerClientFactory.ConnectionCloseHandler {

    private static final Logger log = Logger.getLogger(CommandContext.class);

    private static final byte RUNNING = 0;
    private static final byte TERMINATING = 1;
    private static final byte TERMINATED = 2;

    /**
     * State Tracking
     *
     * Interact - Interactive UI
     *
     * Silent - Only send input. No output. Error On Interact - If
     * non-interactive mode requests user interaction, throw an error.
     */
    private boolean SILENT = false;

    /**
     * command registry
     */
    private final CommandRegistry cmdRegistry;
    /**
     * loads command handlers from the domain management model extensions
     */
    private ExtensionsLoader extLoader;

    /**
     * whether the session should be terminated
     */
    private byte terminate;

    /**
     * current command line
     */
    private String cmdLine;
    /**
     * parsed command arguments
     */
    private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);

    /**
     * domain or standalone mode
     */
    private boolean domainMode;
    /**
     * the controller client
     */
    private ModelControllerClient client;

    /**
     * various key/value pairs
     */
    private Map<Scope, Map<String, Object>> map = new HashMap<>();
    /**
     * operation request address prefix
     */
    private final OperationRequestAddress prefix = new DefaultOperationRequestAddress();
    /**
     * the prefix formatter
     */
    private final NodePathFormatter prefixFormatter = DefaultPrefixFormatter.INSTANCE;
    /**
     * provider of operation request candidates for tab-completion
     */
    /**
     * operation request handler
     */
    private final OperationRequestHandler operationHandler;
    /**
     * batches
     */
    private BatchManager batchManager = new DefaultBatchManager();

    private List<CliEventListener> listeners = new ArrayList<CliEventListener>();

    /**
     * the value of this variable will be used as the exit code of the vm, it is
     * reset by every command/operation executed
     */
    private int exitCode;

    /**
     * whether to resolve system properties passed in as values of operation
     * parameters
     */
    private boolean resolveParameterValues;

    private Map<String, String> variables;

    /**
     * command line handling redirection
     */
    private CommandLineRedirectionRegistration redirection;

    private final CLIPrintStream cliPrintStream;

    private static final short DEFAULT_TIMEOUT = 0;
    private int timeout = DEFAULT_TIMEOUT;
    private int configTimeout;

    private boolean redefinedOutput;

    private File currentDir = new File("");
    private final CliConfig config;
    /**
     * output target
     */
    private BufferedWriter outputTarget;

    /**
     * Constructor called from Boot invoker, minimal configuration.
     *
     */
    public BootCommandContext(OutputStream output) throws CliInitializationException {
        config = CliConfigImpl.getDefault();
        operationHandler = new OperationRequestHandler();

        redefinedOutput = output != null;
        cliPrintStream = !redefinedOutput ? new CLIPrintStream() : new CLIPrintStream(output);
        this.cmdRegistry = new CommandRegistry();
        try {
            initCommands();
        } catch (CommandLineException e) {
            throw new CliInitializationException("Failed to initialize commands", e);
        }
    }

    private void initCommands() throws CommandLineException {

        // variables
        cmdRegistry.registerHandler(new SetVariableHandler(), "set");
        cmdRegistry.registerHandler(new EchoVariableHandler(true), "echo");
        cmdRegistry.registerHandler(new UnsetVariableHandler(), "unset");
        // batch commands
        cmdRegistry.registerHandler(new BatchHandler(this), "batch");
        cmdRegistry.registerHandler(new BatchDiscardHandler(), "discard-batch");
        cmdRegistry.registerHandler(new BatchListHandler(), "list-batch");
        cmdRegistry.registerHandler(new BatchHoldbackHandler(), "holdback-batch");
        cmdRegistry.registerHandler(new BatchRunHandler(this), "run-batch");
        cmdRegistry.registerHandler(new BatchClearHandler(), "clear-batch");
        cmdRegistry.registerHandler(new BatchRemoveLineHandler(), "remove-batch-line");
        cmdRegistry.registerHandler(new BatchMoveLineHandler(), "move-batch-line");
        cmdRegistry.registerHandler(new BatchEditLineHandler(), "edit-batch-line");

        // try-catch
        cmdRegistry.registerHandler(new TryHandler(), "try");
        cmdRegistry.registerHandler(new CatchHandler(), "catch");
        cmdRegistry.registerHandler(new FinallyHandler(), "finally");
        cmdRegistry.registerHandler(new EndTryHandler(), "end-try");

        // if else
        cmdRegistry.registerHandler(new IfHandler(), "if");
        cmdRegistry.registerHandler(new ElseHandler(), "else");
        cmdRegistry.registerHandler(new EndIfHandler(), "end-if");

        // for
        cmdRegistry.registerHandler(new ForHandler(), "for");
        cmdRegistry.registerHandler(new DoneHandler(), "done");
    }

    protected void setOutputTarget(String filePath) {
        if (filePath == null) {
            this.outputTarget = null;
            return;
        }
        try {
            if (filePath.startsWith(">")) {
                File f = new File(filePath.substring(1).trim());
                this.outputTarget = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } else {
                File f = new File(filePath);
                this.outputTarget = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            error(e.getLocalizedMessage());
            return;
        }
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public boolean isTerminated() {
        return terminate == TERMINATED;
    }

    private StringBuilder lineBuffer;
    private StringBuilder origLineBuffer;
    private CommandExecutor executor = new CommandExecutor(this);

    @Override
    public void handle(String line) throws CommandLineException {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return; // ignore comments
        }

        int i = line.length() - 1;
        while (i > 0 && line.charAt(i) <= ' ') {
            if (line.charAt(--i) == '\\') {
                break;
            }
        }
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
            line = lineBuffer.toString();
            lineBuffer = null;
        }

        resetArgs(line);

        try {
            if (redirection != null) {
                redirection.target.handle(this);
            } else {
                if (parsedCmd.getFormat() == OperationFormat.INSTANCE) {
                    handleOperation(parsedCmd);
                } else {
                    final String cmdName = parsedCmd.getOperationName();
                    CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
                    if (handler == null) {
                        throw new CommandLineException("Unexpected command '" + cmdName + "'. Type 'help --commands' for the list of supported commands.");
                    }
                    handleLegacyCommand(line, handler, false);
                }
            }
        } catch (CommandLineException e) {
            throw e;
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debugf(t, "Failed to handle '%s'", line);
            }
            throw new CommandLineException("Failed to handle '" + line + "'", t);
        } finally {
            // so that getArgumentsString() doesn't return this line
            // during the tab-completion of the next command
            cmdLine = null;
            clear(Scope.REQUEST);
        }
    }

    // Method called for if condition and low level operation to be guarded by a timeout.
    @Override
    public ModelNode execute(Operation mn, String description) throws CommandLineException, IOException {
        if (client == null) {
            throw new CommandLineException("The connection to the controller "
                    + "has not been established.");
        }
        try {
            return execute(() -> {
                return executor.execute(mn, timeout, TimeUnit.SECONDS);
            }, description);
        } catch (CommandLineException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            } else {
                throw ex;
            }
        }
    }

    @Override
    public ModelNode execute(ModelNode mn, String description) throws CommandLineException, IOException {
        OperationBuilder opBuilder = new OperationBuilder(mn, true);
        return execute(opBuilder.build(), description);
    }

    // Single execute method to handle exceptions.
    private <T> T execute(Callable<T> c, String msg) throws CommandLineException {
        try {
            return c.call();
        } catch (IOException ex) {
            throw new CommandLineException("IO exception for " + msg, ex);
        } catch (TimeoutException ex) {
            throw new CommandLineException("Timeout exception for " + msg);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CommandLineException("Interrupt exception for " + msg);
        } catch (ExecutionException ex) {
            // Part of command parsing can occur at execution time.
            if (ex.getCause() instanceof CommandFormatException) {
                throw new CommandFormatException(ex);
            } else {
                throw new CommandLineException(ex);
            }
        } catch (CommandLineException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CommandLineException("Exception for " + msg, ex);
        }
    }

    public void handleSafe(String line) {
        exitCode = 0;
        try {
            handle(line);
        } catch (Throwable t) {
            // Remove exceptions that are wrappers to not pollute error message.
            if (t instanceof CommandLineException
                    && t.getCause() instanceof ExecutionException) {
                // Get the ExecutionException cause.
                Throwable cause = t.getCause().getCause();
                if (cause != null) {
                    t = cause;
                }
            }
            error(Util.getMessagesFromThrowable(t));
        }
    }

    @Override
    public String getArgumentsString() {
        // a little hack to support tab-completion of commands and ops spread across multiple lines
        if (lineBuffer != null) {
            return lineBuffer.toString();
        }
        if (cmdLine != null && parsedCmd.getOperationName() != null) {
            int cmdNameLength = parsedCmd.getOperationName().length();
            if (cmdLine.length() == cmdNameLength) {
                return null;
            } else {
                return cmdLine.substring(cmdNameLength + 1);
            }
        }
        return null;
    }

    @Override
    public void terminateSession() {
        if (terminate == RUNNING) {
            clear(Scope.CONTEXT);
            clear(Scope.REQUEST);
            terminate = TERMINATING;
            disconnectController();
            executor.cancel();
            terminate = TERMINATED;
        }
    }

    // Only collect when called directly on context.
    public void print(String message, boolean newLine, boolean collect) {
        if (message == null) {
            return;
        }
        final Level logLevel;
        if (exitCode != 0) {
            logLevel = Level.ERROR;
        } else {
            logLevel = Level.INFO;
        }
        if (log.isEnabled(logLevel)) {
            log.log(logLevel, message);
        }
        if (outputTarget != null) {
            try {
                outputTarget.append(message);
                if (newLine) {
                    outputTarget.newLine();
                }
                outputTarget.flush();
            } catch (IOException e) {
                System.err.println("Failed to print '" + message + "' to the output target: " + e.getLocalizedMessage());
            }
            return;
        }
        // Could be a redirection at the aesh command or operation level
        if (!SILENT) {
            cliPrintStream.println(message);
        }
    }

    @Override
    public void printDMR(ModelNode node, boolean compact) {
        if (getConfig().isOutputJSON()) {
            printLine(node.toJSONString(compact), node.get("outcome").asString());
        } else if (getConfig().isOutputJSON()) {
            printLine(node.toJSONString(compact));
        } else {
            if (compact) {
                printLine(Util.compactToString(node));
            } else {
                printLine(node.toString());
            }
        }
    }

//    public final boolean isColorOutput() {
//        return false;
//    }
    @Override
    public void printLine(String message) {

        print(message, true, true);
    }

    public void printLine(String message, String outcome) {

        print(message, true, true);
    }

    @Override
    public void print(String message) {

        print(message, false, true);
    }

    /**
     * Set the exit code of the process to indicate an error and output the
     * error message.
     *
     * WARNING This method should only be called for unrecoverable errors as
     * once the exit code is set subsequent operations may not be possible.
     *
     * @param message The message to display.
     */
    protected void error(String message) {
        this.exitCode = 1;
        printLine(message, "error");
    }

//    // Aesh input API expects InterruptedException so is implemented with input methods
//    // That is done in the Shell implementation (CLICommandInvocationBuilder)
//    public String input(String prompt, boolean password) throws CommandLineException, InterruptedException, IOException {
//        Prompt pr;
//        if (password) {
//            pr = new Prompt(prompt, (char) 0x00);
//        } else {
//            pr = new Prompt(prompt);
//        }
//        return input(pr);
//    }
//
//    // Aesh input API expects InterruptedException so is implemented with input methods
//    // That is done in the Shell implementation (CLICommandInvocationBuilder)
//    public String input(Prompt prompt) throws CommandLineException, InterruptedException, IOException {
//        // Only fail an interact if we're not in interactive.
//        interactionDisabled();
//        return null;
//    }
//
//    // Aesh input API expects InterruptedException so is implemented with input methods
//    // That is done in the Shell implementation (CLICommandInvocationBuilder)
//    public int[] input() throws CommandLineException, InterruptedException, IOException {
//        interactionDisabled();
//        return null;
//    }
    protected void interactionDisabled() throws CommandLineException {
        throw new CommandLineException("Invalid Usage. Prompt attempted in non-interactive mode. Please check commands or change CLI mode.");
    }

    @Override
    public void printColumns(Collection<String> col) {
        if (col == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info(col);
        }
        String columns = null;
        if (!SILENT) {
        }
        if (columns == null) {
            for (String s : col) {
                print(s, true, true);
            }
        } else {
            print(columns, false, true);
        }
    }

    @Override
    public void set(Scope scope, String key, Object value) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(key);
        Map<String, Object> store = map.get(scope);
        if (store == null) {
            store = new HashMap<>();
            map.put(scope, store);
        }
        store.put(key, value);
    }

    @Override
    public Object get(Scope scope, String key) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(key);
        Map<String, Object> store = map.get(scope);
        Object value = null;
        if (store != null) {
            value = store.get(key);
        }
        return value;
    }

    @Override
    public void clear(Scope scope) {
        Objects.requireNonNull(scope);
        Map<String, Object> store = map.remove(scope);
        if (store != null) {
            store.clear();
        }
    }

    @Override
    public Object remove(Scope scope, String key) {
        Objects.requireNonNull(scope);
        Map<String, Object> store = map.get(scope);
        Object value = null;
        if (store != null) {
            value = store.remove(key);
        }
        return value;
    }

    @Override
    public ModelControllerClient getModelControllerClient() {
        return client;
    }

    @Override
    public CommandLineParser getCommandLineParser() {
        return new DefaultOperationRequestParser(this);
    }

    @Override
    public OperationRequestAddress getCurrentNodePath() {
        return prefix;
    }

    @Override
    public NodePathFormatter getNodePathFormatter() {

        return prefixFormatter;
    }

    @Override
    public OperationCandidatesProvider getOperationCandidatesProvider() {
        return null;
    }

    @Override
    public void connectController() throws CommandLineException {
        connectController(null);
    }

    @Override
    public void connectController(String controller) throws CommandLineException {
        connectController(controller, null);
    }

    @Override
    public void connectController(String controller, String clientAddress) throws CommandLineException {
        throw new RuntimeException();
    }

    @Override
    @Deprecated
    public void connectController(String host, int port) throws CommandLineException {
        throw new RuntimeException();
    }

    @Override
    public void bindClient(ModelControllerClient newClient) {
        ConnectionInfoBean conInfo = new ConnectionInfoBean();
        conInfo.setLoggedSince(new Date());
        initNewClient(newClient, null, conInfo);
    }

    private void initNewClient(ModelControllerClient newClient, ControllerAddress address, ConnectionInfoBean conInfo) {
        if (newClient != null) {
            client = newClient;
        }
    }

    @Override
    public File getCurrentDir() {
        return currentDir;
    }

    @Override
    public void setCurrentDir(File dir) {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        this.currentDir = dir;
    }

    @Override
    public void registerRedirection(CommandLineRedirection redirection) throws CommandLineException {
        if (this.redirection != null) {
            throw new CommandLineException("Another redirection is currently active.");
        }
        this.redirection = new CommandLineRedirectionRegistration(redirection);
        redirection.set(this.redirection);
    }

    @Override
    public void disconnectController() {

    }

    @Override
    @Deprecated
    public String getDefaultControllerHost() {
        return null;
    }

    @Override
    @Deprecated
    public int getDefaultControllerPort() {
        return -1;
    }

    @Override
    public ControllerAddress getDefaultControllerAddress() {
        return null;
    }

    @Override
    public String getControllerHost() {
        return null;
    }

    @Override
    public int getControllerPort() {
        return -1;
    }

    @Override
    public void clearScreen() {
    }

    @Override
    public CommandHistory getHistory() {
        return null;
    }

    private void resetArgs(String cmdLine) throws CommandFormatException {
        if (cmdLine != null) {
            parsedCmd.parse(prefix, cmdLine, this, redirection != null);
            setOutputTarget(parsedCmd.getOutputTarget());
        }
        this.cmdLine = cmdLine;
    }

    @Override
    public boolean isBatchMode() {
        return batchManager.isBatchActive();
    }

    @Override
    public boolean isWorkflowMode() {
        return redirection != null;
    }

    @Override
    public BatchManager getBatchManager() {
        return batchManager;
    }

    @Override
    public BatchedCommand toBatchedCommand(String line) throws CommandFormatException {
        HandledRequest req = buildRequest(line, true);
        return new DefaultBatchedCommand(this, line, req.getRequest(), req.getResponseHandler());
    }

    @Override
    public ModelNode buildRequest(String line) throws CommandFormatException {
        return buildRequest(line, false).getRequest();
    }

    protected HandledRequest buildRequest(String line, boolean batchMode) throws CommandFormatException {

        if (line == null || line.isEmpty()) {
            throw new OperationFormatException("The line is null or empty.");
        }

        final DefaultCallbackHandler originalParsedArguments = this.parsedCmd;
        final String originalCmdLine = this.cmdLine;
        try {
            this.parsedCmd = new DefaultCallbackHandler();
            resetArgs(line);

            if (parsedCmd.getFormat() == OperationFormat.INSTANCE) {
                final ModelNode request = this.parsedCmd.toOperationRequest(this);
                StringBuilder op = new StringBuilder();
                op.append(prefixFormatter.format(parsedCmd.getAddress()));
                op.append(line.substring(line.indexOf(':')));
                return new HandledRequest(request, null);
            }
            final CommandHandler handler = cmdRegistry.getCommandHandler(parsedCmd.getOperationName());
            if (handler != null) {
                if (batchMode) {
                    if (!handler.isBatchMode(this)) {
                        throw new OperationFormatException("The command is not allowed in a batch.");
                    }
                    Batch batch = getBatchManager().getActiveBatch();
                    return ((OperationCommand) handler).buildHandledRequest(this, batch.getAttachments());
                } else if (!(handler instanceof OperationCommand)) {
                    throw new OperationFormatException("The command does not translate to an operation request.");
                }
                return new HandledRequest(((OperationCommand) handler).buildRequest(this), null);
            }
            throw new CommandFormatException("Unknown command " + parsedCmd.getOperationName());
        } finally {
            clear(Scope.REQUEST);
            this.parsedCmd = originalParsedArguments;
            this.cmdLine = originalCmdLine;
        }
    }

    private void handleOperation(ParsedCommandLine parsedLine) throws CommandFormatException, CommandLineException {
        if (isBatchMode()) {
            String line = parsedLine.getOriginalLine();
            Batch batch = getBatchManager().getActiveBatch();
            final ModelNode request = Util.toOperationRequest(this,
                    parsedLine, batch.getAttachments());
            StringBuilder op = new StringBuilder();
            op.append(getNodePathFormatter().format(parsedLine.getAddress()));
            op.append(line.substring(line.indexOf(':')));
            DefaultBatchedCommand batchedCmd
                    = new DefaultBatchedCommand(this, op.toString(), request, null);
            batch.add(batchedCmd);
        } else {
            Attachments attachments = new Attachments();
            final ModelNode op = Util.toOperationRequest(BootCommandContext.this,
                    parsedLine, attachments);
            RequestWithAttachments req = new RequestWithAttachments(op, attachments);
            set(Scope.REQUEST, "OP_REQ", req);
            operationHandler.handle(this);
        }
    }

    private void handleLegacyCommand(String opLine, CommandHandler handler, boolean direct) throws CommandLineException {
        if (isBatchMode() && handler.isBatchMode(this)) {
            if (!(handler instanceof OperationCommand)) {
                throw new CommandLineException("The command is not allowed in a batch.");
            } else {
                try {
                    Batch batch = getBatchManager().getActiveBatch();
                    HandledRequest request = ((OperationCommand) handler).buildHandledRequest(this,
                            batch.getAttachments());
                    BatchedCommand batchedCmd
                            = new DefaultBatchedCommand(this, opLine,
                                    request.getRequest(), request.getResponseHandler());
                    batch.add(batchedCmd);
                } catch (CommandFormatException e) {
                    throw new CommandFormatException("Failed to add to batch '" + opLine + "'", e);
                }
            }
        } else if (direct) {
            handler.handle(BootCommandContext.this);
        } else {
            execute(() -> {
                executor.execute(handler, timeout, TimeUnit.SECONDS);
                return null;
            }, opLine);
        }
    }

    @Override
    public CommandLineCompleter getDefaultCommandCompleter() {
        return null;
    }

    @Override
    public ParsedCommandLine getParsedCommandLine() {
        return parsedCmd;
    }

    @Override
    public boolean isDomainMode() {
        return domainMode;
    }

    @Override
    public void addEventListener(CliEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null.");
        }
        listeners.add(listener);
    }

    @Override
    public CliConfig getConfig() {
        return config;
    }

    protected void notifyListeners(CliEvent event) {
        for (CliEventListener listener : listeners) {
            listener.cliEvent(event, this);
        }
    }

    @Override
    public void interact() {
        throw new IllegalStateException("The console hasn't been initialized at construction time.");
    }

    @Override
    public boolean isResolveParameterValues() {
        return resolveParameterValues;
    }

    @Override
    public void setResolveParameterValues(boolean resolve) {
        this.resolveParameterValues = resolve;
    }

    @Override
    public void handleClose() {

    }

    @Override
    public boolean isSilent() {
        return SILENT;
    }

    @Override
    public void setSilent(boolean silent) {
        SILENT = silent;
    }

    @Override
    public int getTerminalWidth() {
        return 80;
    }

    @Override
    public int getTerminalHeight() {
        return 24; // WFCORE-3540 24 has no special meaning except that it is a value greater than 0
    }

    @Override
    public void setVariable(String name, String value) throws CommandLineException {
        if (name == null || name.isEmpty()) {
            throw new CommandLineException("Variable name can't be null or an empty string");
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            throw new CommandLineException("Variable name must be a valid Java identifier (and not contain '$'): '" + name + "'");
        }
        for (int i = 1; i < name.length(); ++i) {
            final char c = name.charAt(i);
            if (!Character.isJavaIdentifierPart(c) || c == '$') {
                throw new CommandLineException("Variable name must be a valid Java identifier (and not contain '$'): '" + name + "'");
            }
        }

        if (value == null) {
            if (variables == null) {
                return;
            }
            variables.remove(name);
        } else {
            if (variables == null) {
                variables = new HashMap<String, String>();
            }
            variables.put(name, value);
        }
    }

    @Override
    public String getVariable(String name) {
        return variables == null ? null : variables.get(name);
    }

    @Override
    public Collection<String> getVariables() {
        return variables == null ? Collections.<String>emptySet() : variables.keySet();
    }

    class CommandLineRedirectionRegistration implements CommandLineRedirection.Registration {

        CommandLineRedirection target;

        CommandLineRedirectionRegistration(CommandLineRedirection redirection) {
            if (redirection == null) {
                throw new IllegalArgumentException("Redirection is null");
            }
            this.target = redirection;
        }

        @Override
        public void unregister() throws CommandLineException {
            ensureActive();
            BootCommandContext.this.redirection = null;
        }

        @Override
        public boolean isActive() {
            return BootCommandContext.this.redirection == this;
        }

        @Override
        public void handle(ParsedCommandLine parsedLine) throws CommandLineException {

            ensureActive();

            try {
                /**
                 * All kind of command can be handled by handleCommand. In order
                 * to stay on the safe side (another parsing is applied on top
                 * of operations and legacy commands by aesh, we only use the
                 * wrapped approach if an operator is present. This could be
                 * simplified when we have confidence that aesh parsing doesn't
                 * fail for complex corner cases.
                 */
                if (parsedCmd.getFormat() == OperationFormat.INSTANCE) {
                    handleOperation(parsedCmd);
                } else {
                    final String cmdName = parsedCmd.getOperationName();
                    CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
                    if (handler != null) {
                        handleLegacyCommand(parsedLine.getOriginalLine(), handler, true);
                    } else {
                        throw new CommandLineException("Unexpected command '" + cmdName + "'. Type 'help --commands' for the list of supported commands.");
                    }
                }
            } finally {
                clear(Scope.REQUEST);
            }
        }

        private void ensureActive() throws CommandLineException {
            if (!isActive()) {
                throw new CommandLineException("The redirection is not registered any more.");
            }
        }
    }

    public ConnectionInfo getConnectionInfo() {
        return null;
    }

    @Override
    public void captureOutput(PrintStream captor) {
        assert captor != null;
        cliPrintStream.captureOutput(captor);
    }

    @Override
    public void releaseOutput() {
        cliPrintStream.releaseOutput();
    }

    @Override
    public final void setCommandTimeout(int numSeconds) {
        if (numSeconds < 0) {
            throw new IllegalArgumentException("The command-timeout must be a "
                    + "valid positive integer:" + numSeconds);
        }
        this.timeout = numSeconds;
    }

    @Override
    public final int getCommandTimeout() {
        return timeout;
    }

    @Override
    public final void resetTimeout(TIMEOUT_RESET_VALUE value) {
        switch (value) {
            case CONFIG: {
                timeout = configTimeout;
                break;
            }
            case DEFAULT: {
                timeout = DEFAULT_TIMEOUT;
                break;
            }
        }
    }

//    // Required by CLICommandInvocationBuilder
//    // in order to expose a CommandContext
//    // that properly handles timeout.
//    public CommandContext newTimeoutCommandContext() {
//        return executor.newTimeoutCommandContext(this);
//    }
}
