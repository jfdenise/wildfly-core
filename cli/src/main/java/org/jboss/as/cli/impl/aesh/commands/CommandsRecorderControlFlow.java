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
package org.jboss.as.cli.impl.aesh.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.utils.Config;
import org.jboss.as.cli.CliInitializationException;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContext.Scope;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.impl.StoredCommands;
import org.jboss.as.cli.impl.StoredCommands.StoredCommand;
import static org.jboss.as.cli.impl.StoredCommands.newStoredCommand;
import org.jboss.as.cli.impl.aesh.CLICommandRegistry;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.parsing.command.CommandFormat;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 * Handles for end-for.
 *
 * @author jfdenise
 */
class CommandsRecorderControlFlow implements CommandLineRedirection {

    private static final String CTX_KEY = "COMMANDS_RECORDER";

    static CommandsRecorderControlFlow get(CommandContext ctx) {
        return (CommandsRecorderControlFlow) ctx.get(Scope.CONTEXT, CTX_KEY);
    }

    private CommandLineRedirection.Registration registration;

    private final List<String> recorded = new ArrayList<>();

    CommandsRecorderControlFlow(CommandContext ctx) throws CommandLineException {
        ctx.set(Scope.CONTEXT, CTX_KEY, this);
    }

    public List<String> getRecorded() {
        return Collections.unmodifiableList(recorded);
    }

    @Override
    public String getPrompt() {
        return "REC";
    }

    @Override
    public void set(CommandLineRedirection.Registration registration) {
        this.registration = registration;
    }

    @Override
    public void handle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine line = ctx.getParsedCommandLine();
        if (line.getFormat() == CommandFormat.INSTANCE) {

            // let the help through
            if (line.hasProperty("--help") || line.hasProperty("-h")) {
                registration.handle(line);
                return;
            }

            // XXX jfdenise, we should be able to do better than that with typed
            // command and proper API to stop the redirection.
            String opName = line.getOperationName();
            if (opName.equals("commands")
                    && (line.getOriginalLine().contains("stop-record") || line.getOriginalLine().contains("list-recorded"))) {
                registration.handle(line);
            } else if (opName.equals("commands")
                    && line.getOriginalLine().contains("record")) {
                throw new CommandLineException("Recording action can't be added to a recording session");
            } else {
                recorded.add(line.getOriginalLine());
            }
        } else {
            recorded.add(line.getOriginalLine());
        }
    }

    void run(CLICommandInvocation ctx, String name, boolean trans, File export) throws CommandLineException, IOException {
        try {
            if (recorded.isEmpty()) {
                ctx.println("No commands to store, leaving recording session.");
                return;
            }
            if (export == null) {
                name = name == null ? "stored-cmd" + System.currentTimeMillis() : name;
            } else {
                if (!export.exists()) {
                    export.createNewFile();
                }
                StringBuilder builder = new StringBuilder();
                for (String cmd : recorded) {
                    builder.append(cmd).append(Config.getLineSeparator());
                }
                Files.write(export.toPath(), builder.toString().getBytes());
                ctx.println("Commands successfully exported to" + export.getAbsolutePath());
            }
            if (name != null) {
                try {
                    if (!trans) {
                        StoredCommands.saveCommand(name, recorded);
                        ctx.println("Command " + name + " successfully persisted.");
                    }
                    CLICommandRegistry reg = (CLICommandRegistry) ctx.getCommandRegistry();
                    reg.addCommand(newStoredCommand(new StoredCommand(name, recorded)));
                } catch (IOException | CliInitializationException | CommandLineParserException ex) {
                    throw new CommandLineException(ex);
                }
            }
        } finally {
            if (registration.isActive()) {
                registration.unregister();
            }
            ctx.getCommandContext().remove(Scope.CONTEXT, CTX_KEY);
        }
    }
}
