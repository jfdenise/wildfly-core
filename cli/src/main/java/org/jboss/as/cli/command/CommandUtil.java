/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command;

import java.io.IOException;
import java.util.Set;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.terminal.Shell;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jfdenise
 */
public class CommandUtil {

    public static void displayResponseHeaders(Shell shell, ModelNode response) {
        if (response.has(org.jboss.as.cli.Util.RESPONSE_HEADERS)) {
            final ModelNode headers = response.get(org.jboss.as.cli.Util.RESPONSE_HEADERS);
            final Set<String> keys = headers.keys();
            final SimpleTable table = new SimpleTable(2);
            for (String key : keys) {
                table.addLine(new String[]{key + ':', headers.get(key).asString()});
            }
            final StringBuilder buf = new StringBuilder();
            table.append(buf, true);
            shell.out().println(buf.toString());
        }
    }

    public static ModelNode execute(ModelNode request, CommandContext ctx) throws CommandException {
        final ModelControllerClient client = ctx.getModelControllerClient();
        try {
            ModelNode response = client.execute(request);
            if (!Util.isSuccess(response)) {
                throw new CommandException(Util.getFailureDescription(response));
            }
            return response;
        } catch (IOException | CommandException e) {
            throw new CommandException(e.getMessage(), e);
        }
    }
}
