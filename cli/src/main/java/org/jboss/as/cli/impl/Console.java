/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.impl;

import java.io.PrintStream;
import java.util.Collection;
import org.jboss.aesh.console.ConsoleCallback;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;

/**
 *
 * @author Alexey Loubyansky
 */
public interface Console {

    void addCompleter(CommandLineCompleter completer);

    boolean isUseHistory();

    CommandHistory getHistory();

    void setCompletion(boolean complete);

    void clearScreen();

    void printColumns(Collection<String> list);

    void print(String line);

    void printNewLine();

    String readLine(String prompt) throws CommandLineException;

    String readLine(String prompt, Character mask) throws CommandLineException;

    int getTerminalWidth();

    int getTerminalHeight();

    /**
     * Checks whether the tab-completion is enabled.
     *
     * @return  true if tab-completion is enabled, false - otherwise
     */
    boolean isCompletionEnabled();

    /**
     * Enables or disables the tab-completion.
     *
     * @param completionEnabled  true will enable the tab-completion, false will disable it
     */
    // void setCompletionEnabled(boolean completionEnabled);

    /**
     * Interrupts blocking readLine method.
     *
     * Added as solution to BZ-1149099.
     */
    void interrupt();

    void controlled();

    boolean isControlled();

    void continuous();

    void setCallback(ConsoleCallback consoleCallback);

    void start();

    void stop();

    boolean running();

    void setPrompt(String prompt);

    void setPrompt(String prompt, Character mask);

    void redrawPrompt();

    boolean isSilent();

    void setSilent(boolean silent);

    void captureOutput(PrintStream captor);

    void releaseOutput();
}
