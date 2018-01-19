/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.cli.impl.aesh.cmd.security.auth;

import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.internal.ParsedCommand;
import org.aesh.command.impl.internal.ParsedOption;
import org.wildfly.core.cli.command.aesh.activator.AbstractDependOptionActivator;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_FILE_SYSTEM_REALM;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_MECHANISM;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_USER_PROPERTIES_FILE;
import org.jboss.as.cli.impl.aesh.cmd.security.model.ElytronUtil;
import org.wildfly.core.cli.command.aesh.activator.AbstractRejectOptionActivator;

/**
 *
 * @author jdenise@redhat.com
 */
public class OptionActivators {

    public static class MechanismWithRealmActivator extends AbstractDependOptionActivator {

        public MechanismWithRealmActivator() {
            super(false, OPT_MECHANISM);
        }
        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            if (!super.isActivated(parsedCommand)) {
                return false;
            }
            ParsedOption opt = parsedCommand.findLongOptionNoActivatorCheck(OPT_MECHANISM);
            return ElytronUtil.getMechanismsWithRealm().contains(opt.value());
        }

    }

    public static class FilesystemRealmActivator extends AbstractRejectOptionActivator {

        public FilesystemRealmActivator() {
            super(OPT_USER_PROPERTIES_FILE);
        }

        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            if (!super.isActivated(parsedCommand)) {
                return false;
            }
            ParsedOption opt = parsedCommand.findLongOptionNoActivatorCheck(OPT_MECHANISM);
            return ElytronUtil.getMechanismsWithRealm().contains(opt.value());
        }

    }

    public static class PropertiesFileRealmActivator extends AbstractRejectOptionActivator {

        public PropertiesFileRealmActivator() {
            super(OPT_FILE_SYSTEM_REALM);
        }

        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            if (!super.isActivated(parsedCommand)) {
                return false;
            }
            ParsedOption opt = parsedCommand.findLongOptionNoActivatorCheck(OPT_MECHANISM);
            return ElytronUtil.getMechanismsWithRealm().contains(opt.value());
        }

    }

    public static class MechanismWithTrustStore extends AbstractDependOptionActivator {

        public MechanismWithTrustStore() {
            super(false, OPT_MECHANISM);
        }
        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            if (!super.isActivated(parsedCommand)) {
                return false;
            }
            ParsedOption opt = parsedCommand.findLongOptionNoActivatorCheck(OPT_MECHANISM);
            return ElytronUtil.getMechanismsWithTrustStore().contains(opt.value());
        }

    }

    public static class DependsOnMechanism extends AbstractDependOptionActivator {

        public DependsOnMechanism() {
            super(false, OPT_MECHANISM);
        }
    }

    public static class SuperUserActivator implements OptionActivator {

        @Override
        public boolean isActivated(ParsedCommand parsedCommand) {
            ParsedOption opt = parsedCommand.findLongOptionNoActivatorCheck(OPT_MECHANISM);
            return opt != null && opt.value() != null && ElytronUtil.getMechanismsLocalUser().contains(opt.value());
        }
    }

    public static class GroupPropertiesFileActivator extends AbstractDependOptionActivator {

        public GroupPropertiesFileActivator() {
            super(false, OPT_USER_PROPERTIES_FILE);
        }
    }

    public static class RelativeToActivator extends AbstractDependOptionActivator {

        public RelativeToActivator() {
            super(false, OPT_USER_PROPERTIES_FILE);
        }
    }

    public static class FileSystemRoleDecoderActivator extends AbstractDependOptionActivator {

        public FileSystemRoleDecoderActivator() {
            super(false, OPT_FILE_SYSTEM_REALM);
        }
    }
}
