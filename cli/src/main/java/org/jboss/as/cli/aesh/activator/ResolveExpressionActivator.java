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
package org.jboss.as.cli.aesh.activator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.converter.OperationRequestAddressConverter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliOptionActivator;

/**
 *
 * @author jdenise@redhat.com
 */
public class ResolveExpressionActivator implements CliOptionActivator {

    private CliCommandContext commandContext;

    public ResolveExpressionActivator() {
    }

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
        try {
            CommandContext ctx = commandContext.getLegacyCommandContext();
            String path = processedCommand.getArgument().getValue();
            // Workaround for Aesh parser bug.
            if ("--".equals(path)) {
                path = "";
            }
            OperationRequestAddress address = OperationRequestAddressConverter.
                    convert(path, ctx);
            List<Boolean> resHolder = new ArrayList<>();
            retrieveDescription(address, ctx, (val) -> {
                resHolder.add(val);
            });
            return resHolder.get(0);
        } catch (CommandFormatException | OptionValidatorException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public static void retrieveDescription(OperationRequestAddress address,
            CommandContext ctx, Consumer<Boolean> consumer) throws CommandFormatException {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-operation-description");
        op.get("name").set("read-attribute");
        op = Util.getAddressNode(ctx, address, op);

        ModelNode returnVal = new ModelNode();
        try {
            returnVal = ctx.getModelControllerClient().execute(op);
        } catch (IOException e) {
            throw new CommandFormatException("Failed to read resource: "
                    + e.getLocalizedMessage(), e);
        }

        if (returnVal.hasDefined("outcome") && returnVal.get("outcome").asString().equals("success")) {
            ModelNode result = returnVal.get("result");
            if (result.hasDefined("request-properties")) {
                ModelNode properties = result.get("request-properties");
                consumer.accept(properties.hasDefined("resolve-expressions"));
            }
        }
    }
}
