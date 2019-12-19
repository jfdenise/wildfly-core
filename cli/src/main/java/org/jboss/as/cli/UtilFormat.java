/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli;

import org.aesh.readline.terminal.formatting.CharacterType;
import org.aesh.readline.terminal.formatting.Color;
import org.aesh.readline.terminal.formatting.TerminalColor;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.readline.terminal.formatting.TerminalTextStyle;
import static org.jboss.as.cli.Util.isWindows;

/**
 *
 * @author jdenise
 */
public class UtilFormat {

    private static TerminalColor ERROR_COLOR;
    private static TerminalColor SUCCESS_COLOR;
    private static TerminalColor WARN_COLOR;
    private static TerminalColor REQUIRED_COLOR;
    private static TerminalColor WORKFLOW_COLOR;
    private static TerminalColor PROMPT_COLOR;
    private static TerminalTextStyle BOLD_STYLE = new TerminalTextStyle(CharacterType.BOLD);

    public static final String formatErrorMessage(String message) {
        return new TerminalString(message, ERROR_COLOR, BOLD_STYLE).toString();
    }

    public static final String formatSuccessMessage(String message) {
        return new TerminalString(message, SUCCESS_COLOR).toString();
    }

    public static final String formatWarnMessage(String message) {
        return new TerminalString(message, WARN_COLOR, BOLD_STYLE).toString();
    }

    public static TerminalString formatRequired(TerminalString name) {
        return new TerminalString(name.toString(), REQUIRED_COLOR, BOLD_STYLE);
    }

    public static String formatWorkflowPrompt(String prompt) {
        return new TerminalString(prompt, WORKFLOW_COLOR).toString();
    }

    public static void formatPrompt(StringBuilder buffer) {
        if (buffer.toString().contains("@")) {
            int at = buffer.indexOf("@");
            int space = buffer.lastIndexOf(" ");
            String preAt = buffer.substring(1, at);
            String postAt = buffer.substring(at + 1, space + 1);
            buffer.delete(1, space + 1);
            buffer.append(
                    new TerminalString(preAt, PROMPT_COLOR).toString());
            buffer.append("@");
            buffer.append(
                    new TerminalString(postAt, PROMPT_COLOR).toString());
        } else if (buffer.toString().contains("disconnected")) {
            String prompt = buffer.substring(1);
            buffer.replace(1, buffer.lastIndexOf(" ") + 1,
                    new TerminalString(prompt, ERROR_COLOR).toString());
        } else {
            String prompt = buffer.substring(1);
            buffer.replace(1, buffer.lastIndexOf(" ") + 1,
                    new TerminalString(prompt, PROMPT_COLOR).toString());
        }
    }

    public static void configureColors(CommandContext ctx) {
        ColorConfig colorConfig = ctx.getConfig().getColorConfig();
        if (colorConfig != null) {
            ERROR_COLOR = new TerminalColor((colorConfig.getErrorColor() != null) ? colorConfig.getErrorColor() : Color.RED,
                    Color.DEFAULT, Color.Intensity.BRIGHT);

            if (!isWindows()) {
                WARN_COLOR = new TerminalColor((colorConfig.getWarnColor() != null) ? colorConfig.getWarnColor() : Color.YELLOW,
                        Color.DEFAULT, Color.Intensity.NORMAL);
            } else {
                WARN_COLOR = new TerminalColor((colorConfig.getWarnColor() != null) ? colorConfig.getWarnColor() : Color.YELLOW,
                        Color.DEFAULT, Color.Intensity.BRIGHT);
            }

            SUCCESS_COLOR = new TerminalColor(
                    (colorConfig.getSuccessColor() != null) ? colorConfig.getSuccessColor() : Color.DEFAULT, Color.DEFAULT,
                    Color.Intensity.NORMAL);
            REQUIRED_COLOR = new TerminalColor(
                    (colorConfig.getRequiredColor() != null) ? colorConfig.getRequiredColor() : Color.CYAN, Color.DEFAULT,
                    Color.Intensity.BRIGHT);
            WORKFLOW_COLOR = new TerminalColor(
                    (colorConfig.getWorkflowColor() != null) ? colorConfig.getWorkflowColor() : Color.GREEN, Color.DEFAULT,
                    Color.Intensity.BRIGHT);
            PROMPT_COLOR = new TerminalColor(
                    (colorConfig.getWorkflowColor() != null) ? colorConfig.getPromptColor() : Color.BLUE, Color.DEFAULT,
                    Color.Intensity.BRIGHT);
        } else {
            ERROR_COLOR = new TerminalColor(Color.RED, Color.DEFAULT, Color.Intensity.BRIGHT);

            if (!isWindows()) {
                WARN_COLOR = new TerminalColor(Color.YELLOW, Color.DEFAULT, Color.Intensity.NORMAL);
            } else {
                WARN_COLOR = new TerminalColor(Color.YELLOW, Color.DEFAULT, Color.Intensity.BRIGHT);
            }

            SUCCESS_COLOR = new TerminalColor(Color.DEFAULT, Color.DEFAULT, Color.Intensity.NORMAL);
            REQUIRED_COLOR = new TerminalColor(Color.MAGENTA, Color.DEFAULT, Color.Intensity.BRIGHT);
            WORKFLOW_COLOR = new TerminalColor(Color.GREEN, Color.DEFAULT, Color.Intensity.BRIGHT);
            PROMPT_COLOR = new TerminalColor(Color.BLUE, Color.DEFAULT, Color.Intensity.BRIGHT);
        }
    }
}
