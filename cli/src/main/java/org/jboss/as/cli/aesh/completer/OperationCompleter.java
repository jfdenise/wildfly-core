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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.command.generic.NodeType;
import org.jboss.as.cli.handlers.OperationCommandWithDescription;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;

/**
 *
 * Complete operations.
 *
 * @author jdenise
 */
public class OperationCompleter implements OptionCompleter<CliCompleterInvocation> {

    private final NodeType nodeType;
    private final Set<String> excludedOps;
    // XXX JFDENISE, WE SHOULD BE ABLE TO REMOVE THESE.
    private final Map<String, OperationCommandWithDescription> customHandlers;

    public OperationCompleter(NodeType nodeType,
            Map<String, OperationCommandWithDescription> customHandlers,
            String... excludedOps) {
        this.nodeType = nodeType;
        this.excludedOps = excludedOps == null ? Collections.<String>emptySet()
                : new HashSet<>(Arrays.asList(excludedOps));
        this.customHandlers = customHandlers;
    }

    public Collection<String> getCandidates(CommandContext ctx, String profileName) {
        DefaultOperationRequestAddress address = new DefaultOperationRequestAddress();
        if (nodeType.dependsOnProfile() && ctx.isDomainMode()) {
            if (profileName == null) {
                return Collections.emptyList();
            }
            address.toNode(Util.PROFILE, profileName);
        }
        for (OperationRequestAddress.Node node : nodeType.getAddress()) {
            address.toNode(node.getType(), node.getName());
        }
        address.toNode(nodeType.getType(), "?");
        Collection<String> ops = Util.getOperationNames(ctx, address);
        ops.removeAll(excludedOps);
        if (customHandlers != null) {
            if (ops.isEmpty()) {
                ops = customHandlers.keySet();
            } else {
                ops = new HashSet<String>(ops);
                for (Map.Entry<String, OperationCommandWithDescription> entry : customHandlers.entrySet()) {
                    if (entry.getValue().isAvailable(ctx)) {
                        ops.add(entry.getKey());
                    } else {
                        ops.remove(entry.getKey()); // in case custom handler overrides the default op
                    }
                }
            }
        }
        return ops;
    }

    @Override
    public void complete(CliCompleterInvocation cliCompleterInvocation) {
        // XXX JFDENISE MUST RETRIEVE profile FROM completer.
        cliCompleterInvocation.
                addAllCompleterValues(getCandidates(cliCompleterInvocation.
                        getCommandContext().getLegacyCommandContext(), null));
    }

}
