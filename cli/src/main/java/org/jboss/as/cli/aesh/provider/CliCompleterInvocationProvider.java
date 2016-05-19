/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.completer.CompleterInvocationProvider;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.as.cli.CommandContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliCompleterInvocationProvider implements CompleterInvocationProvider<CliCompleterInvocation> {

    private final CommandContext ctx;
    private final CommandRegistry registry;

    public CliCompleterInvocationProvider(CommandContext ctx,
            CommandRegistry registry) {
        this.ctx = ctx;
        this.registry = registry;
    }

    @Override
    public CliCompleterInvocation enhanceCompleterInvocation(CompleterInvocation completerInvocation) {
        return new CliCompleterInvocation(completerInvocation, ctx, registry);
    }
}
