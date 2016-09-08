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

import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.wildfly.core.cli.command.activator.DomainOptionActivator;
import org.wildfly.core.cli.command.activator.NotExpectedOptionsActivator;

/**
 *
 * @author jdenise@redhat.com
 */
public interface DeploymentActivators {

    public static class UrlActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) pc.getCommand();
            return cmd.getPermissions().getAddOrReplacePermission().
                    isSatisfied(cmd.getCommandContext());
        }
    }

    public static class NameActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) pc.getCommand();
            return cmd.getPermissions().getDeployPermission().
                    isSatisfied(cmd.getCommandContext());
        }

    }

    public static class FileActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) pc.getCommand();
            return cmd.getPermissions().getAddOrReplacePermission().
                    isSatisfied(cmd.getCommandContext());
        }
    }

    public static class UnmanagedActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) pc.getCommand();
            return cmd.getPermissions().getMainAddPermission().
                    isSatisfied(cmd.getCommandContext());
        }
    }

    public static class RuntimeNameActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) pc.getCommand();
            return cmd.getPermissions().getAddOrReplacePermission().
                    isSatisfied(cmd.getCommandContext());
        }
    }

    public static class ForceActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) pc.getCommand();
            return cmd.getPermissions().getFullReplacePermission().
                    isSatisfied(cmd.getCommandContext());
        }
    }
    public static class ServerGroupsActivator extends NotExpectedOptionsActivator
            implements DomainOptionActivator {

        public ServerGroupsActivator() {
            super("all-server-groups");
        }

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            DeploymentControlledCommand cmd = (DeploymentControlledCommand) processedCommand.getCommand();
            if (!cmd.getPermissions().getDeployPermission().
                    isSatisfied(cmd.getCommandContext())) {
                return false;
            }
            if (!cmd.getCommandContext().isDomainMode()) {
                return false;
            }
            return super.isActivated(processedCommand);
        }
    }

    public static class AllServerGroupsActivator extends NotExpectedOptionsActivator
            implements DomainOptionActivator {

        public AllServerGroupsActivator() {
            super("server-groups");
        }

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            DeploymentControlledCommand cmd
                    = (DeploymentControlledCommand) processedCommand.getCommand();
            if (!cmd.getPermissions().getDeployPermission().
                    isSatisfied(cmd.getCommandContext())) {
                return false;
            }
            if (!cmd.getCommandContext().isDomainMode()) {
                return false;
            }
            return super.isActivated(processedCommand);
        }
    }
}
