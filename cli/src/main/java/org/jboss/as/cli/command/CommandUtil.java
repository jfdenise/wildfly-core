/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command;

import java.util.Set;
import org.jboss.aesh.terminal.Shell;
import org.jboss.as.cli.util.SimpleTable;
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
}
