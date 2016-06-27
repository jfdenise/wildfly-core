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
package org.jboss.as.test.integration.management.cli;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 *
 * @author jdenise@redhat.com
 */
@RunWith(WildflyTestRunner.class)
public class ExplodedDeploymentTestCase {

    private static CLIWrapper cli;

    @BeforeClass
    public static void before() throws Exception {
        cli = new CLIWrapper(true, null, System.in);
    }

    @AfterClass
    public static void after() throws Exception {
        cli.close();
    }

    @Test
    public void testAttachement() throws Exception {
        String deploymentUnit = "/deployment=test" + System.currentTimeMillis() + "-content.war";
        // Add the deployment
        cli.sendLine(deploymentUnit + ":add(empty)");
        assertTrue(cli.readOutput().contains("success"));
        try {
            File index = new File(TestSuiteEnvironment.getTmpDir(), "index.xhtml");
            index.createNewFile();
            Files.write(index.toPath(), "<html></html>".getBytes());

            File css = new File(TestSuiteEnvironment.getTmpDir(), "index.css");
            css.createNewFile();
            Files.write(css.toPath(), "p { text-align: center; }".getBytes());

            File js = new File(TestSuiteEnvironment.getTmpDir(), "index.js");
            js.createNewFile();
            Files.write(js.toPath(), "".getBytes());

            cli.sendLine(deploymentUnit + ":add-content(content=["
                    + "{input-stream-index=" + index.getAbsolutePath() + ", target-path=index.xhtml}"
                    + "{input-stream-index=" + css.getAbsolutePath() + ", target-path=css/theme.css}"
                    + "{input-stream-index=" + js.getAbsolutePath() + ", target-path=code/js/script.js}"
                    + "]");
            cli.sendLine(deploymentUnit + ":browse-content(path=./)");
            assertTrue(cli.readOutput().contains("index.xhtml"));
            assertTrue(cli.readOutput().contains("css/theme.css"));
            assertTrue(cli.readOutput().contains("code/js/script.js"));

            // Batch
            cli.sendLine("batch");

            cli.sendLine(deploymentUnit + ":add-content(content=["
                    + "{input-stream-index=" + index.getAbsolutePath() + ", target-path=batch/index.xhtml}"
                    + "{input-stream-index=" + css.getAbsolutePath() + ", target-path=batch/css/theme.css}"
                    + "{input-stream-index=" + js.getAbsolutePath() + ", target-path=batch/code/js/script.js}"
                    + "]");
            cli.sendLine(deploymentUnit + ":add-content(content=["
                    + "{input-stream-index=" + index.getAbsolutePath() + ", target-path=batch2/index.xhtml}"
                    + "{input-stream-index=" + css.getAbsolutePath() + ", target-path=batch2/css/theme.css}"
                    + "{input-stream-index=" + js.getAbsolutePath() + ", target-path=batch2/code/js/script.js}"
                    + "]");

            cli.sendLine("run-batch");
            assertTrue(cli.readOutput().contains("success"));

            cli.sendLine(deploymentUnit + ":browse-content(path=./)");
            assertTrue(cli.readOutput().contains("batch/index.xhtml"));
            assertTrue(cli.readOutput().contains("batch/css/theme.css"));
            assertTrue(cli.readOutput().contains("batch/code/js/script.js"));
            assertTrue(cli.readOutput().contains("batch2/index.xhtml"));
            assertTrue(cli.readOutput().contains("batch2/css/theme.css"));
            assertTrue(cli.readOutput().contains("batch2/code/js/script.js"));

            // if
            cli.sendLine("if (outcome == success) of " + deploymentUnit + ":read-attribute(name=name)");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + index.getAbsolutePath() + ", target-path=if/index.xhtml}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + css.getAbsolutePath() + ", target-path=if/css/theme.css}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + js.getAbsolutePath() + ", target-path=if/code/js/script.js}]");
            cli.sendLine("else");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + index.getAbsolutePath() + ", target-path=else/index.xhtml}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + css.getAbsolutePath() + ", target-path=else/css/theme.css}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + js.getAbsolutePath() + ", target-path=else/code/js/script.js}]");
            cli.sendLine("end-if");

            //try
            cli.sendLine("try");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + index.getAbsolutePath() + ", target-path=try/index.xhtml}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + css.getAbsolutePath() + ", target-path=try/css/theme.css}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + js.getAbsolutePath() + ", target-path=try/code/js/script.js}]");
            // Then fail...
            cli.sendLine(deploymentUnit + ":read-attribute(name=toto)");
            cli.sendLine("catch");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + index.getAbsolutePath() + ", target-path=catch/index.xhtml}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + css.getAbsolutePath() + ", target-path=catch/css/theme.css}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + js.getAbsolutePath() + ", target-path=catch/code/js/script.js}]");
            cli.sendLine("finally");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + index.getAbsolutePath() + ", target-path=finally/index.xhtml}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + css.getAbsolutePath() + ", target-path=finally/css/theme.css}]");
            cli.sendLine(deploymentUnit + ":add-content(content=[{input-stream-index=" + js.getAbsolutePath() + ", target-path=finally/code/js/script.js}]");
            cli.sendLine("end-try");

            // completion
            testCompletion(deploymentUnit + ":add-content(content=[{target-path=");
            testCompletion(deploymentUnit + ":add-content(content=[{target-path=toto},{target-path=");
            testCompletion(deploymentUnit + ":browse-content(path=");

            {
                String cmd = deploymentUnit + ":remove-content(paths=";
                List<String> candidates = new ArrayList<>();
                cli.getCommandContext().getDefaultCommandCompleter().complete(cli.getCommandContext(), cmd,
                        cmd.length(), candidates);
                assertTrue(candidates.toString(), candidates.contains("["));
                assertTrue(candidates.toString(), candidates.size() == 1);
            }

            {
                String cmd = deploymentUnit + ":browse-content(path=/";
                List<String> candidates = new ArrayList<>();
                cli.getCommandContext().getDefaultCommandCompleter().complete(cli.getCommandContext(), cmd,
                        cmd.length(), candidates);
                assertTrue(candidates.toString(), candidates.isEmpty());
            }

            testCompletion(deploymentUnit + ":remove-content(paths=[");

        } finally {
            cli.sendLine(deploymentUnit + ":remove");
        }
    }

    private void testCompletion(String radical) {
        CommandContext ctx = cli.getCommandContext();
        {
            String cmd = radical;
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("index.xhtml"));
            assertTrue(candidates.toString(), candidates.contains("css/"));
            assertTrue(candidates.toString(), candidates.contains("code/"));
            assertTrue(candidates.toString(), candidates.contains("batch/"));
            assertTrue(candidates.toString(), candidates.contains("batch2/"));
            assertTrue(candidates.toString(), candidates.contains("if/"));
            assertTrue(candidates.toString(), candidates.contains("try/"));
            assertTrue(candidates.toString(), candidates.contains("catch/"));
            assertTrue(candidates.toString(), candidates.contains("finally/"));
        }

        {
            String cmd = radical + ".";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("./"));
        }

        {
            String cmd = radical + "batch";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("batch/"));
            assertTrue(candidates.toString(), candidates.contains("batch2/"));
        }

        {
            String cmd = radical + "batch/";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("index.xhtml"));
            assertTrue(candidates.toString(), candidates.contains("css/"));
            assertTrue(candidates.toString(), candidates.contains("code/"));
        }

        {
            String cmd = radical + "if";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("if/"));
        }

        {
            String cmd = radical + "if/";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("index.xhtml"));
            assertTrue(candidates.toString(), candidates.contains("css/"));
            assertTrue(candidates.toString(), candidates.contains("code/"));
        }

        {
            String cmd = radical + "try/";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("index.xhtml"));
            assertTrue(candidates.toString(), candidates.contains("css/"));
            assertTrue(candidates.toString(), candidates.contains("code/"));
        }

        {
            String cmd = radical + "catch/";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("index.xhtml"));
            assertTrue(candidates.toString(), candidates.contains("css/"));
            assertTrue(candidates.toString(), candidates.contains("code/"));
        }

        {
            String cmd = radical + "finally/";
            List<String> candidates = new ArrayList<>();
            ctx.getDefaultCommandCompleter().complete(ctx, cmd,
                    cmd.length(), candidates);
            assertTrue(candidates.toString(), candidates.contains("index.xhtml"));
            assertTrue(candidates.toString(), candidates.contains("css/"));
            assertTrue(candidates.toString(), candidates.contains("code/"));
        }
    }
}
