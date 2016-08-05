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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.command.ReadAttributeCommand;
import org.jboss.as.cli.impl.AttributeNamePathCompleter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class AttributesCompleter implements OptionCompleter<CliCompleterInvocation> {

    public static AttributesCompleter INSTANCE = new AttributesCompleter();

    private AttributesCompleter() {

    }

    @Override
    public void complete(CliCompleterInvocation cliCompleterInvocation) {
        List<String> candidates = new ArrayList<>();
        int cursor = 0;
        int pos = 0;
        if (cliCompleterInvocation.getGivenCompleteValue() != null) {
            pos = cliCompleterInvocation.getGivenCompleteValue().length();
        }

        ReadAttributeCommand c = (ReadAttributeCommand) cliCompleterInvocation.getCommand();
        OperationRequestAddress address = c.getNode();
        final ModelNode req = new ModelNode();
        if (address == null || address.isEmpty()) {
            req.get(Util.ADDRESS).setEmptyList();
        } else {
            if (address.endsOnType()) {
                return;
            }
            final ModelNode addrNode = req.get(Util.ADDRESS);
            for (OperationRequestAddress.Node node : address) {
                addrNode.add(node.getType(), node.getName());
            }
        }
        req.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
        try {
            final ModelNode response = cliCompleterInvocation.
                    getCommandContext().getModelControllerClient().execute(req);
            if (Util.isSuccess(response)) {
                if (response.hasDefined(Util.RESULT)) {
                    final ModelNode result = response.get(Util.RESULT);
                    if (result.hasDefined(Util.ATTRIBUTES)) {
                        cursor = AttributeNamePathCompleter.INSTANCE.
                                complete(cliCompleterInvocation.getGivenCompleteValue(),
                                        candidates, result.get(Util.ATTRIBUTES));
                    }
                }
            }
        } catch (IOException e) {
            // XXX OK.
        }
        cliCompleterInvocation.addAllCompleterValues(candidates);
        cliCompleterInvocation.setOffset(pos - cursor);
    }
}
