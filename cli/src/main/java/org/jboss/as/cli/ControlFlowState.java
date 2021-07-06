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

import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 * When a control flow command is hidden inside a top level control flow command
 * each typed command can change the Completion and visibility state of the hidden control flow commands.
 * Implement this interface to track control flow state changes.
 * @author jdenise
 */
public interface ControlFlowState {

    public void typedCommand(CommandContext ctx, ParsedCommandLine line) throws CommandLineException;

}
