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
package org.jboss.as.cli.command.embedded;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CliEvent;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.aesh.converter.FileConverter;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.embedded.Contexts;
import org.jboss.as.cli.embedded.EmbeddedLogContext;
import org.jboss.as.cli.embedded.EmbeddedProcessLaunch;
import org.jboss.as.cli.embedded.EnvironmentRestorer;
import org.jboss.as.cli.embedded.ThreadContextsModelControllerClient;
import org.jboss.as.cli.embedded.ThreadLocalContextSelector;
import org.jboss.as.cli.embedded.UncloseablePrintStream;
import org.jboss.as.cli.handlers.SimpleTabCompleter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.modules.ModuleLoader;
import org.jboss.stdio.NullOutputStream;
import org.jboss.stdio.StdioContext;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.activator.CliOptionActivator;
import org.wildfly.core.embedded.EmbeddedManagedProcess;
import org.wildfly.core.embedded.EmbeddedProcessFactory;
import org.wildfly.core.embedded.EmbeddedProcessStartException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "host-controller", description = "", activator = EmbedServerActivator.class)
public class EmbedHostControllerCommand implements Command<CliCommandInvocation> {

    private static final class JBossHomeActivator implements CliOptionActivator {

        private CliCommandContext commandContext;

        @Override
        public void setCommandContext(CliCommandContext commandContext) {
            this.commandContext = commandContext;
        }

        @Override
        public CliCommandContext getCommandContext() {
            return commandContext;
        }

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            EmbedHostControllerCommand cmd = (EmbedHostControllerCommand) processedCommand.getCommand();
            return !cmd.isModularExecution();
        }
    }

    @Deprecated
    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    private static final String ECHO = "echo";
    private static final String DISCARD_STDOUT = "discard";
    private static final String DOMAIN_CONFIG = "--domain-config";
    private static final String HOST_CONFIG = "--host-config";

    @Option(name = "domain-config", completer = FileCompleter.class,
            converter = FileConverter.class, shortName = 'c', required = false)
    private File domainConfig;

    @Option(name = "jboss-home", activator = JBossHomeActivator.class, required = false)
    private File jbossHome;

    @Option(name = "std-out", completer = StdOutCompleter.class, required = false)
    private String stdOutHandling;

    @Option(name = "host-config", required = false)
    private String hostConfig;

    @Option(required = false)
    private Long timeout;

    private final AtomicReference<EmbeddedProcessLaunch> hostControllerReference;
    private final boolean modular;

    public EmbedHostControllerCommand(AtomicReference<EmbeddedProcessLaunch> hostControllerReference, boolean modular) {
        this.hostControllerReference = hostControllerReference;
        this.modular = modular;
    }

    boolean isModularExecution() {
        return modular;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("embed host-controller"));
            return CommandResult.SUCCESS;
        }
        if (isModularExecution() && jbossHome != null) {
            throw new CommandException("jboss-home can only be used with non modular CLI execution");
        }
        final File jbossHome = getJBossHome();

        Long bootTimeout = timeout == null ? null : TimeUnit.SECONDS.toNanos(timeout);

        if (stdOutHandling != null) {
            if (!(stdOutHandling.equals(ECHO) || stdOutHandling.equals(DISCARD_STDOUT))) {
                throw new CommandException("The --std-out parameter should be one of { echo, discard }.");
            }
        }
        final EnvironmentRestorer restorer = new EnvironmentRestorer();
        boolean ok = false;
        ThreadLocalContextSelector contextSelector = null;
        try {

            Contexts defaultContexts = restorer.getDefaultContexts();

            StdioContext discardStdoutContext = null;
            if (!ECHO.equalsIgnoreCase(stdOutHandling)) {
                PrintStream nullStream = new UncloseablePrintStream(NullOutputStream.getInstance());
                StdioContext currentContext = defaultContexts.getStdioContext();
                discardStdoutContext = StdioContext.create(currentContext.getIn(), nullStream, currentContext.getErr());
            }

            // Configure and get the log context
            final LogContext embeddedLogContext = EmbeddedLogContext.configureLogContext(new File(jbossHome, "domain"), "host-controller.log", ctx);

            Contexts localContexts = new Contexts(embeddedLogContext, discardStdoutContext);
            contextSelector = new ThreadLocalContextSelector(localContexts, defaultContexts);
            contextSelector.pushLocal();

            StdioContext.setStdioContextSelector(contextSelector);
            LogContext.setLogContextSelector(contextSelector);

            List<String> cmdsList = new ArrayList<>();
            if (domainConfig != null && domainConfig.getAbsolutePath().trim().length() > 0) {
                cmdsList.add(DOMAIN_CONFIG);
                cmdsList.add(domainConfig.getAbsolutePath().trim());
            }

            if (hostConfig != null && hostConfig.trim().length() > 0) {
                cmdsList.add(HOST_CONFIG);
                cmdsList.add(hostConfig.trim());
            }

            String[] cmds = cmdsList.toArray(new String[cmdsList.size()]);

            EmbeddedManagedProcess hostController;
            if (this.jbossHome == null) {
                // Modular environment
                hostController = EmbeddedProcessFactory.createHostController(ModuleLoader.forClass(getClass()), jbossHome, cmds);
            } else {
                hostController = EmbeddedProcessFactory.createHostController(jbossHome.getAbsolutePath(), null, null, cmds);
            }
            hostController.start();
            hostControllerReference.set(new EmbeddedProcessLaunch(hostController, restorer, true));

            ModelControllerClient mcc = new ThreadContextsModelControllerClient(hostController.getModelControllerClient(), contextSelector);
            if (bootTimeout == null || bootTimeout > 0) {
                long expired = bootTimeout == null ? Long.MAX_VALUE : System.nanoTime() + bootTimeout;

                String localName = "master";
                String status = "starting";

                // read out the host controller name
                final ModelNode getNameOp = new ModelNode();
                getNameOp.get(ClientConstants.OP).set(ClientConstants.READ_ATTRIBUTE_OPERATION);
                getNameOp.get(ClientConstants.NAME).set(Util.LOCAL_HOST_NAME);

                final ModelNode getStateOp = new ModelNode();
                getStateOp.get(ClientConstants.OP).set(ClientConstants.READ_ATTRIBUTE_OPERATION);
                ModelNode address = getStateOp.get(ClientConstants.ADDRESS);
                address.add(ClientConstants.HOST, localName);
                getStateOp.get(ClientConstants.NAME).set(ClientConstants.HOST_STATE);
                do {
                    try {
                        final ModelNode nameResponse = mcc.execute(getNameOp);
                        if (Util.isSuccess(nameResponse)) {
                            localName = nameResponse.get(ClientConstants.RESULT).asString();
                            address.set(ClientConstants.HOST, localName);
                            final ModelNode stateResponse = mcc.execute(getStateOp);
                            if (Util.isSuccess(stateResponse)) {
                                status = stateResponse.get(ClientConstants.RESULT).asString();
                            }
                        }
                    } catch (Exception e) {
                        // ignore and try again
                    }

                    if ("starting".equals(status)) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CommandException("Interrupted while waiting for embedded server to start");
                        }
                    } else {
                        break;
                    }
                } while (System.nanoTime() < expired);

                if ("starting".equals(status)) {
                    assert bootTimeout != null; // we'll assume the loop didn't run for decades
                    // Stop server and restore environment
                    StopEmbeddedServerCommand.cleanup(hostControllerReference);
                    throw new CommandException("Embedded host controller did not exit 'starting' status within "
                            + TimeUnit.NANOSECONDS.toSeconds(bootTimeout) + " seconds");
                }
            }
            // Expose the client to the rest of the CLI last so nothing can be done with
            // it until we're ready
            ctx.bindClient(mcc);
            // Stop the server on any disconnect event
            ctx.addEventListener(new CliEventListener() {
                @Override
                public void cliEvent(CliEvent event, CommandContext ctx) {
                    if (event == CliEvent.DISCONNECTED) {
                        StopEmbeddedServerCommand.cleanup(hostControllerReference);
                    }
                }
            });
            ok = true;
        } catch (RuntimeException | EmbeddedProcessStartException e) {
            throw new CommandException("Cannot start embedded Host Controller", e);
        } finally {
            if (!ok) {
                ctx.disconnectController();
                restorer.restoreEnvironment();
            } else if (contextSelector != null) {
                contextSelector.restore(null);
            }
        }
        return CommandResult.SUCCESS;
    }

    private File getJBossHome() throws CommandException {
        String jbossHome = this.jbossHome == null ? null : this.jbossHome.getAbsolutePath();
        if (jbossHome == null || jbossHome.length() == 0) {
            jbossHome = WildFlySecurityManager.getEnvPropertyPrivileged("JBOSS_HOME", null);
            if (jbossHome == null || jbossHome.length() == 0) {
                if (this.jbossHome != null) {
                    throw new CommandException("Missing configuration value for --jboss-home and environment variable JBOSS_HOME is not set");
                } else {
                    throw new CommandException("Environment variable JBOSS_HOME is not set");
                }
            }
            return validateJBossHome(jbossHome, "environment variable JBOSS_HOME");
        } else {
            return validateJBossHome(jbossHome, "argument --jboss-home");
        }
    }

    private static File validateJBossHome(String jbossHome, String source) throws CommandException {
        File f = new File(jbossHome);
        if (!f.exists()) {
            throw new CommandException(String.format("File %s specified by %s does not exist", jbossHome, source));
        } else if (!f.isDirectory()) {
            throw new CommandException(String.format("File %s specified by %s is not a directory", jbossHome, source));
        }
        return f;
    }

    public static final class StdOutCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation cliCompleterInvocation) {
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (cliCompleterInvocation.getGivenCompleteValue() != null) {
                pos = cliCompleterInvocation.getGivenCompleteValue().length();
            }
            SimpleTabCompleter completer = new SimpleTabCompleter(new String[]{ECHO, DISCARD_STDOUT});
            int cursor = completer.complete(cliCompleterInvocation.getCommandContext().getLegacyCommandContext(),
                    cliCompleterInvocation.getGivenCompleteValue(), pos, candidates);
            cliCompleterInvocation.addAllCompleterValues(candidates);
            cliCompleterInvocation.setOffset(cliCompleterInvocation.getGivenCompleteValue().length() - cursor);
        }
    }
}
