/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.console;

import org.jboss.aesh.console.settings.CommandNotFoundHandler;
import org.jboss.aesh.parser.Parser;
import org.jboss.aesh.terminal.Shell;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliCommandNotFound implements CommandNotFoundHandler {

    private final boolean interactive;

    CliCommandNotFound(boolean interactive) {
        this.interactive = interactive;
    }

    @Override
    public void handleCommandNotFound(String line, Shell shell) {
        if (line.startsWith("#")) {
            //we ignore this since batch lines might use it as comments
        } else if (interactive) {
            shell.out().println("Command not found: " + Parser.findFirstWord(line));
        } else {
            throw new RuntimeException("Command not found: " + Parser.findFirstWord(line));
        }
    }
}
