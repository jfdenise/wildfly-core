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
package org.jboss.as.cli.impl.aesh.cmd;

import java.io.IOException;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.readline.Prompt;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "sample", description = "")
public class SampleCommand implements Command<CLICommandInvocation> {

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext();
        final ModelControllerClient client = ctx.getModelControllerClient();
        if (client == null) {
            ctx.print("<connect to the controller and re-run the version command to see the release info>\n");
        } else {
            final ModelNode req = new ModelNode();
            req.get(Util.OPERATION).set("whoami");
            req.get(Util.ADDRESS).setEmptyList();
            try {
                final ModelNode response = client.execute(req);
                if (Util.isSuccess(response)) {
                    if (response.hasDefined(Util.RESULT)) {
                        final ModelNode result = response.get(Util.RESULT);
                        ctx.print(result.asString());
                    } else {
                        ctx.print("result was not available.");
                    }
                } else {
                    ctx.print(Util.getFailureDescription(response));
                }
                String reply = null;
                while (reply == null) {
                    reply = commandInvocation.inputLine(new Prompt("Are you OK? y/n:"));
                    if (reply != null && reply.equals("y")) {
                        break;
                    } else if (reply != null && !reply.equals("n")) {
                        reply = null;
                    }
                }
            } catch (IOException e) {
                throw new CommandException("Failed to get the whoami: " + e.getLocalizedMessage());
            }
        }
        return CommandResult.SUCCESS;
    }

}
