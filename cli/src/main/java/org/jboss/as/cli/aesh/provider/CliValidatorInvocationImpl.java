/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.console.AeshContext;
import org.wildfly.core.cli.command.CliCommandContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliValidatorInvocationImpl implements CliValidatorInvocation {

    private CliCommandContext commandContext;
    private Object value;
    private Object command;
    private AeshContext aeshContext;

    public CliValidatorInvocationImpl(CliCommandContext commandContext, Object value,
            AeshContext aeshContext, Object command) {
        this.commandContext = commandContext;
        this.value = value;
        this.aeshContext = aeshContext;
        this.command = command;
    }

    @Override
    public CliCommandContext getCommandContext() {
        return commandContext;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Object getCommand() {
        return command;
    }

    @Override
    public AeshContext getAeshContext() {
        return aeshContext;
    }
}
