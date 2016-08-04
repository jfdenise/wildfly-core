/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.converter;

import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class OperationRequestAddressConverter implements Converter<OperationRequestAddress, CliConverterInvocation> {

    @Override
    public OperationRequestAddress convert(CliConverterInvocation converterInvocation) throws OptionValidatorException {
        return convert(converterInvocation.getInput(),
                converterInvocation.getCommandContext().getLegacyCommandContext());
    }

    public static OperationRequestAddress convert(String path, CommandContext ctx) throws OptionValidatorException {
        final DefaultCallbackHandler parsedOp = new DefaultCallbackHandler();
        try {
            parsedOp.parseOperation(ctx.getCurrentNodePath(), path);
            return parsedOp.getAddress();
        } catch (CommandFormatException e) {
            throw new OptionValidatorException(e.toString());
        }
    }
}