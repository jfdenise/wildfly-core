/*
Copyright 2021 Red Hat, Inc.

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
package org.jboss.as.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 * Handles a stack of states. Automatically build ControlFlowState when a
 * command that starts a new control flow is seen.
 *
 * @author jdenise
 */
public abstract class ControlFlowStateHandler {

    private static final Map<String, ControlFlowStateBuilder> COMPLETER_BUILDERS = new HashMap<>();
    private static final Stack<ControlFlowState> STATES = new Stack<>();
    private static boolean isBatch;

    public static boolean isBatch() {
        return isBatch;
    }

    /**
     * Register a state builder for the given command name. Builder will be used
     * to instantiate the state when the command is typed.
     */
    public static void registerBuilder(String command, ControlFlowStateBuilder completerBuilder) {
        COMPLETER_BUILDERS.put(command, completerBuilder);
    }

    /**
     * Returns the current state or null if non.
     */
    public static ControlFlowState getCurrent() {
        if (STATES.isEmpty()) {
            return null;
        }
        return STATES.peek();
    }

    public static boolean isEmpty() {
        return STATES.isEmpty();
    }

    private static void push(ControlFlowState state) {
        STATES.push(state);
    }

    /**
     * Remove the latest stacked state. If the provided state is not equal to
     * the latest state, an exception is thrown.
     *
     * @param state
     */
    public static void pop(ControlFlowState state) throws CommandLineException {
        ControlFlowState removed = STATES.pop();
        if (removed != state) {
            throw new CommandLineException("Invalid state to remove: " + state);
        }
    }

    /**
     * Called by the top level Control Workflow command for each typed command.
     */
    public static boolean buildWorkFlow(CommandContext ctx, ParsedCommandLine line) throws CommandLineException {
        ControlFlowStateBuilder builder = COMPLETER_BUILDERS.get(line.getOperationName());
        if (builder != null) {
            if (isBatch) {
                throw new CommandLineException("Command can't be called in batch mode");
            }
            ControlFlowState state = builder.build(ctx, line);
            push(state);
            return true;
        }
        if ("run-batch".equals(line.getOperationName()) || "discard-batch".equals(line.getOperationName())) {
            if (!isBatch) {
                throw new CommandLineException("No active batch.");
            }
            isBatch = false;
        } else if ("batch".equals(line.getOperationName())) {
            if (isBatch) {
                throw new CommandLineException("Can't start a new batch while in batch mode.");
            }
            isBatch = true;
        }
        return false;
    }

    public static void command(CommandContext ctx, ParsedCommandLine line) throws CommandLineException {
        ControlFlowState current = getCurrent();
        if (current != null) {
            current.typedCommand(ctx, line);
        }
    }
}
