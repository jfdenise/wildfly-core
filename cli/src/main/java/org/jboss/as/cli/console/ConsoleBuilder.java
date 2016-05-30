/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli.console;

import java.io.InputStream;
import java.io.OutputStream;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.jboss.as.cli.impl.Console;

/**
 *
 * @author jfdenise
 */
public class ConsoleBuilder {

    private boolean interactive;
    private boolean silent;
    private Boolean errorOnInteract;
    private CommandContextImpl context;
    private Settings aeshSettings;
    private InputStream consoleInput;
    private OutputStream consoleOutput;

    public ConsoleBuilder setInteractive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public ConsoleBuilder setConsoleInputStream(InputStream consoleInput) {
        this.consoleInput = consoleInput;
        return this;
    }

    public ConsoleBuilder setConsoleOutputStream(OutputStream consoleOutput) {
        this.consoleOutput = consoleOutput;
        return this;
    }

    public ConsoleBuilder setSilent(boolean silent) {
        this.silent = silent;
        return this;
    }

    public ConsoleBuilder setContext(CommandContextImpl context) {
        this.context = context;
        return this;
    }

    public ConsoleBuilder setErrorOnInteract(Boolean errorOnInteract) {
        this.errorOnInteract = errorOnInteract;
        return this;
    }

    public ConsoleBuilder setAeshSettings(Settings aeshSettings) {
        this.aeshSettings = aeshSettings;
        return this;
    }

    public Console create() throws CommandLineParserException, CommandLineException {
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }

        return new AeshCliConsole(context, silent, errorOnInteract, aeshSettings,
                consoleInput, consoleOutput);
    }

}
