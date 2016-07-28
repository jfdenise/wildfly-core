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
package org.jboss.as.cli.command.trycatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.controller.client.ModelControllerClient;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CommandRedirection;

/**
 * XXX JFDENISE The exception that occurs are displayed twice, once when
 * executing the failing command (Aesh execute) once when the exception is
 * rethrown and caught. Aesh should move the exception printing one layer ubove.
 *
 * @author jfdenise
 */
class TryCatchFinallyRedirection extends CommandRedirection {
    private static final String CTX_KEY = "TRY";

    private static final int IN_TRY = 0;
    private static final int IN_CATCH = 1;
    private static final int IN_FINALLY = 2;

//    static TryCatchFinallyRedirection get(CommandContext ctx) {
//        return (TryCatchFinallyRedirection) ctx.get(Scope.CONTEXT, CTX_KEY);
//    }

    private List<String> tryList;
    private List<String> catchList;
    private List<String> finallyList;
    private int state;

    TryCatchFinallyRedirection(CliCommandContext ctx) {
        // ctx.getLegacyCommandContext().set(Scope.CONTEXT, CTX_KEY, this);
    }

    @Override
    public List<String> getRedirectionCommands() {
        return Arrays.asList("try", "catch", "finally", "end-try");
    }

    @Override
    public void addCommand(CliCommandContext ctx, AeshLine line) {
        addLine(line.getOriginalInput());
    }

    boolean isInTry() {
        return state == IN_TRY;
    }

    boolean isInFinally() {
        return state == IN_FINALLY;
    }

    void moveToCatch() throws CommandException {
        switch (state) {
            case IN_TRY:
                state = IN_CATCH;
                break;
            case IN_CATCH:
                throw new CommandException("Already in catch block. Only one catch block is allowed.");
            case IN_FINALLY:
                throw new CommandException("Catch block is not allowed in finally");
            default:
                throw new IllegalStateException("Unexpected block id: " + state);
        }
    }

    void moveToFinally() throws CommandLineException {
        switch (state) {
            case IN_TRY:
                state = IN_FINALLY;
                break;
            case IN_CATCH:
                state = IN_FINALLY;
                break;
            case IN_FINALLY:
                throw new CommandLineException("Already in finally");
            default:
                throw new IllegalStateException("Unexpected block id: " + state);
        }
    }

    private void addLine(String line) {
        switch (state) {
            case IN_TRY:
                if (tryList == null) {
                    tryList = new ArrayList<String>();
                }
                tryList.add(line);
                break;
            case IN_CATCH:
                if (catchList == null) {
                    catchList = new ArrayList<String>();
                }
                catchList.add(line);
                break;
            case IN_FINALLY:
                if (finallyList == null) {
                    finallyList = new ArrayList<String>();
                }
                finallyList.add(line);
                break;
            default:
                throw new IllegalStateException("Unexpected block id: " + state);
        }
    }

    void run(CliCommandContext ctx) throws CommandException {

        if (state == IN_TRY) {
            throw new CommandException("The flow can be executed only after catch or finally.");
        }

        try {
            final ModelControllerClient client = ctx.getModelControllerClient();
            if (client == null) {
                throw new CommandException("The connection to the controller has not been established.");
            }

            getRegistration().unregister();

            CommandException error = null;

            if (tryList == null || tryList.isEmpty()) {
                throw new CommandException("The try block is empty");
            }

            try {
                executeBlock(ctx, tryList, "try");
            } catch (CommandException eTry) {
                if (catchList == null) {
                    error = eTry;
                } else {
                    try {
                        executeBlock(ctx, catchList, "catch");
                    } catch (CommandException eCatch) {
                        error = eCatch;
                    }
                }
            }

            try {
                executeBlock(ctx, finallyList, "finally");
            } catch (CommandException eFinally) {
                error = eFinally;
            }

            if (error != null) {
                throw error;
            }
        } finally {
            if (getRegistration().isActive()) {
                getRegistration().unregister();
            }
            //ctx.getLegacyCommandContext().remove(Scope.CONTEXT, CTX_KEY);
        }
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
}
