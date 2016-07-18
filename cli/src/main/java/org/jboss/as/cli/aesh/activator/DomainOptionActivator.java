/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.activator;

import org.wildfly.core.cli.command.CliOptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.wildfly.core.cli.command.CliCommandContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DomainOptionActivator implements CliOptionActivator {
    private CliCommandContext commandContext;

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
        return commandContext.isDomainMode();
    }
}