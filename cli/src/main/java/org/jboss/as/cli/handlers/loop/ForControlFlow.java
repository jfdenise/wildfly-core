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
 * Implements the loop iteration logic.
 *
 * @author jfdenise
 */
class ForControlFlow implements CommandLineRedirection {

    private static final String CTX_KEY = "FOR";

    static ForControlFlow get(CommandContext ctx) {
        return (ForControlFlow) ctx.get(Scope.CONTEXT, CTX_KEY);
    }

    private CommandLineRedirection.Registration registration;

    private final List<ModelNode> result;
    private final List<String> forBlock = new ArrayList<>();
    private final String varName;

    ForControlFlow(CommandContext ctx, String varName, String iterable) throws CommandLineException {
        if (varName == null) {
            throw new IllegalArgumentException("Variable is null");
        }
        if (iterable == null) {
            throw new IllegalArgumentException("Iterable is null");
        }
        if (ctx.getVariable(varName) != null) {
            throw new CommandFormatException("Variable " + varName + " already exists.");
        }

        final ModelControllerClient client = ctx.getModelControllerClient();
        if (client == null) {
            throw new CommandLineException("The connection to the controller has not been established.");
        }

        this.varName = varName;
        ModelNode forRequest = ctx.buildRequest(iterable);

        ModelNode targetValue;
        try {
            targetValue = ctx.execute(forRequest, "for iterable");
        } catch (IOException e) {
            throw new CommandLineException("iterable request failed", e);
        }
        if (!targetValue.hasDefined(Util.RESULT)) {
            throw new CommandLineException("iterable request failed, no result");
        }
        ModelNode mn = targetValue.get(Util.RESULT);
        result = mn.asList();
        // Define the variable with a dummy value. That is required for operations in the block
        // referencing this variable otherwise parsing would fail.
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
            final String cmd = line.getOperationName();
            if ("for".equals(cmd)) {
                throw new CommandFormatException("for is not allowed while in for block");
            }
            if (line.hasProperty("--help")
                    || line.hasProperty("-h")
                    || "done".equals(cmd) || "help".equals(cmd)) {
                registration.handle(line);
                return;
            }
        }
        forBlock.add(line.getOriginalLine());
    }

    void run(CommandContext ctx, boolean discard) throws CommandLineException {
        try {
            registration.unregister();
            if (!discard) {
                for (ModelNode v : result) {
                    String value = v.asString();
                    ctx.setVariable(varName, value);
                    for (String cmd : forBlock) {
                        ctx.handle(cmd);
                    }
                }
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
