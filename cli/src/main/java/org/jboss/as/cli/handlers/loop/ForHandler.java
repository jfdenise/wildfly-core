/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.handlers.loop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.impl.DefaultCompleter.CandidatesProvider;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 *
 * @author jfdenise
 */
public class ForHandler extends CommandHandlerWithHelp {

    private final ArgumentWithValue varName;
    private final ArgumentWithValue in;

    public ForHandler() {
        super("for", true);

        varName = new ArgumentWithValue(this, 0, "--var");
        varName.addCantAppearAfter(helpArg);

        in = new ArgumentWithValue(this, new DefaultCompleter(new CandidatesProvider() {
            @Override
            public Collection<String> getAllCandidates(CommandContext ctx) {
                return Collections.singletonList("in");
            }
        }), 1, "--in");
        in.addRequiredPreceding(varName);

        final ArgumentWithValue line = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                final ParsedCommandLine args = ctx.getParsedCommandLine();
                final String lnStr = in.getValue(args);
                if (lnStr == null) {
                    return -1;
                }

                final String originalLine = args.getOriginalLine();
                String varNameStr;
                try {
                    varNameStr = varName.getValue(args, true);
                } catch (CommandFormatException e) {
                    return -1;
                }
                int i = originalLine.indexOf(varNameStr);
                if (i < 0) {
                    return -1;
                }
                i = originalLine.indexOf("in ", i + varNameStr.length());
                if (i < 0) {
                    return -1;
                }

                final String cmd = originalLine.substring(i + 3);
                /*                    final DefaultCallbackHandler parsedLine = new DefaultCallbackHandler();
                    try {
                        parsedLine.parse(ctx.getCurrentNodePath(), cmd);
                    } catch (CommandFormatException e) {
                        return -1;
                    }
                    int cmdResult = OperationRequestCompleter.INSTANCE.complete(ctx, parsedLine, cmd, 0, candidates);
                 */
                int cmdResult = ctx.getDefaultCommandCompleter().complete(ctx, cmd, cmd.length(), candidates);
                if (cmdResult < 0) {
                    return cmdResult;
                }

                // escaping index correction
                int escapeCorrection = 0;
                int start = originalLine.length() - 1 - buffer.length();
                while (start - escapeCorrection >= 0) {
                    final char ch = originalLine.charAt(start - escapeCorrection);
                    if (Character.isWhitespace(ch) || ch == '=') {
                        break;
                    }
                    ++escapeCorrection;
                }

                return buffer.length() + escapeCorrection - (cmd.length() - cmdResult);
            }
        }, Integer.MAX_VALUE, "--line") {
        };
        line.addRequiredPreceding(in);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return ForControlFlow.get(ctx) == null && !ctx.getBatchManager().isBatchActive();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        String argsStr = ctx.getArgumentsString();
        if (argsStr == null) {
            throw new CommandFormatException("The command is missing arguments.");
        }

        final BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            throw new CommandFormatException("for is not allowed while in batch mode.");
        }

        final ParsedCommandLine args = ctx.getParsedCommandLine();
        final String varName = this.varName.getOriginalValue(args, true);
        int i = argsStr.indexOf(varName);
        if (i < 0) {
            throw new CommandFormatException("Failed to locate '" + varName + "' in '" + argsStr + "'");
        }
        i = argsStr.indexOf("in", i + varName.length());
        if (i < 0) {
            throw new CommandFormatException("Failed to locate 'in' in '" + argsStr + "'");
        }
        final String requestStr = argsStr.substring(i + 2);
        ctx.registerRedirection(new ForControlFlow(ctx, varName, requestStr));
    }

    /**
     * It has to accept everything since we don't know what kind of command will
     * be edited.
     */
    @Override
    public boolean hasArgument(CommandContext ctx, int index) {
        return true;
    }

    @Override
    public boolean hasArgument(CommandContext ctx, String name) {
        return true;
    }
}
