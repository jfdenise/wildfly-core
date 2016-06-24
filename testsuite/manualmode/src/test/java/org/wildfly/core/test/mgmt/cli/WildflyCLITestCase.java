/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.core.test.mgmt.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.cli.WildFlyCLI;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.wildfly.core.cli.WildFlyCLI.Result;
import org.wildfly.core.cli.WildFlyCLIConfiguration;

/**
 * Test various connection states of the WildflyCLI class.
 *
 * @author Jean-Francois Denise (jdenise@redhat.com)
 */
// Unignore it when https://issues.jboss.org/browse/AESH-367 is fixed
@Ignore
@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class WildflyCLITestCase {

    private static final File ROOT = new File(System.getProperty("jboss.home"));
    private static final String JBOSS_HOME = " --jboss-home="
            + ROOT.getAbsolutePath();

    @Inject
    private ServerController serverController;

    @Test
    public void testConnectStatus() {

        // Offline instance
        WildFlyCLI cli = new WildFlyCLI();

        try {
            copyConfig("standalone.xml", "standalone-cli.xml", true);
        } catch (IOException ex) {
            Assert.fail("Exception copying configuration: " + ex);
        }

        // start an embedded server
        executeCommand(cli, "embed-server --std-out=echo "
                + "--server-config=standalone-cli.xml" + JBOSS_HOME);
        try {
            // Enable management
            executeCommand(cli, "reload --admin-only=false");

            // Start a clean CLI
            WildFlyCLIConfiguration.Builder builder = new WildFlyCLIConfiguration.Builder();
            builder.setController("http-remoting://" + TestSuiteEnvironment.getServerAddress() + ":123");
            WildFlyCLI cli2 = new WildFlyCLI(builder);

            // Make an invalid connect
            checkFail(() -> cli2.execute("connect"));

            // Make a valid connect
            WildFlyCLI cli3 = new WildFlyCLI();
            try {
                executeCommand(cli3, "version");
            } finally {
                cli3.terminate();
            }
        } finally {
            cli.execute("stop-embedded-server");
        }
    }

        @Test
    public void testTerminate() {
        WildFlyCLI cli = new WildFlyCLI();
        cli.terminate();
        cli.terminate();

        cli.execute("version");

        cli.terminate();
        checkFail(() -> cli.execute("connect"));
    }

    @Test
    public void testBatch() {
        serverController.start();
        try {
            WildFlyCLIConfiguration.Builder builder = new WildFlyCLIConfiguration.Builder();
            builder.setController("http-remoting://" + serverController.getClient().getMgmtAddress() + ":" + serverController.getClient().getMgmtPort());
            WildFlyCLI cli = new WildFlyCLI(builder);
            cli.execute("connect");
            addProperty(cli, "prop1", "prop1_a");
            addProperty(cli, "prop2", "prop2_a");

            cli.execute("batch");
            writeProperty(cli, "prop1", "prop1_b");
            writeProperty(cli, "prop2", "prop2_b");
            cli.execute("run-batch");
            assertEquals("prop1_b", readProperty(cli, "prop1"));
            assertEquals("prop2_b", readProperty(cli, "prop2"));
        } finally {
            serverController.stop();
        }
    }

    private static void checkFail(Runnable runner) {
        boolean failed = false;
        try {
            runner.run();
        } catch (RuntimeException ex) {
            failed = true;
        }
        if (!failed) {
            Assert.fail("Should have failed");
        }
    }

    private static void executeCommand(WildFlyCLI cli, String cmd) {
        Result res = cli.execute(cmd);
        if (!res.isSuccess()) {
            Assert.fail("Invalid response " + res.getResponse().asString());
        }
    }

    private static void addProperty(WildFlyCLI cli, String name, String value) {
        cli.execute("/system-property=" + name + ":add(value=" + value + ")");
    }

    private static String readProperty(WildFlyCLI cli, String name) {
        Result res = cli.execute("/system-property=" + name
                + ":read-attribute(name=value)");
        return res.getResponse().get("result").asString();
    }

    private static void writeProperty(WildFlyCLI cli, String name, String value) {
        cli.execute("/system-property=" + name
                + ":write-attribute(name=value,value=" + value + ")");
    }

    private static void copyConfig(String base, String newName,
            boolean requiresExists) throws IOException {
        File configDir = new File(ROOT, "standalone" + File.separatorChar
                + "configuration");
        File baseFile = new File(configDir, base);
        assertTrue(!requiresExists || baseFile.exists());
        File newFile = new File(configDir, newName);
        Files.copy(baseFile.toPath(), newFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
    }

}
