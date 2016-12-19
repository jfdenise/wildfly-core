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
package org.jboss.as.cli.impl.aesh.commands.security;

import java.io.IOException;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.aesh.commands.activator.ConnectedActivator;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class ControlledCommandActivator extends ConnectedActivator {

    @Override
    public boolean isActivated(ProcessedCommand cmd) {
        if (getCommandContext().isLegacyMode()) {
            return false;
        }
        if (!super.isActivated(cmd)) {
            return false;
        }
        ControlledCommand controlled = (ControlledCommand) cmd.getCommand();
        CommandContext ctx = getCommandContext();
        if (controlled.isDependsOnProfile() && ctx.isDomainMode()) { // not checking address in all the profiles
            return ctx.getConfig().isAccessControl()
                    ? controlled.getAccessRequirement().isSatisfied(ctx) : true;
        }

        boolean available;
        if (controlled.getRequiredType() == null) {
            available = isAddressValid(ctx, controlled.getRequiredAddress());
        } else {
            final ModelNode request = new ModelNode();
            final ModelNode address = request.get(Util.ADDRESS);
            for (OperationRequestAddress.Node node : controlled.getRequiredAddress()) {
                address.add(node.getType(), node.getName());
            }
            request.get(Util.OPERATION).set(Util.READ_CHILDREN_TYPES);
            ModelNode result;
            try {
                result = ctx.getModelControllerClient().execute(request);
            } catch (IOException e) {
                return false;
            }
            available = Util.listContains(result, controlled.getRequiredType());
        }

        if (ctx.getConfig().isAccessControl()) {
            available = available && controlled.getAccessRequirement().isSatisfied(ctx);
        }
        return available;
    }

    protected boolean isAddressValid(CommandContext ctx,
            OperationRequestAddress requiredAddress) {
        final ModelNode request = new ModelNode();
        final ModelNode address = request.get(Util.ADDRESS);
        address.setEmptyList();
        request.get(Util.OPERATION).set(Util.VALIDATE_ADDRESS);
        final ModelNode addressValue = request.get(Util.VALUE);
        for (OperationRequestAddress.Node node : requiredAddress) {
            addressValue.add(node.getType(), node.getName());
        }
        final ModelNode response;
        try {
            response = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return false;
        }
        final ModelNode result = response.get(Util.RESULT);
        if (!result.isDefined()) {
            return false;
        }
        final ModelNode valid = result.get(Util.VALID);
        if (!valid.isDefined()) {
            return false;
        }
        return valid.asBoolean();
    }
}
