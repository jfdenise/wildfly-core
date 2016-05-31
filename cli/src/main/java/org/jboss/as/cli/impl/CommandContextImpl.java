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
package org.jboss.as.cli.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.sasl.SaslException;
import org.jboss.aesh.cl.parser.CommandLineParserException;

import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliEvent;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHandlerProvider;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.ConnectionInfo;
import org.jboss.as.cli.ControllerAddress;
import org.jboss.as.cli.ControllerAddressResolver;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.batch.impl.DefaultBatchManager;
import org.jboss.as.cli.batch.impl.DefaultBatchedCommand;
import org.jboss.as.cli.embedded.EmbeddedControllerHandlerRegistrar;
import org.jboss.as.cli.embedded.EmbeddedProcessLaunch;
import org.jboss.as.cli.handlers.ArchiveHandler;
import org.jboss.as.cli.handlers.ClearScreenHandler;
import org.jboss.as.cli.handlers.CommandCommandHandler;
import org.jboss.as.cli.handlers.ConnectionInfoHandler;
import org.jboss.as.cli.handlers.DeployHandler;
import org.jboss.as.cli.handlers.DeploymentInfoHandler;
import org.jboss.as.cli.handlers.DeploymentOverlayHandler;
import org.jboss.as.cli.handlers.EchoDMRHandler;
import org.jboss.as.cli.handlers.EchoVariableHandler;
import org.jboss.as.cli.handlers.GenericTypeOperationHandler;
import org.jboss.as.cli.handlers.HelpHandler;
import org.jboss.as.cli.handlers.HistoryHandler;
import org.jboss.as.cli.handlers.LsHandler;
import org.jboss.as.cli.handlers.OperationRequestHandler;
import org.jboss.as.cli.handlers.PrefixHandler;
import org.jboss.as.cli.handlers.PrintWorkingNodeHandler;
import org.jboss.as.cli.handlers.QuitHandler;
import org.jboss.as.cli.handlers.ReadAttributeHandler;
import org.jboss.as.cli.handlers.ReadOperationHandler;
import org.jboss.as.cli.handlers.ReloadHandler;
import org.jboss.as.cli.handlers.SetVariableHandler;
import org.jboss.as.cli.handlers.ShutdownHandler;
import org.jboss.as.cli.handlers.UndeployHandler;
import org.jboss.as.cli.handlers.UnsetVariableHandler;
import org.jboss.as.cli.handlers.VersionHandler;
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
import org.jboss.as.cli.handlers.jca.DataSourceAddCompositeHandler;
import org.jboss.as.cli.handlers.jca.JDBCDriverInfoHandler;
import org.jboss.as.cli.handlers.jca.JDBCDriverNameProvider;
import org.jboss.as.cli.handlers.jca.XADataSourceAddCompositeHandler;
import org.jboss.as.cli.handlers.module.ASModuleHandler;
import org.jboss.as.cli.handlers.trycatch.CatchHandler;
import org.jboss.as.cli.handlers.trycatch.EndTryHandler;
import org.jboss.as.cli.handlers.trycatch.FinallyHandler;
import org.jboss.as.cli.handlers.trycatch.TryHandler;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.NodePathFormatter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.cli.operation.impl.RolloutPlanCompleter;
import org.jboss.as.cli.parsing.command.CommandFormat;
import org.jboss.as.cli.parsing.operation.OperationFormat;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.GeneralTimeoutHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.stdio.StdioContext;
import org.xnio.http.RedirectException;
import org.jboss.as.cli.console.ConsoleBuilder;
import org.jboss.as.cli.handlers.ConnectHandler;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.cli.security.AuthenticationCallbackHandler;
import org.jboss.as.cli.security.CliSSLContext;

/**
 *
 * @author Alexey Loubyansky
 */
// XXX JFDENISE, is public for now, will be package when dependency moved to this package
public class CommandContextImpl implements CommandContext, ModelControllerClientFactory.ConnectionCloseHandler {

    private static final Logger log = Logger.getLogger(CommandContext.class);

    private static final byte RUNNING = 0;
    private static final byte TERMINATING = 1;
    private static final byte TERMINATED = 2;

    /** the cli configuration */
    private final CliConfig config;
    private final ControllerAddressResolver addressResolver;

    /** command registry */
    private final CommandRegistry cmdRegistry;
    /** loads command handlers from the domain management model extensions */
    private ExtensionsLoader extLoader;

    /**
     * At some point the Context shouldn't have a reference to the console.
     * @deprecated
     */
    @Deprecated
    private final Console console;

    /** whether the session should be terminated */
    private byte terminate;

    /** current command line */
    private String cmdLine;
    /** parsed command arguments */
    private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);

    /** domain or standalone mode */
    private boolean domainMode;
    /** the controller client */
    private ModelControllerClient client;

    /** the address of the current controller */
    private ControllerAddress currentAddress;
    /** the command line specified username */
    private final String username;
    /** the command line specified password */
    private final char[] password;
    /** flag to disable the local authentication mechanism */
    private final boolean disableLocalAuth;
    /** The SSLContext when managed by the CLI */
    private CliSSLContext sslContext;
    /** various key/value pairs */
    private Map<Scope, Map<String, Object>> map = new HashMap<>();
    /** operation request address prefix */
    private final OperationRequestAddress prefix = new DefaultOperationRequestAddress();
    /** the prefix formatter */
    private final NodePathFormatter prefixFormatter = DefaultPrefixFormatter.INSTANCE;
    /** provider of operation request candidates for tab-completion */
    private final OperationCandidatesProvider operationCandidatesProvider;
    /** operation request handler */
    private final OperationRequestHandler operationHandler;
    /** batches */
    private BatchManager batchManager = new DefaultBatchManager();
    /** the default command completer */
    private final CommandCompleter cmdCompleter;
    /** the timeout handler */
    private final GeneralTimeoutHandler timeoutHandler = new GeneralTimeoutHandler();
    /** the client bind address */
    private final String clientBindAddress;

    /** output target */
    private BufferedWriter outputTarget;

    private List<CliEventListener> listeners = new ArrayList<CliEventListener>();

    /** the value of this variable will be used as the exit code of the vm, it is reset by every command/operation executed */
    private int exitCode;

    private File currentDir = new File("");

    /** whether to resolve system properties passed in as values of operation parameters*/
    private boolean resolveParameterValues;

    private Map<String, String> variables;

    /** command line handling redirection */
    private CommandLineRedirectionRegistration redirection;

    /** this object saves information to be used in ConnectionInfoHandler */
    private ConnectionInfoBean connInfoBean;

    // Store a ref to the default input stream aesh will use before we do any manipulation of stdin
    //private InputStream stdIn = new SettingsBuilder().create().getInputStream();
    private boolean uninstallIO;

    private CliShutdownHook.Handler shutdownHook;

    private static JaasConfigurationWrapper jaasConfigurationWrapper; // we want this wrapper to be only created once

    /**
     * Version mode - only used when --version is called from the command line.
     *
     * @throws CliInitializationException
     */
    CommandContextImpl() throws CliInitializationException,
            CommandLineParserException, CommandLineException {
        this(new CommandContextConfiguration.Builder().build());
    }

    /**
     * Default constructor used for both interactive and non-interactive mode.
     *
     */
    CommandContextImpl(CommandContextConfiguration configuration) throws
            CliInitializationException, CommandLineParserException,
            CommandLineException {
        config = CliConfigImpl.load(this, configuration);
        addressResolver = ControllerAddressResolver.newInstance(config, configuration.getController());

        operationHandler = new OperationRequestHandler();

        this.username = configuration.getUsername();
        this.password = configuration.getPassword();
        this.disableLocalAuth = configuration.isDisableLocalAuth();
        this.clientBindAddress = configuration.getClientBindAddress();
        this.console = new ConsoleBuilder().setContext(this).
                setConsoleInputStream(configuration.getConsoleInput()).
                setConsoleOutputStream(configuration.getConsoleOutput()).
                setErrorOnInteract(configuration.isErrorOnInteract()).
                setInteractive(configuration.isInitConsole()).
                setSilent(configuration.isSilent()).
                create();
        resolveParameterValues = config.isResolveParameterValues();
        initStdIO();
        cmdRegistry = console.getLegacyCommandRegistry();
        try {
            initCommands();
        } catch (CommandLineException e) {
            throw new CliInitializationException("Failed to initialize commands", e);
        }
        sslContext = new CliSSLContext(config.getSslConfig(), timeoutHandler,
                (X509Certificate[] chain) -> {
                    if (connInfoBean == null) {
                        connInfoBean = new ConnectionInfoBean();
                        connInfoBean.setServerCertificates(chain);
                    }
                }, console);
        if (configuration.isInitConsole() || configuration.getConsoleInput() != null) {
            cmdCompleter = new CommandCompleter(cmdRegistry);
            this.operationCandidatesProvider = new DefaultOperationCandidatesProvider();
        } else {
            this.cmdCompleter = null;
            this.operationCandidatesProvider = null;
        }
        initJaasConfig();
        addShutdownHook();
        CliLauncher.runcom(this);
    }

    protected void addShutdownHook() {
        shutdownHook = new CliShutdownHook.Handler() {
            @Override
            public void shutdown() {
                console.interrupt();
                terminateSession();
            }
        };
        CliShutdownHook.add(shutdownHook);
    }

    private void initStdIO() {
        try {
            StdioContext.install();
            this.uninstallIO = true;
        } catch (IllegalStateException e) {
            this.uninstallIO = false;
        }
    }

    private void restoreStdIO() {
        if (uninstallIO) {
            try {
                StdioContext.uninstall();
            } catch (IllegalStateException ignored) {
                // someone else must have uninstalled
            }
        }
    }

    private void initCommands() throws CommandLineException {
        cmdRegistry.registerHandler(new PrefixHandler(), "cd", "cn");
        cmdRegistry.registerHandler(new ClearScreenHandler(), "clear", "cls");
        cmdRegistry.registerHandler(new CommandCommandHandler(cmdRegistry), "command");
        cmdRegistry.registerHandler(new ConnectHandler(), "connect");
        cmdRegistry.registerHandler(new EchoDMRHandler(), "echo-dmr");
        cmdRegistry.registerHandler(new HelpHandler(cmdRegistry), "help", "h");
        cmdRegistry.registerHandler(new HistoryHandler(), "history");
        cmdRegistry.registerHandler(new LsHandler(this), "ls");
        cmdRegistry.registerHandler(new ASModuleHandler(this), "module");
        cmdRegistry.registerHandler(new PrintWorkingNodeHandler(), "pwd", "pwn");
        cmdRegistry.registerHandler(new QuitHandler(), "quit", "q", "exit");
        cmdRegistry.registerHandler(new ReadAttributeHandler(this), "read-attribute");
        cmdRegistry.registerHandler(new ReadOperationHandler(this), "read-operation");
        cmdRegistry.registerHandler(new VersionHandler(), "version");
        cmdRegistry.registerHandler(new ConnectionInfoHandler(), "connection-info");

        // variables
        cmdRegistry.registerHandler(new SetVariableHandler(), "set");
        cmdRegistry.registerHandler(new EchoVariableHandler(), "echo");
        cmdRegistry.registerHandler(new UnsetVariableHandler(), "unset");

        // deployment
        cmdRegistry.registerHandler(new DeployHandler(this), "deploy");
        cmdRegistry.registerHandler(new UndeployHandler(this), "undeploy");
        cmdRegistry.registerHandler(new DeploymentInfoHandler(this), "deployment-info");
        cmdRegistry.registerHandler(new DeploymentOverlayHandler(this), "deployment-overlay");

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

        // data-source
        final DefaultCompleter driverNameCompleter = new DefaultCompleter(JDBCDriverNameProvider.INSTANCE);
        final GenericTypeOperationHandler dsHandler = new GenericTypeOperationHandler(this, "/subsystem=datasources/data-source", null);
        dsHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        // override the add operation with the handler that accepts connection props
        final DataSourceAddCompositeHandler dsAddHandler = new DataSourceAddCompositeHandler(this, "/subsystem=datasources/data-source");
        dsAddHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        dsHandler.addHandler(Util.ADD, dsAddHandler);
        cmdRegistry.registerHandler(dsHandler, "data-source");
        final GenericTypeOperationHandler xaDsHandler = new GenericTypeOperationHandler(this, "/subsystem=datasources/xa-data-source", null);
        xaDsHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        // override the xa add operation with the handler that accepts xa props
        final XADataSourceAddCompositeHandler xaDsAddHandler = new XADataSourceAddCompositeHandler(this, "/subsystem=datasources/xa-data-source");
        xaDsAddHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        xaDsHandler.addHandler(Util.ADD, xaDsAddHandler);
        cmdRegistry.registerHandler(xaDsHandler, "xa-data-source");
        cmdRegistry.registerHandler(new JDBCDriverInfoHandler(this), "jdbc-driver-info");

        // rollout plan
        final GenericTypeOperationHandler rolloutPlan = new GenericTypeOperationHandler(this, "/management-client-content=rollout-plans/rollout-plan", null);
        rolloutPlan.addValueConverter("content", HeadersArgumentValueConverter.INSTANCE);
        rolloutPlan.addValueCompleter("content", RolloutPlanCompleter.INSTANCE);
        cmdRegistry.registerHandler(rolloutPlan, "rollout-plan");

        // supported but hidden from tab-completion until stable implementation
        cmdRegistry.registerHandler(new ArchiveHandler(this), false, "archive");

        final AtomicReference<EmbeddedProcessLaunch> embeddedServerLaunch = EmbeddedControllerHandlerRegistrar.registerEmbeddedCommands(cmdRegistry, this);
        cmdRegistry.registerHandler(new ReloadHandler(this, embeddedServerLaunch), "reload");
        cmdRegistry.registerHandler(new ShutdownHandler(this, embeddedServerLaunch), "shutdown");
        registerExtraHandlers();

        extLoader = new ExtensionsLoader(cmdRegistry, this);
    }

    private void registerExtraHandlers() throws CommandLineException {
        ServiceLoader<CommandHandlerProvider> loader = ServiceLoader.load(CommandHandlerProvider.class);
        for (CommandHandlerProvider provider : loader) {
            cmdRegistry.registerHandler(provider.createCommandHandler(this), provider.isTabComplete(), provider.getNames());
        }
    }

    public int getExitCode() {
        return exitCode;
    }

    /**
     * The underlying SASL mechanisms may require a JAAS definition, unless a more specific definition as been provided use our
     * own definition for GSSAPI.
     */
    private void initJaasConfig() {
        // create the wrapper only once to avoid memory leak
        if (jaasConfigurationWrapper == null) {
            Configuration coreConfig = null;

            try {
                coreConfig = SecurityActions.getGlobalJaasConfiguration();
            } catch (SecurityException e) {
                log.debug("Unable to obtain default configuration", e);
            }

            jaasConfigurationWrapper = new JaasConfigurationWrapper(coreConfig);
            SecurityActions.setGlobalJaasConfiguration(jaasConfigurationWrapper);
        }
    }

    @Override
    public boolean isTerminated() {
        return terminate == TERMINATED;
    }

    private StringBuilder lineBuffer;

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
            }
            lineBuffer.append(line, 0, i);
            lineBuffer.append(' ');
            return;
        } else if (lineBuffer != null) {
            lineBuffer.append(line);
            line = lineBuffer.toString();
            lineBuffer = null;
        }

        resetArgs(line);
        try {
            if(redirection != null) {
                redirection.target.handle(this);
            } else if (parsedCmd.getFormat() == OperationFormat.INSTANCE) {
                handleOperation(parsedCmd);
            } else {
                final String cmdName = parsedCmd.getOperationName();
                // XXX JF DENISE, NEED TO BRIDGE BOTH REGISTRIES...
                // FOR BACKEWARD COMPAT
                CommandHandler handler = console.getLegacyCommandRegistry().getCommandHandler(cmdName.toLowerCase());
                if (handler != null) {
                    if (isBatchMode() && handler.isBatchMode(this)) {
                        if (!(handler instanceof OperationCommand)) {
                            throw new CommandLineException("The command is not allowed in a batch.");
                        } else {
                            try {
                                ModelNode request = ((OperationCommand) handler).buildRequest(this);
                                addBatchOperation(request, line);
                            } catch (CommandFormatException e) {
                                throw new CommandFormatException("Failed to add to batch '" + line + "'", e);
                            }
                        }
                    } else {
                        handler.handle(this);
                    }
                } else {
                    throw new CommandLineException("Unexpected command '" + line + "'. Type 'help --commands' for the list of supported commands.");
                }
            }
        } catch (CommandLineException e) {
            throw e;
        } catch(Throwable t) {
            if(log.isDebugEnabled()) {
                log.debug("Failed to handle '" + line + "'", t);
            }
            throw new CommandLineException("Failed to handle '" + line + "'", t);
        } finally {
            // so that getArgumentsString() doesn't return this line
            // during the tab-completion of the next command
            cmdLine = null;
            clear(Scope.REQUEST);
        }
    }

    public void handleSafe(String line) {
        exitCode = 0;
        try {
            handle(line);
        } catch(Throwable t) {
            error(Util.getMessagesFromThrowable(t));
        }
    }

    @Override
    public String getArgumentsString() {
        // a little hack to support tab-completion of commands and ops spread across multiple lines
        if(lineBuffer != null) {
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
        if(terminate == RUNNING) {
            clear(Scope.CONTEXT);
            clear(Scope.REQUEST);
            terminate = TERMINATING;
            disconnectController();
            restoreStdIO();
            console.stop();
            if (shutdownHook != null) {
                CliShutdownHook.remove(shutdownHook);
            }
            terminate = TERMINATED;
        }
    }

    @Override
    @Deprecated
    public void printLine(String message) {
        final Level logLevel;
        if(exitCode != 0) {
            logLevel = Level.ERROR;
        } else {
            logLevel = Level.INFO;
        }
        if(log.isEnabled(logLevel)) {
            log.log(logLevel, message);
        }

        if (outputTarget != null) {
            try {
                outputTarget.append(message);
                outputTarget.newLine();
                outputTarget.flush();
            } catch (IOException e) {
                System.err.println("Failed to print '" + message + "' to the output target: " + e.getLocalizedMessage());
            }
            return;
        }

        console.print(message);
        console.printNewLine();
    }

    /**
     * Set the exit code of the process to indicate an error and output the error message.
     *
     * WARNING This method should only be called for unrecoverable errors as once the exit code is set subsequent operations may
     * not be possible.
     *
     * @param message The message to display.
     */
    protected void error(String message) {
        this.exitCode = 1;
        printLine(message);
    }

    @Deprecated
    private String readLine(String prompt, boolean password) throws CommandLineException {
        if (!console.running()) {
            console.start();
        }
        try {
            if (password) {
                return console.promptForInput(prompt, (char) 0x00);

            } else {
                return console.promptForInput(prompt);
            }
        } catch (IOException | InterruptedException ex) {
            throw new CommandLineException(ex);
        }
    }


    @Override
    @Deprecated
    public void printColumns(Collection<String> col) {
        if(log.isInfoEnabled()) {
            log.info(col);
        }
        if (outputTarget != null) {
            try {
                for (String item : col) {
                    outputTarget.append(item);
                    outputTarget.newLine();
                }
            } catch (IOException e) {
                System.err.println("Failed to print columns '" + col + "' to the console: " + e.getLocalizedMessage());
            }
            return;
        }

        console.printColumns(col);
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
        return DefaultOperationRequestParser.INSTANCE;
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
        return operationCandidatesProvider;
    }

    @Override
    public void connectController() throws CommandLineException {
        connectController(null);
    }

    public Console getConsole() {
        return console;
    }

    @Override
    @Deprecated
    public void connectController(String controller) throws CommandLineException {
        connectController(controller, getConsole());
    }

    public void connectController(String controller, Console cons) throws CommandLineException {

        ControllerAddress address = addressResolver.resolveAddress(controller);

        // In case the alias mappings cause us to enter some form of loop or a badly
        // configured server does the same,
        Set<ControllerAddress> visited = new HashSet<ControllerAddress>();
        visited.add(address);
        boolean retry = false;
        do {
            try {
                CallbackHandler cbh = new AuthenticationCallbackHandler(username, password, cons);
                if (log.isDebugEnabled()) {
                    log.debug("connecting to " + address.getHost() + ':' + address.getPort() + " as " + username);
                }
                ModelControllerClient tempClient = ModelControllerClientFactory.CUSTOM.getClient(address, cbh,
                        disableLocalAuth, sslContext.getSslContext(),
                        config.getConnectionTimeout(),
                        this,
                        timeoutHandler,
                        clientBindAddress);
                retry = false;
                connInfoBean = new ConnectionInfoBean();
                tryConnection(tempClient, address);
                initNewClient(tempClient, address, connInfoBean);
                connInfoBean.setDisableLocalAuth(disableLocalAuth);
                connInfoBean.setLoggedSince(new Date());
            } catch (RedirectException re) {
                try {
                    URI location = new URI(re.getLocation());
                    if ("http-remoting".equals(address.getProtocol()) && "https".equals(location.getScheme())) {
                        int port = location.getPort();
                        if (port < 0) {
                            port = 443;
                        }
                        address = addressResolver.resolveAddress(new URI("https-remoting", null, location.getHost(), port,
                                null, null, null).toString());
                        if (visited.add(address) == false) {
                            throw new CommandLineException("Redirect to address already tried encountered Address="
                                    + address.toString());
                        }
                        retry = true;
                    } else if (address.getHost().equals(location.getHost()) && address.getPort() == location.getPort()
                            && location.getPath() != null && location.getPath().length() > 1) {
                        throw new CommandLineException("Server at " + address.getHost() + ":" + address.getPort()
                                + " does not support " + address.getProtocol());
                    } else {
                        throw new CommandLineException("Unsupported redirect received.", re);
                    }
                } catch (URISyntaxException e) {
                    throw new CommandLineException("Bad redirect location '" + re.getLocation() + "' received.", e);
                }
            } catch (IOException e) {
                throw new CommandLineException("Failed to resolve host '" + address.getHost() + "'", e);
            }
        } while (retry);
    }

    @Override
    @Deprecated
    public void connectController(String host, int port) throws CommandLineException {
        try {
            connectController(new URI(null, null, host, port, null, null, null).toString().substring(2));
        } catch (URISyntaxException e) {
            throw new CommandLineException("Unable to construct URI for connection.", e);
        }
    }

    @Override
    public void bindClient(ModelControllerClient newClient) {
        initNewClient(newClient, null, null);
    }

    private void initNewClient(ModelControllerClient newClient, ControllerAddress address, ConnectionInfoBean conInfo) {
        if (newClient != null) {
            if (this.client != null) {
                disconnectController();
            }

            client = newClient;
            this.currentAddress = address;
            this.connInfoBean = conInfo;

            List<String> nodeTypes = Util.getNodeTypes(newClient, new DefaultOperationRequestAddress());
            domainMode = nodeTypes.contains(Util.SERVER_GROUP);

            try {
                extLoader.loadHandlers(currentAddress);
            } catch (CommandLineException e) {
                printLine(Util.getMessagesFromThrowable(e));
            }
        }
    }

    @Override
    public File getCurrentDir() {
        return currentDir;
    }

    @Override
    public void setCurrentDir(File dir) {
        if(dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        this.currentDir = dir;
    }

    @Override
    public void registerRedirection(CommandLineRedirection redirection) throws CommandLineException {
        if(this.redirection != null) {
            throw new CommandLineException("Another redirection is currently active.");
        }
        this.redirection = new CommandLineRedirectionRegistration(redirection);
        redirection.set(this.redirection);
    }

    /**
     * Used to make a call to the server to verify that it is possible to connect.
     */
    private void tryConnection(final ModelControllerClient client, ControllerAddress address) throws CommandLineException, RedirectException {
        try {
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            builder.setOperationName(Util.READ_ATTRIBUTE);
            builder.addProperty(Util.NAME, Util.NAME);

            final long start = System.currentTimeMillis();
            final long timeoutMillis = config.getConnectionTimeout() + 1000;
            boolean tryConnection = true;
            while (tryConnection) {
                final ModelNode response = client.execute(builder.buildRequest());
                if (!Util.isSuccess(response)) {
                    // here we check whether the error is related to the access control permissions
                    // WFLYCTL0332: Permission denied
                    // WFLYCTL0313: Unauthorized to execute operation
                    final String failure = Util.getFailureDescription(response);
                    if (failure.contains("WFLYCTL0332")) {
                        StreamUtils.safeClose(client);
                        throw new CommandLineException(
                                "Connection refused based on the insufficient user permissions."
                                        + " Please, make sure the security-realm attribute is specified for the relevant management interface"
                                        + " (standalone.xml/host.xml) and review the access-control configuration (standalone.xml/domain.xml).");
                    } else if (failure.contains("WFLYCTL0379")) { // system boot is in process
                        if (System.currentTimeMillis() - start > timeoutMillis) {
                            throw new CommandLineException("Timeout waiting for the system to boot.");
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            disconnectController();
                            throw new CommandLineException("Interrupted while pausing before trying connection.", e);
                        }
                    } else {
                        // Otherwise, on one hand, we don't actually care what the response is,
                        // we just want to be sure the ModelControllerClient does not throw an Exception.
                        // On the other hand, reading name attribute is a very basic one which should always work
                        printLine("Warning! The connection check resulted in failure: " + Util.getFailureDescription(response));
                        tryConnection = false;
                    }
                } else {
                    tryConnection = false;
                }
            }
        } catch (Exception e) {
            try {
                Throwable current = e;
                while (current != null) {
                    if (current instanceof SaslException) {
                        throw new CommandLineException("Unable to authenticate against controller at " + address.getHost() + ":" + address.getPort(), current);
                    }
                    if (current instanceof SSLException) {
                        throw new CommandLineException("Unable to negotiate SSL connection with controller at "+ address.getHost() + ":" + address.getPort());
                    }
                    if (current instanceof RedirectException) {
                        throw (RedirectException) current;
                    }
                    if (current instanceof CommandLineException) {
                        throw (CommandLineException) current;
                    }
                    current = current.getCause();
                }

                // We don't know what happened, most likely a timeout.
                throw new CommandLineException("The controller is not available at " + address.getHost() + ":" + address.getPort(), e);
            } finally {
                StreamUtils.safeClose(client);
            }
        }
    }

    @Override
    public void disconnectController() {
        if (this.client != null) {
            StreamUtils.safeClose(client);
            // if(loggingEnabled) {
            // printLine("Closed connection to " + this.controllerHost + ':' +
            // this.controllerPort);
            // }
            client = null;
            this.currentAddress = null;
            domainMode = false;
            notifyListeners(CliEvent.DISCONNECTED);
            connInfoBean = null;
            extLoader.resetHandlers();
        }
        promptConnectPart = null;
        if(console != null && terminate == RUNNING) {
            console.setPrompt(getPrompt());
        }
    }

    @Override
    @Deprecated
    public String getDefaultControllerHost() {
        return config.getDefaultControllerHost();
    }

    @Override
    @Deprecated
    public int getDefaultControllerPort() {
        return config.getDefaultControllerPort();
    }

    @Override
    public ControllerAddress getDefaultControllerAddress() {
        return config.getDefaultControllerAddress();
    }

    @Override
    public String getControllerHost() {
        return currentAddress != null ? currentAddress.getHost() : null;
    }

    @Override
    public int getControllerPort() {
        return currentAddress != null ? currentAddress.getPort() : -1;
    }

    @Override
    public void clearScreen() {
        if(console != null) {
            console.clearScreen();
        }
    }

    String promptConnectPart;

    @Override
    public String getPrompt() {
        if(lineBuffer != null) {
            return "> ";
        }
        StringBuilder buffer = new StringBuilder();
        if (promptConnectPart == null) {
            buffer.append('[');
            String controllerHost = getControllerHost();
            if (client != null) {
                if (domainMode) {
                    buffer.append("domain@");
                } else {
                    buffer.append("standalone@");
                }
                if (controllerHost != null) {
                    buffer.append(controllerHost).append(':').append(getControllerPort()).append(' ');
                } else {
                    buffer.append("embedded ");
                }
                promptConnectPart = buffer.toString();
            } else {
                buffer.append("disconnected ");
            }
        } else {
            buffer.append(promptConnectPart);
        }

        if (prefix.isEmpty()) {
            buffer.append('/');
        } else {
            buffer.append(prefix.getNodeType());
            final String nodeName = prefix.getNodeName();
            if (nodeName != null) {
                buffer.append('=').append(nodeName);
            }
        }

        if (isBatchMode()) {
            buffer.append(" #");
        }
        buffer.append("] ");
        return buffer.toString();
    }

    @Override
    @Deprecated
    public CommandHistory getHistory() {
        return console.getHistory();
    }

    private void resetArgs(String cmdLine) throws CommandFormatException {
        if (cmdLine != null) {
            parsedCmd.parse(prefix, cmdLine, this);
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
        return new DefaultBatchedCommand(this, line, buildRequest(line, true));
    }

    @Override
    public ModelNode buildRequest(String line) throws CommandFormatException {
        return buildRequest(line, false);
    }

    protected ModelNode buildRequest(String line, boolean batchMode) throws CommandFormatException {

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
                return request;
            }

            final CommandHandler handler = cmdRegistry.getCommandHandler(parsedCmd.getOperationName());
            if (handler == null) {
                throw new OperationFormatException("No command handler for '" + parsedCmd.getOperationName() + "'.");
            }
            if(batchMode) {
                if(!handler.isBatchMode(this)) {
                    throw new OperationFormatException("The command is not allowed in a batch.");
                }
            } else if (!(handler instanceof OperationCommand)) {
                throw new OperationFormatException("The command does not translate to an operation request.");
            }

            return ((OperationCommand) handler).buildRequest(this);
        } finally {
            clear(Scope.REQUEST);
            this.parsedCmd = originalParsedArguments;
            this.cmdLine = originalCmdLine;
        }
    }

    @Override
    public CommandLineCompleter getDefaultCommandCompleter() {
        return cmdCompleter;
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

    protected void setOutputTarget(String filePath) {
        if (filePath == null) {
            this.outputTarget = null;
            return;
        }
        FileWriter writer;
        try {
            writer = new FileWriter(filePath, false);
        } catch (IOException e) {
            error(e.getLocalizedMessage());
            return;
        }
        this.outputTarget = new BufferedWriter(writer);
    }

    protected void notifyListeners(CliEvent event) {
        for (CliEventListener listener : listeners) {
            listener.cliEvent(event, this);
        }
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
        // if the connection loss was triggered by an instruction to restart/reload
        // then we don't disconnect yet
        if(parsedCmd.getFormat() != null) {
            if(Util.RELOAD.equals(parsedCmd.getOperationName())) {
                // do nothing
            } else if(Util.SHUTDOWN.equals(parsedCmd.getOperationName())) {
                if(CommandFormat.INSTANCE.equals(parsedCmd.getFormat())
                        // shutdown command handler decides whether to disconnect or not
                        || Util.TRUE.equals(parsedCmd.getPropertyValue(Util.RESTART))) {
                    // do nothing
                } else {
                    printLine("");
                    printLine("The controller has closed the connection.");
                    disconnectController();
                }
            } else {
                // we don't disconnect here because the connection may be closed by another
                // CLI instance/session doing a reload (this happens in our testsuite)
            }
        } else {
            // we don't disconnect here because the connection may be closed by another
            // CLI instance/session doing a reload (this happens in our testsuite)
        }
    }

    @Override
    @Deprecated
    public boolean isSilent() {
        return console.isSilent();
    }

    @Override
    @Deprecated
    public void setSilent(boolean silent) {
        console.setSilent(silent);
    }

    @Override
    @Deprecated
    public int getTerminalWidth() {
        return console.getTerminalWidth();
    }

    @Override
    @Deprecated
    public int getTerminalHeight() {
        return console.getTerminalHeight();
    }

    @Override
    public void setVariable(String name, String value) throws CommandLineException {
        if(name == null || name.isEmpty()) {
            throw new CommandLineException("Variable name can't be null or an empty string");
        }
        if(!Character.isJavaIdentifierStart(name.charAt(0))) {
            throw new CommandLineException("Variable name must be a valid Java identifier (and not contain '$'): '" + name + "'");
        }
        for(int i = 1; i < name.length(); ++i) {
            final char c = name.charAt(i);
            if(!Character.isJavaIdentifierPart(c) || c == '$') {
                throw new CommandLineException("Variable name must be a valid Java identifier (and not contain '$'): '" + name + "'");
            }
        }

        if(value == null) {
            if(variables == null) {
                return;
            }
            variables.remove(name);
        } else {
            if(variables == null) {
                variables = new HashMap<String,String>();
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

    @Override
    @Deprecated
    public void interact() {
        try {
            console.interact(true);
        } catch (CliInitializationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class JaasConfigurationWrapper extends Configuration {

        private final Configuration wrapped;

        private JaasConfigurationWrapper(Configuration toWrap) {
            this.wrapped = toWrap;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            AppConfigurationEntry[] response = wrapped != null ? wrapped.getAppConfigurationEntry(name) : null;
            if (response == null) {
                if ("com.sun.security.jgss.initiate".equals(name)) {
                    HashMap<String, String> options = new HashMap<String, String>(2);
                    options.put("useTicketCache", "true");
                    options.put("doNotPrompt", "true");
                    response = new AppConfigurationEntry[] { new AppConfigurationEntry(
                            "com.sun.security.auth.module.Krb5LoginModule",
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options) };
                }

            }

            return response;
        }

    }

    class CommandLineRedirectionRegistration implements CommandLineRedirection.Registration {

        CommandLineRedirection target;

        CommandLineRedirectionRegistration(CommandLineRedirection redirection) {
            if(redirection == null) {
                throw new IllegalArgumentException("Redirection is null");
            }
            this.target = redirection;
        }

        @Override
        public void unregister() throws CommandLineException {
            ensureActive();
            CommandContextImpl.this.redirection = null;
        }

        @Override
        public boolean isActive() {
            return CommandContextImpl.this.redirection == this;
        }

        @Override
        public void handle(ParsedCommandLine parsedLine) throws CommandLineException {

            ensureActive();

            final String line = parsedLine.getSubstitutedLine();
            try {
                if (parsedLine.getFormat() == OperationFormat.INSTANCE) {
                    final ModelNode request = Util.toOperationRequest(CommandContextImpl.this, parsedLine);
                    if (isBatchMode()) {
                        StringBuilder op = new StringBuilder();
                        op.append(getNodePathFormatter().format(parsedCmd.getAddress()));
                        op.append(line.substring(line.indexOf(':')));
                        DefaultBatchedCommand batchedCmd
                                = new DefaultBatchedCommand(CommandContextImpl.this, op.toString(), request);
                        Batch batch = getBatchManager().getActiveBatch();
                        batch.add(batchedCmd);
                    } else {
                        set(Scope.REQUEST, "OP_REQ", request);
                        operationHandler.handle(CommandContextImpl.this);
                    }
                } else {
                    final String cmdName = parsedCmd.getOperationName();
                    CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
                    if (handler != null) {
                        if (isBatchMode() && handler.isBatchMode(CommandContextImpl.this)) {
                            if (!(handler instanceof OperationCommand)) {
                                throw new CommandLineException("The command is not allowed in a batch.");
                            } else {
                                try {
                                    ModelNode request = ((OperationCommand) handler).buildRequest(CommandContextImpl.this);
                                    BatchedCommand batchedCmd
                                            = new DefaultBatchedCommand(CommandContextImpl.this, line, request);
                                    Batch batch = getBatchManager().getActiveBatch();
                                    batch.add(batchedCmd);
                                } catch (CommandFormatException e) {
                                    throw new CommandFormatException("Failed to add to batch '" + line + "'", e);
                                }
                            }
                        } else {
                            handler.handle(CommandContextImpl.this);
                        }
                    } else {
                        throw new CommandLineException("Unexpected command '" + line + "'. Type 'help --commands' for the list of supported commands.");
                    }
                }
            } finally {
                clear(Scope.REQUEST);
            }
        }

        private void ensureActive() throws CommandLineException {
            if(!isActive()) {
                throw new CommandLineException("The redirection is not registered any more.");
            }
        }
    }

    public ConnectionInfo getConnectionInfo() {
        return connInfoBean;
    }

    @Override
    @Deprecated
    public void captureOutput(PrintStream captor) {
        assert captor != null;
        console.captureOutput(captor);
}

    @Override
    @Deprecated
    public void releaseOutput() {
        console.releaseOutput();
    }

    public void setParsedCommandLine(DefaultCallbackHandler line) {
        parsedCmd = line;
    }

    public void addBatchOperation(ModelNode request, String line) {
        BatchedCommand batchedCmd
                = new DefaultBatchedCommand(this, line, request);
        Batch batch = getBatchManager().getActiveBatch();
        batch.add(batchedCmd);
    }

    public void handleOperation(ParsedCommandLine line) throws CommandLineException {
        if (line.getFormat() == OperationFormat.INSTANCE) {
            final ModelNode request = Util.toOperationRequest(this, line);

            if (isBatchMode()) {
                String str = line.getOriginalLine();
                StringBuilder op = new StringBuilder();
                op.append(getNodePathFormatter().format(line.getAddress()));
                op.append(str.substring(str.indexOf(':')));
                DefaultBatchedCommand batchedCmd
                        = new DefaultBatchedCommand(this, op.toString(), request);
                Batch batch = getBatchManager().getActiveBatch();
                batch.add(batchedCmd);
            } else {
                try {
                    set(Scope.REQUEST, "OP_REQ", request);
                    operationHandler.handle(this);
                } finally {
                    clear(Scope.REQUEST);
                }
            }
        } else {
            throw new CommandLineException("Not an operation");
        }
    }
}
