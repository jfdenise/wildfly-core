/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.console;

import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.Shell;
import org.jboss.as.cli.console.AeshCliConsole.CliResultHandler;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliCommandNotFound implements CommandNotFoundHandler {

    private final CliResultHandler handler;

    CliCommandNotFound(CliResultHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handleCommandNotFound(String line, Shell shell) {
        if (line.startsWith("#")) {//we ignore this since batch lines might use it as comments

        } else if (handler.isInteractive()) {
            shell.out().println("Command not found: " + Parser.findFirstWord(line));
        } else {
            handler.onValidationFailure(CommandResult.FAILURE,
                    new RuntimeException("Command not found: " + Parser.findFirstWord(line)));
        }
    }
}
