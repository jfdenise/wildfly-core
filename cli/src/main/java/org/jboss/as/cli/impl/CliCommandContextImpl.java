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
package org.jboss.as.cli.impl;

import org.jboss.as.cli.CliCommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jfdenise
 */
// XXX JFDENISE, is public for now, will be package when moved to right package
public class CliCommandContextImpl implements CliCommandContext {
    private final CommandContextImpl context;
    public CliCommandContextImpl(CommandContextImpl context) {
        this.context = context;
    }

    @Override
    public boolean isDomainMode() {
        return context.isDomainMode();
    }

    @Override
    public void setParsedCommandLine(DefaultCallbackHandler line) {
        context.setParsedCommandLine(line);
    }

    @Override
    public void addBatchOperation(ModelNode buildRequest, String originalInput) {
        context.addBatchOperation(buildRequest, originalInput);
    }

    @Override
    public void handleOperation(DefaultCallbackHandler operationParser) throws CommandLineException {
        context.handleOperation(operationParser);
    }

    @Override
    public void connectController(String url) throws CommandLineException {
        context.connectController(url, context.getConsole());
    }

    @Override
    public void interruptConnect() {
        context.interruptConnect();
    }

    @Override
    public void exit() {
        // Exit should be enough, but Aesh Console is not properly cleaned
        // in shutdown handler.
        context.terminateSession();
        System.exit(1);
    }

}
