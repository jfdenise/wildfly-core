/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.impl.aesh.cmd.security.ssl;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.aesh.cmd.security.HttpServerCommandActivator;
import org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_NO_RELOAD;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_SERVER_NAME;
import org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OptionCompleters;
import org.jboss.as.cli.impl.aesh.cmd.security.model.HTTPServer;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "disable-ssl-http-server", description = "", activator = HttpServerCommandActivator.class)
public class HTTPServerDisableSSLCommand implements Command<CLICommandInvocation>, DMRCommand {
    @Option(name = OPT_NO_RELOAD, hasValue = false)
    boolean noReload;

    @Option(name = OPT_SERVER_NAME, completer = OptionCompleters.ServerNameCompleter.class)
    String serverName;

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext();
        ModelNode request;
        try {
            request = buildRequest(ctx);
        } catch (CommandFormatException ex) {
            throw new CommandException(ex.getLocalizedMessage(), ex);
        }
        SecurityCommand.execute(ctx, request, noReload);
        ctx.printLine("SSL disabled for " + serverName);
        return CommandResult.SUCCESS;
    }

    @Override
    public ModelNode buildRequest(CommandContext context) throws CommandFormatException {
        ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        try {
            serverName = HTTPServer.disableSSL(context,
                    serverName, composite.get(Util.STEPS));
        } catch (Exception ex) {
            throw new CommandFormatException(ex.getLocalizedMessage() == null
                    ? ex.toString() : ex.getLocalizedMessage(), ex);
        }
        return composite;
    }

}
