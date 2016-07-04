/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.aesh.activator;

/**
 *
 * @author jdenise@redhat.com
 */
import org.wildfly.core.cli.command.CliCommandActivator;
import org.wildfly.core.cli.command.CliCommandContext;

public class ConnectedActivator implements CliCommandActivator {

    private CliCommandContext ctx;

    public ConnectedActivator() {
    }

    @Override
    public boolean isActivated() {
        return ctx.isConnected();
    }

    @Override
    public void setCommandContext(CliCommandContext commandContext) {
        this.ctx = commandContext;
    }

    @Override
    public CliCommandContext getCommandContext() {
        return ctx;
    }
}
