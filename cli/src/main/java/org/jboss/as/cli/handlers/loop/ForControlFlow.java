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
package org.jboss.as.cli.handlers.loop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContext.Scope;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.parsing.command.CommandFormat;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Handles for end-for.
 *
 * @author jfdenise
 */
class ForControlFlow implements CommandLineRedirection {

    private static final String CTX_KEY = "FOR";

    static ForControlFlow get(CommandContext ctx) {
        return (ForControlFlow) ctx.get(Scope.CONTEXT, CTX_KEY);
    }

    private CommandLineRedirection.Registration registration;

    private final ModelNode forRequest;
    private final List<String> forBlock = new ArrayList<>();
    private final String varName;

    ForControlFlow(CommandContext ctx, String varName, String iterable) throws CommandLineException {
        if (iterable == null) {
            throw new IllegalArgumentException("Variable is null");
        }
        if (iterable == null) {
            throw new IllegalArgumentException("Iterable is null");
        }
        if (ctx.getVariable(varName) != null) {
            throw new CommandFormatException("Variable " + varName + " already exists.");
        }
        this.varName = varName;
        this.forRequest = ctx.buildRequest(iterable);
        // Put it temporarily with a place holder
        ctx.setVariable(varName, "null");
        ctx.set(Scope.CONTEXT, CTX_KEY, this);
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

            final String cmd = line.getOperationName();
            if ("done".equals(cmd)) {
                registration.handle(line);
            } else {
                forBlock.add(line.getOriginalLine());
            }
        } else {
            forBlock.add(line.getOriginalLine());
        }
    }

    void run(CommandContext ctx, boolean verbose) throws CommandLineException {

        try {
            final ModelControllerClient client = ctx.getModelControllerClient();
            if (client == null) {
                throw new CommandLineException("The connection to the controller has not been established.");
            }

            ModelNode targetValue;
            try {
                targetValue = ctx.execute(forRequest, "for iterable");
            } catch (IOException e) {
                throw new CommandLineException("iterable request failed", e);
            }
            if (!targetValue.hasDefined(Util.RESULT)) {
                throw new CommandLineException("iterable request failed, no result");
            }
            registration.unregister();
            ModelNode mn = targetValue.get(Util.RESULT);
            try {
                List<ModelNode> lst = mn.asList();
                for (ModelNode v : lst) {
                    String value = v.asString();
                    ctx.setVariable(varName, value);
                    if (verbose) {
                        ctx.printLine("CLI VERBOSE OUTPUT:" + varName + "=" + value);
                    }
                    for (String cmd : forBlock) {
                        ctx.handle(cmd);
                    }
                }
            } catch (Exception ex) {
                throw new CommandLineException("iterable request failed.", ex);
            }

        } finally {
            ctx.setVariable(varName, null);
            if (registration.isActive()) {
                registration.unregister();
            }
            ctx.remove(Scope.CONTEXT, CTX_KEY);
        }
    }
}
