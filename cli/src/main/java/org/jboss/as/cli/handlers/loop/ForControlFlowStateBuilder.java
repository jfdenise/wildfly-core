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
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.ControlFlowState;
import org.jboss.as.cli.ControlFlowStateBuilder;

/**
 *
 * @author jdenise
 */
public class ForControlFlowStateBuilder implements ControlFlowStateBuilder {

    @Override
    public ControlFlowState build(CommandContext ctx, ParsedCommandLine line) throws CommandLineException {
        // Build an inactive ControlFlow to do minimal validation. Actual validation
        // and command execution will be done during execution.
        ForControlFlow flow = new ForHandler().buildForControlFlow(ctx, false);
        return new ForControlFlowState(flow);
    }

}
