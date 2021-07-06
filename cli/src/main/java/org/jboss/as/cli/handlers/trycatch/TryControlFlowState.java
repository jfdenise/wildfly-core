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
package org.jboss.as.cli.handlers.trycatch;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.ControlFlowStateHandler;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.ControlFlowState;

/**
 *
 * @author jdenise
 */
public class TryControlFlowState implements ControlFlowState {

    private static final int IN_TRY = 0;
    private static final int IN_CATCH = 1;
    private static final int IN_FINALLY = 2;
    private int state;

    boolean isInTry() {
        return state == IN_TRY;
    }

    boolean isInFinally() {
        return state == IN_FINALLY;
    }

    @Override
    public void typedCommand(CommandContext ctx, ParsedCommandLine line) throws CommandLineException {
        String cmd = line.getOperationName();
        if ("end-try".equals(cmd)) {
            if(isInTry()) {
                throw new CommandLineException("end-if may appear only after catch or finally");
            }
            ControlFlowStateHandler.pop(this);
        } else {
            if ("catch".equals(cmd)) {
                if (isInTry()) {
                    moveToCatch();
                } else {
                    throw new CommandLineException("catch may appear only once after try and before finally");
                }
            } else {
                if ("finally".equals(cmd)) {
                    if (isInFinally()) {
                        throw new CommandLineException("Already in finally");
                    }
                    moveToFinally();
                }
            }
        }
    }

    void moveToCatch() throws CommandLineException {
        switch (state) {
            case IN_TRY:
                state = IN_CATCH;
                break;
            case IN_CATCH:
                throw new CommandLineException("Already in catch block. Only one catch block is allowed.");
            case IN_FINALLY:
                throw new CommandLineException("Catch block is not allowed in finally");
            default:
                throw new IllegalStateException("Unexpected block id: " + state);
        }
    }

    void moveToFinally() throws CommandLineException {
        switch (state) {
            case IN_TRY:
            case IN_CATCH:
                state = IN_FINALLY;
                break;
            case IN_FINALLY:
                throw new CommandLineException("Already in finally");
            default:
                throw new IllegalStateException("Unexpected block id: " + state);
        }
    }
}
