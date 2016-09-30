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
package org.jboss.as.cli.command.legacy;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.ConnectionInfo;
import org.jboss.as.cli.ControllerAddress;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.NodePathFormatter;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class CommandContextWrapper implements CommandContext {

    private final CommandContext ctx;
    private final ParsedCommandLine parser;

    public CommandContextWrapper(CommandContext ctx, ParsedCommandLine parser) {
        this.ctx = ctx;
        this.parser = parser;
    }

    @Override
    public CliConfig getConfig() {
        return ctx.getConfig();
    }

    @Override
    public String getArgumentsString() {
        // XXX JFDENISE, this could be buggy outside operation usage.
        return ctx.getArgumentsString();
    }

    @Override
    public ParsedCommandLine getParsedCommandLine() {
        return parser;
    }

    @Override
    public void printLine(String message) {
        ctx.printLine(message);
    }

    @Override
    public void printColumns(Collection<String> col) {
        ctx.printColumns(col);
    }

    @Override
    public void clearScreen() {
        ctx.clearScreen();
    }

    @Override
    public void terminateSession() {
        ctx.terminateSession();
    }

    @Override
    public boolean isTerminated() {
        return ctx.isTerminated();
    }

    @Override
    public void set(CommandContext.Scope scope, String key, Object value) {
        ctx.set(scope, key, value);
    }

    @Override
    public Object get(CommandContext.Scope scope, String key) {
        return ctx.get(scope, key);
    }

    @Override
    public void clear(CommandContext.Scope scope) {
        ctx.clear(scope);
    }

    @Override
    public Object remove(CommandContext.Scope scope, String key) {
        return ctx.remove(scope, key);
    }

    @Override
    public ModelControllerClient getModelControllerClient() {
        return ctx.getModelControllerClient();
    }

    @Override
    public void connectController() throws CommandLineException {
        ctx.connectController();
    }

    @Override
    public void connectController(String controller) throws CommandLineException {
        ctx.connectController();
    }

    @Override
    public void connectController(String host, int port) throws CommandLineException {
        ctx.connectController(host, port);
    }

    @Override
    public void bindClient(ModelControllerClient newClient) {
        ctx.bindClient(newClient);
    }

    @Override
    public void disconnectController() {
        ctx.disconnectController();
    }

    @Override
    public String getDefaultControllerHost() {
        return ctx.getDefaultControllerHost();
    }

    @Override
    public int getDefaultControllerPort() {
        return ctx.getDefaultControllerPort();
    }

    @Override
    public ControllerAddress getDefaultControllerAddress() {
        return ctx.getDefaultControllerAddress();
    }

    @Override
    public String getControllerHost() {
        return ctx.getControllerHost();
    }

    @Override
    public int getControllerPort() {
        return ctx.getControllerPort();
    }

    @Override
    public CommandLineParser getCommandLineParser() {
        return ctx.getCommandLineParser();
    }

    @Override
    public OperationRequestAddress getCurrentNodePath() {
        return ctx.getCurrentNodePath();
    }

    @Override
    public NodePathFormatter getNodePathFormatter() {
        return ctx.getNodePathFormatter();
    }

    @Override
    public OperationCandidatesProvider getOperationCandidatesProvider() {
        return ctx.getOperationCandidatesProvider();
    }

    @Override
    public CommandHistory getHistory() {
        return ctx.getHistory();
    }

    @Override
    public boolean isBatchMode() {
        return ctx.isBatchMode();
    }

    @Override
    public boolean isWorkflowMode() {
        return ctx.isWorkflowMode();
    }

    @Override
    public BatchManager getBatchManager() {
        return ctx.getBatchManager();
    }

    @Override
    public BatchedCommand toBatchedCommand(String line) throws CommandFormatException {
        return ctx.toBatchedCommand(line);
    }

    @Override
    public ModelNode buildRequest(String line) throws CommandFormatException {
        return ctx.buildRequest(line);
    }

    @Override
    public CommandLineCompleter getDefaultCommandCompleter() {
        return ctx.getDefaultCommandCompleter();
    }

    @Override
    public boolean isDomainMode() {
        return ctx.isDomainMode();
    }

    @Override
    public void addEventListener(CliEventListener listener) {
        ctx.addEventListener(listener);
    }

    @Override
    public int getExitCode() {
        return ctx.getExitCode();
    }

    @Override
    public void handle(String line) throws CommandLineException {
        ctx.handle(line);
    }

    @Override
    public void handleSafe(String line) {
        ctx.handleSafe(line);
    }

    @Override
    public void interact() {
        ctx.interact();
    }

    @Override
    public File getCurrentDir() {
        return ctx.getCurrentDir();
    }

    @Override
    public void setCurrentDir(File dir) {
        ctx.setCurrentDir(dir);
    }

    @Override
    public boolean isResolveParameterValues() {
        return ctx.isResolveParameterValues();
    }

    @Override
    public void setResolveParameterValues(boolean resolve) {
        ctx.setResolveParameterValues(resolve);
    }

    @Override
    public boolean isSilent() {
        return ctx.isSilent();
    }

    @Override
    public void setSilent(boolean silent) {
        ctx.setSilent(silent);
    }

    @Override
    public int getTerminalWidth() {
        return ctx.getTerminalWidth();
    }

    @Override
    public int getTerminalHeight() {
        return ctx.getTerminalHeight();
    }

    @Override
    public void setVariable(String name, String value) throws CommandLineException {
        ctx.setVariable(name, value);
    }

    @Override
    public String getVariable(String name) {
        return ctx.getVariable(name);
    }

    @Override
    public Collection<String> getVariables() {
        return ctx.getVariables();
    }

    @Override
    public void registerRedirection(CommandLineRedirection redirection) throws CommandLineException {
        ctx.registerRedirection(redirection);
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return ctx.getConnectionInfo();
    }

    @Override
    public void captureOutput(PrintStream captor) {
        ctx.captureOutput(captor);
    }

    @Override
    public void releaseOutput() {
        ctx.releaseOutput();
    }

    @Override
    public void setCommandTimeout(int numSeconds) {
        ctx.setCommandTimeout(numSeconds);
    }

    @Override
    public int getCommandTimeout() {
        return ctx.getCommandTimeout();
    }

    @Override
    public void resetTimeout(CommandContext.TIMEOUT_RESET_VALUE value) {
        ctx.resetTimeout(value);
    }

    @Override
    public ModelNode execute(ModelNode mn, String description) throws CommandLineException, IOException {
        return ctx.execute(mn, description);
    }

    @Override
    public ModelNode execute(Operation op, String description) throws CommandLineException, IOException {
        return ctx.execute(op, description);
    }

}
