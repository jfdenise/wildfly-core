/*
Copyright 2017 Red Hat, Inc.

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
package org.jboss.as.cli.impl.aesh.cmd.security.auth;

import org.jboss.as.cli.impl.aesh.cmd.security.model.AuthFactorySpec;
import java.io.IOException;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand;
import org.jboss.as.cli.impl.aesh.cmd.security.model.FileSystemRealmConfiguration;
import org.jboss.as.cli.impl.aesh.cmd.security.model.LocalUserConfiguration;
import org.jboss.as.cli.impl.aesh.cmd.security.model.MechanismConfiguration;
import org.jboss.as.cli.impl.aesh.cmd.security.model.PropertiesRealmConfiguration;
import org.jboss.as.cli.impl.aesh.cmd.security.model.AuthSecurityBuilder;
import org.jboss.as.cli.impl.aesh.cmd.RelativeFile;
import org.jboss.as.cli.impl.aesh.cmd.RelativeFilePathConverter;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_EXPOSED_REALM_NAME;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_EXT_TRUST_STORE;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_FILE_SYSTEM_REALM;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_GROUP_PROPERTIES_FILE;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_MECHANISM;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_NEW_AUTH_FACTORY_NAME;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_NEW_SECURITY_DOMAIN_NAME;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_NO_RELOAD;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_RELATIVE_TO;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_SUPER_USER;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_USER_PROPERTIES_FILE;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_USER_ROLE_DECODER;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.formatOption;
import org.jboss.as.cli.impl.aesh.cmd.security.model.AuthFactory;
import org.jboss.as.cli.impl.aesh.cmd.security.model.AuthMechanism;
import org.jboss.as.cli.impl.aesh.cmd.security.model.ElytronUtil;
import org.jboss.as.cli.impl.aesh.cmd.security.model.EmptyConfiguration;
import org.jboss.as.cli.impl.aesh.cmd.security.model.TrustStoreConfiguration;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_NEW_SECURITY_REALM_NAME;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "abstract-auth-enable", description = "")
public abstract class AbstractEnableAuthenticationCommand implements Command<CLICommandInvocation>, DMRCommand {

    @Option(name = OPT_MECHANISM,
            completer = SecurityCommand.OptionCompleters.MechanismCompleter.class)
    String mechanism;

    @Option(name = OPT_FILE_SYSTEM_REALM, activator = OptionActivators.FilesystemRealmActivator.class,
            completer = SecurityCommand.OptionCompleters.FileSystemRealmCompleter.class)
    String fileSystemRealm;

    @Option(name = OPT_USER_ROLE_DECODER, activator = OptionActivators.FileSystemRoleDecoderActivator.class,
            completer = SecurityCommand.OptionCompleters.SimpleDecoderCompleter.class)
    String userRoleDecoder;

    @Option(name = OPT_USER_PROPERTIES_FILE, activator = OptionActivators.PropertiesFileRealmActivator.class,
            converter = RelativeFilePathConverter.class, completer = FileOptionCompleter.class)
    RelativeFile userPropertiesFile;

    @Option(name = OPT_GROUP_PROPERTIES_FILE, activator = OptionActivators.GroupPropertiesFileActivator.class,
            converter = RelativeFilePathConverter.class, completer = FileOptionCompleter.class)
    RelativeFile groupPropertiesFile;

    @Option(name = OPT_EXPOSED_REALM_NAME, activator = OptionActivators.MechanismWithRealmActivator.class)
    String exposedRealmName;

    @Option(name = OPT_RELATIVE_TO, activator = OptionActivators.RelativeToActivator.class)
    String relativeTo;

    @Option(name = OPT_NO_RELOAD, hasValue = false, activator = OptionActivators.DependsOnMechanism.class)
    boolean noReload;

    @Option(name = OPT_SUPER_USER, hasValue = false, activator = OptionActivators.SuperUserActivator.class)
    boolean superUser;

    @Option(name = OPT_NEW_SECURITY_DOMAIN_NAME, activator = OptionActivators.DependsOnMechanism.class)
    String newSecurityDomain;

    @Option(name = OPT_NEW_AUTH_FACTORY_NAME, activator = OptionActivators.DependsOnMechanism.class)
    String newAuthFactoryName;

    @Option(name = OPT_NEW_SECURITY_REALM_NAME, activator = OptionActivators.MechanismWithRealmActivator.class)
    String newRealmName;

    @Option(name = OPT_EXT_TRUST_STORE, activator = OptionActivators.MechanismWithTrustStore.class,
            completer = SecurityCommand.OptionCompleters.KeyStoreNameCompleter.class)
    String externalTrustStore;

    private final AuthFactorySpec factorySpec;
    protected AbstractEnableAuthenticationCommand(AuthFactorySpec factorySpec) {
        this.factorySpec = factorySpec;
    }

    public AuthFactorySpec getFactorySpec() {
        return factorySpec;
    }
    protected abstract void secure(CommandContext ctx, AuthSecurityBuilder builder) throws Exception;

    protected abstract String getOOTBFactory(CommandContext ctx) throws Exception;

    protected abstract String getSecuredEndpoint(CommandContext ctx);

    protected abstract String getEnabledFactory(CommandContext ctx) throws Exception;

    public String getTargetedFactory(CommandContext ctx) throws Exception {
        String factory = getEnabledFactory(ctx);
        if (factory == null) {
            factory = getOOTBFactory(ctx);
        }
        return factory;
    }

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext();
        AuthSecurityBuilder builder;
        try {
            builder = buildSecurityRequest(ctx);
        } catch (Exception ex) {
            throw new CommandException(ex.getLocalizedMessage());
        }
        if (!builder.isEmpty()) {
            SecurityCommand.execute(ctx, builder.getRequest(), !shouldReload());
            commandInvocation.getCommandContext().printLine("Command success.");
            commandInvocation.getCommandContext().printLine("Authentication configured for "
                    + getSecuredEndpoint(commandInvocation.getCommandContext()));
            commandInvocation.getCommandContext().printLine(factorySpec.getName()
                    + " authentication-factory=" + builder.getAuthFactory().getName());
            commandInvocation.getCommandContext().printLine("security-domain="
                    + builder.getAuthFactory().getSecurityDomain().getName());
        } else {
            commandInvocation.getCommandContext().
                    printLine("Authentication is already enabled for " + getSecuredEndpoint(commandInvocation.getCommandContext()));
        }
        return CommandResult.SUCCESS;
    }

    @Override
    public ModelNode buildRequest(CommandContext context) throws CommandFormatException {
        try {
            return buildSecurityRequest(context).getRequest();
        } catch (Exception ex) {
            throw new CommandFormatException(ex.getLocalizedMessage() == null
                    ? ex.toString() : ex.getLocalizedMessage());
        }
    }

    private AuthSecurityBuilder buildSecurityRequest(CommandContext context) throws Exception {
        AuthSecurityBuilder builder = buildSecurityBuilder(context);
        //OOTB
        if (builder == null) {
            String factoryName = getOOTBFactory(context);
            AuthFactory factory = ElytronUtil.getAuthFactory(factoryName, getFactorySpec(), context);
            if (factory == null) {
                throw new Exception("Can't enable " + factorySpec.getName() + " authentication, "
                        + factoryName + " doesn't exist");
            }
            builder = new AuthSecurityBuilder(factory);
        }
        builder.buildRequest(context);
        if (!builder.isFactoryAlreadySet()) {
            secure(context, builder);
        }
        return builder;
    }

    private AuthSecurityBuilder buildSecurityBuilder(CommandContext context) throws Exception {
        AuthMechanism mec = buildAuthMechanism(context);
        if (mec != null) {
            return buildSecurityBuilder(context, mec);
        }
        return null;
    }

    private AuthSecurityBuilder buildSecurityBuilder(CommandContext ctx, AuthMechanism mec) throws Exception {
        String existingFactory = getEnabledFactory(ctx);
        AuthSecurityBuilder builder = new AuthSecurityBuilder(mec, getFactorySpec());
        builder.setActiveFactoryName(existingFactory);
        configureBuilder(builder);
        return builder;
    }

    protected MechanismConfiguration buildLocalUserConfiguration(CommandContext ctx,
            boolean superUser) throws CommandException, IOException, OperationFormatException {
        if (!ElytronUtil.localUserExists(ctx)) {
            throw new CommandException("Can't configure 'local' user, no such identity.");
        }
        return new LocalUserConfiguration(superUser);
    }

    public static void throwInvalidOptions() throws CommandException {
        throw new CommandException("You must only set a single mechanism.");
    }

    protected static MechanismConfiguration buildExternalConfiguration(CommandContext ctx, String externalTrustStore) throws CommandException, IOException, OperationFormatException {
        if (!ElytronUtil.keyStoreExists(ctx, externalTrustStore)) {
            throw new CommandException("Can't configure 'certificate' authentication, no trustore " + externalTrustStore);
        }
        return new TrustStoreConfiguration(externalTrustStore);
    }

    protected static MechanismConfiguration buildUserPasswordConfiguration(RelativeFile userPropertiesFile,
            String fileSystemRealm, String userRoleDecoder, String exposedRealmName, RelativeFile groupPropertiesFile, String relativeTo) throws CommandException, IOException {
        if (userPropertiesFile == null && fileSystemRealm == null) {
            throw new CommandException("A properties file or a filesystem-realm name must be provided");
        }

        if (userPropertiesFile != null && fileSystemRealm != null) {
            throw new CommandException("A properties file or a filesystem-realm name must be provided");
        }
        if (userPropertiesFile != null) {
            if (exposedRealmName == null) {
                throw new CommandException(formatOption(OPT_EXPOSED_REALM_NAME) + " must be set when using a user properties file");
            }
            PropertiesRealmConfiguration config = new PropertiesRealmConfiguration(exposedRealmName,
                    userPropertiesFile,
                    groupPropertiesFile,
                    relativeTo);
            return config;
        } else {
            FileSystemRealmConfiguration config = new FileSystemRealmConfiguration(exposedRealmName, fileSystemRealm, userRoleDecoder);
            return config;
        }
    }

    private AuthMechanism buildAuthMechanism(CommandContext context)
            throws Exception {
        AuthMechanism mec = null;
        if (mechanism == null) {
            return null;
        }
        if (ElytronUtil.getMechanismsWithRealm().contains(mechanism)) {
            MechanismConfiguration config = buildUserPasswordConfiguration(userPropertiesFile,
                    fileSystemRealm, userRoleDecoder, exposedRealmName,
                    groupPropertiesFile, relativeTo);
            mec = new AuthMechanism(mechanism, config);
        } else if (ElytronUtil.getMechanismsWithTrustStore().contains(mechanism)) {
            MechanismConfiguration config = buildExternalConfiguration(context, externalTrustStore);
            mec = new AuthMechanism(mechanism, config);
        } else if (ElytronUtil.getMechanismsLocalUser().contains(mechanism)) {
            MechanismConfiguration config = buildLocalUserConfiguration(context, superUser);
            mec = new AuthMechanism(mechanism, config);
        } else {
            mec = new AuthMechanism(mechanism, new EmptyConfiguration());
        }
        return mec;
    }

    private boolean shouldReload() {
        return !noReload;
    }

    private void configureBuilder(AuthSecurityBuilder builder) {
        builder.setNewRealmName(newRealmName).
                setAuthFactoryName(newAuthFactoryName).setSecurityDomainName(newSecurityDomain);
    }
}
