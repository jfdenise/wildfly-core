/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
 *
 */
package org.jboss.as.cli.impl.aesh;

import java.lang.reflect.Method;
import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.util.Config;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class HelpSupportTestCase {

    private static final AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();

    @Test
    public void testStandalone() throws Exception {
        for (Class<? extends Command> clazz : Commands.TESTS_STANDALONE) {
            testStandalone(clazz);
        }
    }

    @Test
    public void testCLICommands() throws Exception {
        CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        Method m = ctx.getClass().getMethod("getAeshCommands");
        m.setAccessible(true);
        AeshCommands commands = (AeshCommands) m.invoke(ctx);
        for (String name : commands.getRegistry().getExposedCommands()) {
            CommandContainer<Command> container = commands.getRegistry().getCommand(name, name);
            testCLICommand(null, container.getParser());
            for (CommandLineParser<Command> child : container.getParser().getAllChildParsers()) {
                testCLICommand(container.getParser(), child);
            }
        }
    }

    private static void testCLICommand(CommandLineParser<Command> parent,
            CommandLineParser<Command> child) throws Exception {
        String fullHelp = parent == null ? HelpSupport.getCommandHelp(child)
                : HelpSupport.getSubCommandHelp(parent.getProcessedCommand().name(), child);
        if (fullHelp.contains(HelpSupport.NULL_DESCRIPTION)) {

            throw new Exception("Command " + (parent == null ? ""
                    : parent.getProcessedCommand().name()) + " "
                    + child.getProcessedCommand().name() + " contains null description:\n" + fullHelp);
        }
    }

    private static void testStandalone(Class<? extends Command> clazz) throws Exception {
        Command c = clazz.newInstance();
        String synopsis = getStandaloneSynopsis(c);
        Assert.assertEquals(clazz.getName() + ". EXPECTED [" + ((TestCommand) c).getSynopsis()
                + "]. FOUND [" + synopsis + "]", ((TestCommand) c).getSynopsis(), synopsis);
    }

    private static String getStandaloneSynopsis(Command c) {
        String fullHelp = HelpSupport.getCommandHelp(containerBuilder.create(c).getParser());
        int start = ("SYNOPSIS" + Config.getLineSeparator() + Config.getLineSeparator()).length();
        int end = fullHelp.indexOf(Config.getLineSeparator() + Config.getLineSeparator() + "DESCRIPTION");
        String synopsis = fullHelp.substring(start, end).trim();
        String[] lines = synopsis.split("\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(" " + line.trim());
        }
        return builder.toString().trim();
    }
}
