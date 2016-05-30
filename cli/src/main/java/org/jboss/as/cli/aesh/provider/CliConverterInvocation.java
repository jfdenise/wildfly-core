/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.command.converter.ConverterInvocation;
import org.jboss.as.cli.CliCommandContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliConverterInvocation implements ConverterInvocation {

    private final CliCommandContext commandContext;
    private final String input;
    private final AeshContext aeshContext;

    public CliConverterInvocation(CliCommandContext commandContext,
            AeshContext aeshContext, String input) {
        this.input = input;
        this.commandContext = commandContext;
        this.aeshContext = aeshContext;
    }

    @Override
    public String getInput() {
        return input;
    }

    @Override
    public AeshContext getAeshContext() {
        return aeshContext;
    }

    public CliCommandContext getCommandContext() {
        return commandContext;
    }
}
