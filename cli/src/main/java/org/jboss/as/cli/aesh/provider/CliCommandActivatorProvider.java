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
package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.cl.activation.CommandActivator;
import org.jboss.aesh.console.command.activator.CommandActivatorProvider;
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.aesh.activator.CliOptionActivator;

/**
 * @author jdenise@redhat.com
 */
public class CliCommandActivatorProvider implements CommandActivatorProvider {

    private final CliCommandContext commandContext;

    public CliCommandActivatorProvider(CliCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    @Override
    public CommandActivator enhanceCommandActivator(CommandActivator commandActivator) {

        if(commandActivator instanceof CliOptionActivator)
            ((CliOptionActivator) commandActivator).setCommandContext(commandContext);

        return commandActivator;
    }
}
