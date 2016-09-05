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
package org.jboss.as.cli.command.ifelse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.handlers.ifelse.Operation;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CommandRedirection;

/**
 *
 * @author jfdenise
 */
public class IfElseRedirection extends CommandRedirection {

    private static final String CTX_KEY = "IF";

    private final Operation ifCondition;
    private final ModelNode ifRequest;
    private List<String> ifBlock;
    private List<String> elseBlock;
    private boolean inElse;

    IfElseRedirection(CliCommandContext ctx, Operation ifCondition, String ifRequest) throws CommandLineException {
        if (ifCondition == null) {
            throw new IllegalArgumentException("Condition is null");
        }
        if (ifRequest == null) {
            throw new IllegalArgumentException("Condition request is null");
        }
        this.ifCondition = ifCondition;
        this.ifRequest = ctx.getLegacyCommandContext().buildRequest(ifRequest);
    }

    @Override
    public List<String> getRedirectionCommands() {
        return Arrays.asList("if", "else", "end-if");
    }

    @Override
    public void addCommand(CliCommandContext ctx, AeshLine line) {
        addLine(line.getOriginalInput());
    }

    void run(CliCommandContext ctx) throws CommandException, CommandLineException {

        try {
            final ModelControllerClient client = ctx.getModelControllerClient();
            if (client == null) {
                throw new CommandException("The connection to the controller has not been established.");
            }

            ModelNode targetValue;
            try {
                targetValue = ctx.execute(ifRequest, "if condition");
            } catch (IOException e) {
                throw new CommandException("condition request failed", e);
            }
            final Object value = ifCondition.resolveValue(ctx.getLegacyCommandContext(), targetValue);
            if (value == null) {
                throw new CommandException("if expression resolved to a null");
            }

            getRegistration().unregister();

            if (Boolean.TRUE.equals(value)) {
                executeBlock(ctx, ifBlock, "if");
            } else if (inElse) {
                executeBlock(ctx, elseBlock, "else");
            }
        } finally {
            if (getRegistration().isActive()) {
                getRegistration().unregister();
            }
        }
    }

    boolean isInIf() {
        return !inElse;
    }

    void moveToElse() {
        this.inElse = true;
    }

    private void executeBlock(CliCommandContext ctx, List<String> block, String blockName) throws CommandException {

        if (block != null && !block.isEmpty()) {
            final BatchManager batchManager = ctx.getLegacyCommandContext().getBatchManager();
            // this is to discard a batch started by the block in case the block fails
            // as the cli remains in the batch mode in case run-batch resulted in an error
            final boolean discardActivatedBatch = !batchManager.isBatchActive();
            try {
                for (String l : block) {
                    ctx.executeCommand(l);
                }
            } finally {
                if (discardActivatedBatch && batchManager.isBatchActive()) {
                    batchManager.discardActiveBatch();
                }
            }
        }
    }

    private void addLine(String line) {
        if (inElse) {
            if (elseBlock == null) {
                elseBlock = new ArrayList<>();
            }
            elseBlock.add(line);
        } else {
            if (ifBlock == null) {
                ifBlock = new ArrayList<>();
            }
            ifBlock.add(line);
        }
    }
}
