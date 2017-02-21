/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.patching.cli;

import java.util.List;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.patching.tool.PatchOperationBuilder;
import org.wildfly.core.cli.command.aesh.activator.HiddenActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "rollback", description = "")
public class PatchRollback extends PatchOverrideCommand {

    @Option(name = "reset-configuration", hasValue = true, required = true)
    private boolean resetConfiguration;

    @Option(name = "rollback-to", hasValue = false, required = false)
    private boolean rollbackTo;

    @Option(name = "patch-stream", hasValue = true, required = false)
    private String patchStream;

    @Option(name = "patch-id", required = false, activator = HiddenActivator.class)
    private String patchId;

    @Arguments(completer = PatchIdCompleter.class)
    private List<String> patchIdArg;

    public PatchRollback(String action) {
        super("rollback");
    }

    private String getPatchId() throws CommandException {
        if (patchId != null && (patchIdArg != null && !patchIdArg.isEmpty())) {
            throw new CommandException("patch-id argument and options can't be set al together.");
        }
        if (patchId != null) {
            return patchId;
        }
        if (patchIdArg != null && !patchIdArg.isEmpty()) {
            return patchIdArg.get(0);
        }
        return null;
    }


    @Override
    protected PatchOperationBuilder createUnconfiguredOperationBuilder(CommandContext ctx) throws CommandException {
        PatchOperationBuilder builder;
        String patchId = getPatchId();
        if (patchId != null) {
            builder = PatchOperationBuilder.Factory.rollback(patchStream, patchId, rollbackTo, resetConfiguration);
        } else {
            builder = PatchOperationBuilder.Factory.rollbackLast(patchStream, resetConfiguration);
        }
        return builder;
    }

}
