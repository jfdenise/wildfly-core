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
package org.jboss.as.cli.command.compat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.util.HelpFormatter;
import org.jboss.as.protocol.StreamUtils;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author jdenise@redhat.com
 */
class Util {

    static void printLegacyHelp(CommandContext ctx, String command) throws CommandLineException {
        String filename = "help/" + command + ".txt";
        InputStream helpInput = WildFlySecurityManager.getClassLoaderPrivileged(CommandHandlerWithHelp.class).getResourceAsStream(filename);
        if (helpInput != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(helpInput));
            try {
                /*                String helpLine = reader.readLine();
                while(helpLine != null) {
                    ctx.printLine(helpLine);
                    helpLine = reader.readLine();
                }
                 */
                HelpFormatter.format(ctx, reader);
            } catch (java.io.IOException e) {
                throw new CommandFormatException("Failed to read help/help.txt: " + e.getLocalizedMessage());
            } finally {
                StreamUtils.safeClose(reader);
            }
        } else {
            throw new CommandFormatException("Failed to locate command description " + filename);
        }
    }
}
