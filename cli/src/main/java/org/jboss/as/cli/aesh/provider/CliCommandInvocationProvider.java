/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocationProvider;
import org.jboss.as.cli.CliCommandContext;
import org.jboss.as.cli.command.CliCommandInvocation;
import org.jboss.as.cli.impl.Console;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliCommandInvocationProvider implements CommandInvocationProvider<CliCommandInvocation> {

    private final CliCommandContext commandContext;
    private final Console console;

    public CliCommandInvocationProvider(final CliCommandContext commandContext, Console console) {
        this.commandContext = commandContext;
        this.console = console;
    }

    @Override
    public CliCommandInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
        return new CliCommandInvocation(commandContext, commandInvocation, console);
    }
}
