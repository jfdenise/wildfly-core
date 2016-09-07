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
package org.jboss.as.cli.command;

import java.io.File;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.wildfly.core.cli.command.activator.ExpectedOptionsActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.aesh.converter.FileConverter;
import org.wildfly.core.cli.command.CliCommandContext;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "save", description = "")
public class AttachmentSaveCommand extends AttachmentDisplayCommand {

    public static final class FileActivator extends ExpectedOptionsActivator {

        public FileActivator() {
            super("operation");
        }
    }

    public static final class OverwriteActivator extends ExpectedOptionsActivator {

        public OverwriteActivator() {
            super("file");
        }
    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(hasValue = true, completer = FileCompleter.class, converter = FileConverter.class, activator = FileActivator.class)
    private File file;

    @Option(hasValue = false, activator = OverwriteActivator.class)
    private boolean overwrite;

    @Override
    AttachmentResponseHandler buildHandler(CliCommandContext commandContext) {
        return new AttachmentResponseHandler((String t) -> {
            commandContext.println(t);
        }, file, true, overwrite);
    }
}
