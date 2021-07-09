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
package org.jboss.as.cli.handlers.loop;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContext.Scope;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.ControlFlowStateHandler;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.VariableState;
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
    private final CommandContext ctx;
    ForControlFlow(CommandContext ctx, String varName, String iterable) throws CommandLineException {
        this(ctx, varName, iterable, true);
    }

    ForControlFlow(CommandContext ctx, String varName, String iterable, boolean active) throws CommandLineException {
        checkNotNullParam("ctx", ctx);
        checkNotNullParam("varName", varName);
        checkNotNullParam("iterable", iterable);
        this.ctx = ctx;
        if (ctx.getVariable(varName) != null) {
            throw new CommandFormatException("Variable " + varName + " already exists.");
        }
        this.varName = varName;
        if (active) {
            // iterable could be a variable
            VariableState v = VariableState.buildVariable(iterable, ctx);
            if (v == null) {
                ModelNode forRequest = ctx.buildRequest(iterable);
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
                ModelNode mn = targetValue.get(Util.RESULT);
                try {
                    result = mn.asList();
                } catch (Exception ex) {
                    throw new CommandLineException("for cannot be used with operations that produce a non-iterable result");
                }
            } else {
                 ModelNode val =  v.getValue(ctx);
                 try {
                    result = val.asList();
                } catch (Exception ex) {
                    throw new CommandLineException("for cannot be used with a variable that is not iterable");
                }
            }
        } else {
            if (iterable.length() == 0) {
                throw new CommandLineException("The line is null or empty.");
            }
            result = null;
        }
        // Define the variable with a dummy value. That is required for operations in the block
        // referencing this variable otherwise parsing would fail.
        ctx.setVariable(varName, "null");
        if (active) {
            ctx.set(Scope.CONTEXT, CTX_KEY, this);
        }
    }

    void reset() throws CommandLineException{
        ctx.setVariable(varName, null);
    }

    @Override
    public void set(CommandLineRedirection.Registration registration) {
        this.registration = registration;
    }

    @Override
    public void handle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine line = ctx.getParsedCommandLine();
        final String cmd = line.getOperationName();
        boolean built = false;
        if (line.getFormat() == CommandFormat.INSTANCE) {
            // The command could be a new WorkFlow one
            built = ControlFlowStateHandler.buildWorkFlow(ctx, line);
            // let the help through
            if (line.hasProperty("--help")
                    || line.hasProperty("-h")
                    || ("done".equals(cmd) && ControlFlowStateHandler.isEmpty()) || "help".equals(cmd)) {
                registration.handle(line);
                return;
            }
        }
        if (!built) {
            ControlFlowStateHandler.command(ctx, line);
        }
        forBlock.add(line.getOriginalLine());
    }

    void run(CommandContext ctx, boolean discard) throws CommandLineException {
        try {
            registration.unregister();
            ctx.remove(Scope.CONTEXT, CTX_KEY);
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
