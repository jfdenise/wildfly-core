/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.test.integration.management.cli;

import org.apache.commons.lang.StringUtils;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class ForLoopTestCase extends AbstractCliTestBase {

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testSimpleFor() throws Exception {
        addProperty("prop1", "prop1_a");
        addProperty("prop2", "prop2_a");
        try {
            cli.sendLine("set var=+");
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("set var=$var+$propName");
            cli.sendLine("done");
            cli.sendLine("echo $var");
            String line = cli.readOutput();
            assertTrue(line, line.equals("++prop1+prop2"));
        } finally {
            cli.sendLine("set var=");
            removeProperties();
        }
    }

    @Test
    public void testDiscard() throws Exception {
        addProperty("prop1", "prop1_a");
        addProperty("prop2", "prop2_a");
        try {
            cli.sendLine("set var=+");
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("set var=$var+$propName");
            cli.sendLine("done --discard");
            cli.sendLine("echo $var");
            String line = cli.readOutput();
            assertTrue(line, line.equals("+"));
        } finally {
            cli.sendLine("set var=");
            removeProperties();
        }
    }

    @Test
    public void testNestedDiscard() throws Exception {
        addProperty("prop1", "prop1_a");
        addProperty("prop2", "prop2_a");
        try {
            cli.sendLine("set var=+");
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("for propName2 in :read-children-names(child-type=system-property");
            cli.sendLine("set var=$var+$propName");
            cli.sendLine("done --discard");
            cli.sendLine("done --discard");
            cli.sendLine("echo $var");
            String line = cli.readOutput();
            assertTrue(line, line.equals("+"));
        } finally {
            cli.sendLine("set var=");
            removeProperties();
        }
    }

    @Test
    public void testVarAlreadyExists() throws Exception {
        cli.sendLine("set var=+");
        try {
            assertFalse(cli.sendLine("for var in :read-children-names(child-type=system-property", true));
        } finally {
            cli.sendLine("set var=");
        }
    }

    @Test
    public void testNestedVarAlreadyExists() throws Exception {
        cli.sendLine("for var in :read-children-names(child-type=system-property", true);
        try {
            assertFalse(cli.sendLine("for var in :read-children-names(child-type=system-property", true));
        } finally {
            cli.sendLine("done --discard");
        }
    }

    @Test
    public void testNestedOp() throws Exception {
        addProperty("p1", "prop1_a");
        addProperty("p2", "prop2_a");
        try {
            cli.sendLine("for var in :read-children-names(child-type=system-property", true);
            // Shouldn't break workflow
            assertFalse(cli.sendLine("for var in :read-children-names(child-type=system-property", true));
            try {
                cli.sendLine("for var2 in :read-children-names(child-type=system-property", true);
                try {
                    cli.sendLine("echo $var-$var2");
                } finally {
                    cli.sendLine("done");
                }
            } finally {
                cli.sendLine("done");
            }
            String output = cli.readOutput();
            assertTrue(output, output.contains("p1-p1"));
            assertTrue(output, output.contains("p1-p2"));
            assertTrue(output, output.contains("p2-p1"));
            assertTrue(output, output.contains("p2-p2"));
            // No more for, this one should fail
            assertFalse(cli.sendLine("done", true));
        } finally {
            removeProperties();
        }
    }

    @Test
    public void testInvalidOps() throws Exception {
        assertFalse(cli.sendLine("for var in :read-wrong", true));
        assertFalse(cli.sendLine("for var in", true));
        assertFalse(cli.sendLine("for var", true));
        assertFalse(cli.sendLine("for", true));
    }

    @Test
    public void testNestedInvalidOps() throws Exception {
        assertTrue(cli.sendLine("for var in :read-resource", true));
        try {
            assertFalse(cli.sendLine("for var in", true));
            assertFalse(cli.sendLine("for var", true));
            assertFalse(cli.sendLine("for", true));
        } finally {
            cli.sendLine("done --discard");
        }
    }

    private void removeProperties() {
        cli.sendLine("for propName in :read-children-names(child-type=system-property");
        cli.sendLine("/system-property=$propName:remove");
        cli.sendLine("done");
        checkEmpty();
    }

    private void checkEmpty() {
        cli.sendLine("if (result==[]) of :read-children-names(child-type=system-property");
        cli.sendLine("echo EMPTY");
        cli.sendLine("else");
        cli.sendLine("echo FAILURE");
        cli.sendLine("end-if");
        String line = cli.readOutput();
        assertTrue(line, line.contains("EMPTY") && !line.contains("FAILURE"));
    }

    private void checkNonEmpty() {
        cli.sendLine("if (result!=[]) of :read-children-names(child-type=system-property");
        cli.sendLine("echo EMPTY");
        cli.sendLine("else");
        cli.sendLine("echo FAILURE");
        cli.sendLine("end-if");
        String line = cli.readOutput();
        assertTrue(line, line.contains("EMPTY") && !line.contains("FAILURE"));
    }

    @Test
    public void testForNobatch() throws Exception {
        cli.sendLine("batch");
        try {
            assertFalse(cli.sendLine("for propName in :read-children-names(child-type=system-property", true));
            String line = cli.readOutput();
            assertTrue(line, line.contains("The command is not available in the current context"));
        } finally {
            cli.sendLine("discard-batch");
        }

    }

    @Test
    public void testNestedForNobatch() throws Exception {
        try {
            cli.sendLine("for propName in :read-children-names(child-type=system-property", true);
            cli.sendLine("batch");
            assertFalse(cli.sendLine("for propName in :read-children-names(child-type=system-property", true));
        } finally {
            cli.sendLine("discard-batch");
            cli.sendLine("done --discard");
        }
    }

    @Test
    public void testForBatch() throws Exception {
        addProperty("prop1", "prop1_a");
        addProperty("prop2", "prop2_a");
        try {
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("batch");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("run-batch");
            cli.sendLine("done");
            checkEmpty();
        } finally {
            removeProperties();
        }
    }

    @Test
    public void testNestedForBatch() throws Exception {
        addProperty("prop1", "prop1_a");
        addProperty("prop2", "prop1_a");
        try {
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("for propName2 in :read-children-names(child-type=system-property");
            cli.sendLine("for propName3 in :read-children-names(child-type=system-property");
            cli.sendLine("batch");
            cli.sendLine("/system-property=prop1:write-attribute(name=value, value=$propName-$propName2-$propName3)");
            cli.sendLine("run-batch --verbose");
            cli.sendLine("done");
            cli.sendLine("done");
            cli.sendLine("done");
            cli.sendLine("set var=`/system-property=prop1:read-attribute(name=value)`");
            cli.sendLine("echo $var");
            String output=cli.readOutput();
            assertTrue(output, output.equals("prop2-prop2-prop2"));
        } finally {
            cli.sendLine("set var=");
            removeProperties();
        }
    }

    @Test
    public void testForIf() throws Exception {
        addProperty("prop1", "prop1_a");
        addProperty("prop2", "prop1_a");
        addProperty("prop3", "prop1_a");
        addProperty("prop4", "prop1_a");
        addProperty("prop5", "prop1_a");
        try {
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("if (result!=[]) of :read-children-names(child-type=system-property");
            cli.sendLine("set foo=bar");
            cli.sendLine("echo $foo");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("end-if");
            cli.sendLine("done");
            checkEmpty();
        } finally {
            removeProperties();
        }
    }

    @Test
    public void testIfFor() throws Exception {
        try {
            cli.sendLine("if (result==[]) of :read-children-names(child-type=system-property");
            cli.sendLine("for propName in :read-children-names(child-type=subsystem)");
            cli.sendLine("/system-property=$propName:add(value=$propName)");
            cli.sendLine("done");
            cli.sendLine("end-if");
            checkNonEmpty();
        } finally {
            removeProperties();
        }
    }

    @Test
    public void testForTry() throws Exception {
        try {
            cli.sendLine("for propName in :read-children-names(child-type=subsystem)");
            cli.sendLine("try");
            cli.sendLine("/system-property=$propName:add(value=$propName)");
            cli.sendLine("catch");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("end-try");
            cli.sendLine("done");

            checkNonEmpty();

            cli.sendLine("for propName in :read-children-names(child-type=subsystem)");
            cli.sendLine("try");
            cli.sendLine("/system-property=$propName:add(value=$propName)");
            cli.sendLine("catch");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("end-try");
            cli.sendLine("done");

            checkEmpty();
        } finally {
            removeProperties();
        }
    }

    @Test
    public void testTryFor() throws Exception {
        try {
            addProperty("prop1", "prop1_a");
            addProperty("prop2", "prop1_a");
            addProperty("prop3", "prop1_a");
            addProperty("prop4", "prop1_a");
            addProperty("prop5", "prop1_a");

            cli.sendLine("try");
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("done");
            cli.sendLine("catch");
            cli.sendLine("echo FAILURE");
            cli.sendLine("end-try");

            String line = cli.readOutput();
            assertTrue(line, line.contains("success") && line.contains("FAILURE"));

            cli.sendLine("try");
            cli.sendLine("for propName in :read-children-names(child-type=system-property");
            cli.sendLine("/system-property=$propName:remove");
            cli.sendLine("done");
            cli.sendLine("catch");
            cli.sendLine("echo FAILURE");
            cli.sendLine("end-try");

            line = cli.readOutput();
            assertEquals(line, 4, StringUtils.countMatches(line, "success"));

        } finally {
            removeProperties();
        }
    }

    @Test
    public void testVarVisibility() {
        cli.sendLine("for propName in :read-children-names(child-type=system-property");
        cli.sendLine("echo $propName");
        cli.sendLine("done");
        assertFalse(cli.sendLine("echo $propName", true));
        String line = cli.readOutput();
        assertEquals("Unrecognized variable propName", line.trim());
    }

    @Test
    public void testNestedVarVisibility() {
        cli.sendLine("for propName in :read-children-names(child-type=system-property");
        cli.sendLine("for propName2 in :read-children-names(child-type=system-property");
        cli.sendLine("echo $propName2");
        cli.sendLine("done");
        cli.sendLine("done");
        assertFalse(cli.sendLine("echo $propName", true));
        String line = cli.readOutput();
        assertEquals("Unrecognized variable propName", line.trim());
        assertFalse(cli.sendLine("echo $propName2", true));
        line = cli.readOutput();
        assertEquals("Unrecognized variable propName2", line.trim());
    }

    @Test
    public void testNonIterableResult() {
        cli.sendLine("set name=World");

        try {
            assertFalse(cli.sendLine("for var in :resolve-expression(expression=Hello $name!)", true));
            String line = cli.readOutput();
            assertEquals("for cannot be used with operations that produce a non-iterable result", line.trim());
        } finally {
            cli.sendLine("set name=");
        }

    }

    private void addProperty(String name, String value) {
        cli.sendLine("/system-property=" + name + ":add(value=" + value + ")");
    }
}
