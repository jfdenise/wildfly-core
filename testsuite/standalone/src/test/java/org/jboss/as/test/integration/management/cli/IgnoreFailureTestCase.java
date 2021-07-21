/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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


import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.cli.ifelse.CLISystemPropertyTestBase;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 *
 * @author jfdenise
 */
@RunWith(WildflyTestRunner.class)
public class IgnoreFailureTestCase extends CLISystemPropertyTestBase {

    @Test
    public void testIgnoreDuplicatedProperty() throws Exception {
        cliOut.reset();
        final CommandContext ctx = CLITestUtil.getCommandContext(cliOut);
        try {
            ctx.connectController();
            ctx.handle(getAddPropertyReq("foo", "val1"));
            ctx.handle(getAddPropertyReq("foo", "val1", true));
            ctx.handle(getAddPropertyReq("foo2", "val2"));
            ctx.handle(getAddPropertyReq("foo2", "val2", true));
            ctx.handle(getAddPropertyReq("foo3", "val3", true));
            ctx.handle(getRemovePropertyReq("foo3"));
            ctx.handle("/a=b:read(foo=bar)?");
        } finally {
            ctx.handleSafe(getRemovePropertyReq("foo"));
            ctx.handleSafe(getRemovePropertyReq("foo2"));
            ctx.handleSafe(getRemovePropertyReq("foo3"));
            ctx.terminateSession();
            cliOut.reset();
        }
    }
}
