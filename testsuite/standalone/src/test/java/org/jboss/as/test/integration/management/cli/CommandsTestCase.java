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
package org.jboss.as.test.integration.management.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class CommandsTestCase {

    @Test
    public void recordTest() throws IOException {
        CliProcessWrapper cli = new CliProcessWrapper();

        try {
            cli.executeInteractive();
            cli.pushLineAndWaitForResults("set varName=toto");
            cli.pushLineAndWaitForResults("commands record", "[disconnected /REC]");
            cli.clearOutput();
            cli.pushLineAndWaitForResults("echo $varName");
            cli.pushLineAndWaitForResults("commands list-recorded");
            assertTrue(cli.getOutput(), cli.getOutput().contains("echo $varName"));
            cli.pushLineAndWaitForResults("commands stop-record --store=cmd1 --transient", "[disconnected /]");
            cli.clearOutput();
            cli.pushLineAndWaitForResults("cmd1");
            assertTrue(cli.getOutput(), cli.getOutput().contains("toto"));
        } finally {
            cli.destroyProcess();
        }
    }

    @Test
    public void recordTest2() throws Exception {
        CliProcessWrapper cli = new CliProcessWrapper();
        File f = new File(TestSuiteEnvironment.getJBossHome() + File.separator + "bin/cli-stored-commands.xml");
        File copy = new File(TestSuiteEnvironment.getTmpDir(), "cli-stored-commands.xml");
        if (copy.exists()) {
            copy.delete();
        }
        copy.deleteOnExit();
        Files.copy(f.toPath(), copy.toPath());
        List<String> lines = Files.readAllLines(f.toPath());
        for (String line : lines) {
            if (line.contains("cmd1")) {
                throw new Exception("Invalid command cmd1");
            }
        }
        try {
            cli.executeInteractive();
            cli.pushLineAndWaitForResults("commands record");
            cli.pushLineAndWaitForResults("set varName=titi");
            cli.pushLineAndWaitForResults("echo $varName");
            cli.pushLineAndWaitForResults("commands stop-record --store=cmd1");
        } finally {
            cli.destroyProcess();
        }

        boolean found = false;
        lines = Files.readAllLines(f.toPath());
        for (String line : lines) {
            if (line.contains("cmd1")) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        cli = new CliProcessWrapper();

        try {
            cli.executeInteractive();
            cli.pushLineAndWaitForResults("cmd1");
            assertTrue(cli.getOutput(), cli.getOutput().contains("titi"));
        } finally {
            cli.destroyProcess();
        }

        // CLI can't persist with no stored commands xml file
        Files.delete(f.toPath());
        try {
            cli = new CliProcessWrapper();
            try {
                cli.executeInteractive();
                cli.pushLineAndWaitForResults("commands record");
                cli.pushLineAndWaitForResults("echo varName");
                cli.pushLineAndWaitForResults("commands stop-record --store=cmd1");
                assertTrue(cli.getOutput(), cli.getOutput().contains("CommandLineException")
                        && cli.getOutput().contains("cli-stored-commands.xml"));
            } finally {
                cli.destroyProcess();
            }
        } finally {
            Files.copy(copy.toPath(), f.toPath());
        }
    }

    @Test
    public void recordTest3() throws IOException {
        CliProcessWrapper cli = new CliProcessWrapper();

        try {
            cli.executeInteractive();
            cli.pushLineAndWaitForResults("commands record");
            cli.pushLineAndWaitForResults("echo toto");
            cli.pushLineAndWaitForResults("commands rewind");
            cli.pushLineAndWaitForResults("commands stop-record --store=cmd1 --transient");
            cli.clearOutput();
            cli.pushLineAndWaitForResults("cmd1");
            assertFalse(cli.getOutput(), cli.getOutput().contains("titi"));
        } finally {
            cli.destroyProcess();
        }
    }
}
