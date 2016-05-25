/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli;

import org.jboss.as.cli.console.Console;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jfdenise
 */
public interface Context {
    void setParsedCommandLine(ParsedCommandLine line);

    /**
     * Set the current node path.
     *
     * @param address The node path
     */
    void setCurrentNodePath(OperationRequestAddress address);

    String getPrompt();

    void handleOperation(ParsedCommandLine line) throws CommandLineException;

    void connectController(String controller, Console console) throws CommandLineException;

    void addBatchOperation(ModelNode node, String input);
}
