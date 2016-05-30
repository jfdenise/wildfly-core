/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.console.command.converter.ConverterInvocation;
import org.jboss.aesh.console.command.converter.ConverterInvocationProvider;
import org.jboss.as.cli.CliCommandContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliConverterInvocationProvider implements ConverterInvocationProvider<CliConverterInvocation> {

    private final CliCommandContext commandContext;

    public CliConverterInvocationProvider(CliCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    @Override
    public CliConverterInvocation enhanceConverterInvocation(ConverterInvocation converterInvocation) {
        return new CliConverterInvocation(commandContext,
                converterInvocation.getAeshContext(), converterInvocation.getInput());
    }
}
