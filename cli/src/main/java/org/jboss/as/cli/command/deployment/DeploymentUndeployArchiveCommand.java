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
package org.jboss.as.cli.command.deployment;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.command.ControlledCommandActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "undeploy-archive", description = "", activator = ControlledCommandActivator.class)
public class DeploymentUndeployArchiveCommand extends DeploymentArchiveCommand {

    public DeploymentUndeployArchiveCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    protected String getAction() {
        return "undeploy-archive";
    }

    @Override
    protected String getDefaultScript() {
        return "undeploy.scr";
    }

    @Override
    protected AccessRequirement buildAccessRequirement(CommandContext ctx) {
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .requirement(getPermissions().getMainRemovePermission())
                .requirement(getPermissions().getUndeployPermission())
                .build();
    }
}
