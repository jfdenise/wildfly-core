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
package org.jboss.as.cli.handlers;

import java.io.File;
import java.util.List;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.command.AttachmentResponseHandler;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.ArgumentWithoutValue;
import org.jboss.as.cli.impl.FileSystemPathArgument;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class AttachmentHandler extends BatchModeCommandHandler {

    private static final String DISPLAY = "display";
    private static final String SAVE = "save";

    private final FileSystemPathArgument targetFile;
    private final ArgumentWithValue operation;
    private final ArgumentWithValue action;
    private final ArgumentWithoutValue overwrite;

    public AttachmentHandler(CommandContext ctx) {
        super(ctx, "attachment", true);

        action = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor,
                    List<String> candidates) {
                if (buffer == null || buffer.isEmpty()) {
                    candidates.add(DISPLAY);
                    candidates.add(SAVE);
                    return cursor;
                }
                if (buffer.equals(DISPLAY) || buffer.equals(SAVE)) {
                    candidates.add(" ");
                    return cursor;
                }
                if (DISPLAY.startsWith(buffer)) {
                    candidates.add(DISPLAY + " ");
                    return 0;
                }
                if (SAVE.startsWith(buffer)) {
                    candidates.add(SAVE + " ");
                    return 0;
                }
                return -1;
            }
        }, 0, "--action");

        operation = new ArgumentWithValue(this, new CommandLineCompleter() {
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor,
                    List<String> candidates) {

                final String originalLine = ctx.getParsedCommandLine().getOriginalLine();
                boolean skipWS;
                int wordCount;
                if (Character.isWhitespace(originalLine.charAt(0))) {
                    skipWS = true;
                    wordCount = 0;
                } else {
                    skipWS = false;
                    wordCount = 1;
                }
                int cmdStart = 1;
                while (cmdStart < originalLine.length()) {
                    if (skipWS) {
                        if (!Character.isWhitespace(originalLine.charAt(cmdStart))) {
                            skipWS = false;
                            ++wordCount;
                            if (wordCount == 3) {
                                break;
                            }
                        }
                    } else if (Character.isWhitespace(originalLine.charAt(cmdStart))) {
                        skipWS = true;
                    }
                    ++cmdStart;
                }

                String cmd;
                if (wordCount == 1) {
                    cmd = "";
                } else if (wordCount != 3) {
                    return -1;
                } else {
                    cmd = originalLine.substring(cmdStart);
                    // remove --operation=
                    int i = cmd.indexOf("=");
                    if (i > 0) {
                        if (i == cmd.length() - 1) {
                            cmd = "";
                        } else {
                            cmd = cmd.substring(i + 1);
                        }
                    }
                }

                int cmdResult = ctx.getDefaultCommandCompleter().complete(ctx,
                        cmd, cmd.length(), candidates);
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
        }, "--operation") {

            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                String act = getAction(ctx);
                if (!(SAVE.equals(act) || DISPLAY.equals(act))) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }
        };

        operation.addRequiredPreceding(action);
        final FilenameTabCompleter pathCompleter = Util.isWindows()
                ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        targetFile = new FileSystemPathArgument(this, pathCompleter, "--file") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (!(SAVE.equals(getAction(ctx)))) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }

        };
        targetFile.addRequiredPreceding(operation);

        overwrite = new ArgumentWithoutValue(this, "--overwrite") {
            @Override
            public boolean canAppearNext(CommandContext ctx) throws CommandFormatException {
                if (!(SAVE.equals(getAction(ctx)))) {
                    return false;
                }
                return super.canAppearNext(ctx);
            }

        };
        headers.addRequiredPreceding(operation);
    }

    @Override
    protected void recognizeArguments(CommandContext ctx) throws CommandFormatException {
        String act = getAction(ctx);
        if (DISPLAY.equals(act)) {
            if (targetFile.isPresent(ctx.getParsedCommandLine())) {
                throw new CommandFormatException(targetFile.getFullName()
                        + " can't be used with display action");
            }
            if (overwrite.isPresent(ctx.getParsedCommandLine())) {
                throw new CommandFormatException(overwrite.getFullName()
                        + " can't be used with display action");
            }
        }
        super.recognizeArguments(ctx);
    }

    private String getAction(CommandContext ctx) {
        final String originalLine = ctx.getParsedCommandLine().getOriginalLine();
        if (originalLine == null || originalLine.isEmpty()) {
            return null;
        }
        String[] words = originalLine.trim().split(" ");
        String action = null;
        boolean seenFirst = false;
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            if (!w.isEmpty()) {
                if (seenFirst) {
                    action = w;
                    break;
                } else {
                    seenFirst = true;
                }
            }
        }
        return action;
    }

    @Override
    protected void handleResponse(CommandContext ctx, OperationResponse response,
            boolean composite) throws CommandLineException {
        ModelNode result = response.getResponseNode();
        String targetPath = targetFile.getValue(ctx.getParsedCommandLine());
        String act = action.getValue(ctx.getParsedCommandLine());
        if (act == null || act.isEmpty()) {
            throw new CommandFormatException("Action is missing");
        }
        try {
            new AttachmentResponseHandler((String t) -> {
                ctx.printLine(t);
            }, targetPath == null ? null : new File(targetPath),
                    act.equals(SAVE), overwrite.isPresent(ctx.getParsedCommandLine())).
                    handleResponse(result, response);
        } catch (CommandException ex) {
            throw new CommandLineException(ex);
        }
    }

    @Override
    protected ModelNode buildRequestWithoutHeaders(CommandContext ctx)
            throws CommandFormatException {
        final String op = operation.getValue(ctx.getParsedCommandLine());
        if (op == null) {
            throw new CommandFormatException("Invalid operation");
        }
        ModelNode mn = ctx.buildRequest(op);
        return mn;
    }

    @Override
    public HandledRequest buildHandledRequest(CommandContext ctx,
            Attachments attachments) throws CommandFormatException {
        String targetPath = targetFile.getValue(ctx.getParsedCommandLine());
        String act = action.getValue(ctx.getParsedCommandLine());
        if (act == null || act.isEmpty()) {
            throw new CommandFormatException("Action is missing");
        }
        ResponseHandler handler = (ModelNode step, OperationResponse response) -> {
            try {
                new AttachmentResponseHandler((String t) -> {
                    ctx.printLine(t);
                }, targetPath == null ? null : new File(targetPath),
                        act.equals(SAVE), overwrite.isPresent(ctx.getParsedCommandLine())).
                        handleResponse(step, response);
            } catch (CommandException ex) {
                throw new CommandLineException(ex);
            }
        };
        return new HandledRequest(buildRequest(ctx, attachments), handler);
    }
}
