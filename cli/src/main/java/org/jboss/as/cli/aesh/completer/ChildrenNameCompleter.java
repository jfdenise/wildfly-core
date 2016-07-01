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

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.command.generic.NodeType;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;

/**
 *
 * Completes properties of a resource type.
 *
 * @author jdenise
 */
public class ChildrenNameCompleter implements OptionCompleter<CliCompleterInvocation> {

    private final CommandContext context;
    private final NodeType type;
    private final OperationRequestAddress address;

    public ChildrenNameCompleter(CommandContext context,
            OperationRequestAddress address, NodeType type) {
        this.context = context;
        this.address = address;
        this.type = type;
    }

    @Override
    public void complete(CliCompleterInvocation cliCompleterInvocation) {
        cliCompleterInvocation.
                addAllCompleterValues(Util.getNodeNames(context.getModelControllerClient(),
                        address, type.getType()));

    }
}
