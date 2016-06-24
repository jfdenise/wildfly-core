/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.console.command.activator.OptionActivatorProvider;
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.aesh.activator.CliOptionActivator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliOptionActivatorProvider implements OptionActivatorProvider {

    private final CliCommandContext commandContext;

    public CliOptionActivatorProvider(CliCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    @Override
    public OptionActivator enhanceOptionActivator(OptionActivator optionActivator) {

        if(optionActivator instanceof CliOptionActivator)
            ((CliOptionActivator) optionActivator).setCommandContext(commandContext);

        return optionActivator;
    }
}
