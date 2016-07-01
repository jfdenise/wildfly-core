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
package org.jboss.as.cli.aesh.completer;

import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.command.generic.NodeType;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;

/**
 *
 * Complete a node type with its instances.
 *
 * @author jdenise
 */
public class InstanceCompleter implements OptionCompleter<CliCompleterInvocation> {

    private final NodeType nodeType;

    public InstanceCompleter(NodeType nodeType) {
        this.nodeType = nodeType;
    }
    @Override
    public void complete(CliCompleterInvocation cliCompleterInvocation) {
        CommandContext ctx = cliCompleterInvocation.
                getCommandContext().getLegacyCommandContext();
        DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
        if (nodeType.dependsOnProfile() && ctx.isDomainMode()) {
            // XXX JFDENISE, SHOULD RETRIEVE PROFILE IN context.
            String profileName = null; //profile.getValue(ctx.getParsedCommandLine());
            if (profileName == null) {
                return;
            }
            address.toNode(Util.PROFILE, profileName);
        }
        for (OperationRequestAddress.Node node : nodeType.getAddress()) {
            address.toNode(node.getType(), node.getName());
        }
        List<String> all = Util.getNodeNames(ctx.getModelControllerClient(),
                        address, nodeType.getType());
        List<String> candidates = new ArrayList<>();
        for(String name : all) {
            if(name.startsWith(cliCompleterInvocation.getGivenCompleteValue())) {
                candidates.add(name);
            }
        }
        cliCompleterInvocation.
                addAllCompleterValues(candidates);
    }

}
