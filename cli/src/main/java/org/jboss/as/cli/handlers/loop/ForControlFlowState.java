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
package org.jboss.as.cli.handlers.loop;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.ControlFlowStateHandler;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.ControlFlowState;

/**
 *
 * @author jdenise
 */
public class ForControlFlowState implements ControlFlowState {
    private final ForControlFlow flow;
    ForControlFlowState(ForControlFlow flow) {
        this.flow = flow;
    }
    @Override
    public void typedCommand(CommandContext ctx, ParsedCommandLine line) throws CommandLineException{
        String cmd = line.getOperationName();
        if ("done".equals(cmd)) {
            ControlFlowStateHandler.pop(this);
            flow.reset();
        }
    }

}
