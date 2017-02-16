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
package org.jboss.as.cli.impl.aesh.commands.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.Command;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.impl.aesh.completer.HeadersCompleter;
import org.jboss.as.cli.impl.aesh.converter.HeadersConverter;
import org.wildfly.core.cli.command.aesh.CLICompleterInvocation;
import org.jboss.as.cli.impl.CommaSeparatedCompleter;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentActivators.AllServerGroupsActivator;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentActivators.ServerGroupsActivator;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 * XXX jfdenise, all fields are public to be accessible from legacy view. To be
 * made private when removed.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "abstract-sub-deployment", description = "")
public abstract class DeploymentAbstractSubCommand extends DeploymentControlledCommand
        implements Command<CLICommandInvocation>, BatchCompliantCommand {

    public static class ServerGroupsCompleter implements
            OptionCompleter<CLICompleterInvocation> {

        @Override
        public void complete(CLICompleterInvocation completerInvocation) {
            DeploymentControlledCommand rc = (DeploymentControlledCommand) completerInvocation.getCommand();

            CommaSeparatedCompleter comp = new CommaSeparatedCompleter() {
                @Override
                protected Collection<String> getAllCandidates(CommandContext ctx) {
                    return rc.getPermissions().getServerGroupAddPermission().
                            getAllowedOn(ctx);
                }
            };
            List<String> candidates = new ArrayList<>();
            int offset = comp.complete(completerInvocation.getCommandContext(),
                    completerInvocation.getGivenCompleteValue(), 0, candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(offset);
        }
    }

    @Option(name = "server-groups", activator = ServerGroupsActivator.class,
            completer = ServerGroupsCompleter.class, required = false)
    public String serverGroups;

    @Option(name = "all-server-groups", activator = AllServerGroupsActivator.class,
            hasValue = false, required = false)
    public boolean allServerGroups;

    @Option(converter = HeadersConverter.class, completer = HeadersCompleter.class,
            required = false)
    public ModelNode headers;

    DeploymentAbstractSubCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(CommandContext commandContext,
            Attachments attachments) {
        return null;
    }
}
