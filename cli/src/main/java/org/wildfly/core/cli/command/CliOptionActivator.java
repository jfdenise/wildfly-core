/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.wildfly.core.cli.command;

import org.jboss.aesh.cl.activation.OptionActivator;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public interface CliOptionActivator extends OptionActivator {

    void setCommandContext(CliCommandContext commandContext);

    CliCommandContext getCommandContext();
}
