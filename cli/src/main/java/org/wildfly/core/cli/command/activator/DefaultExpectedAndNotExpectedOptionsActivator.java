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
package org.wildfly.core.cli.command.activator;

import java.util.Set;
import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 *
 * Use this activator to make an option available if some options are already
 * present or not present.
 *
 * @author jdenise@redhat.com
 */
public abstract class DefaultExpectedAndNotExpectedOptionsActivator implements ExpectedOptionsActivator, NotExpectedOptionsActivator {
    private static class ExpectedOptionsActivatorImpl extends DefaultExpectedOptionsActivator {

        ExpectedOptionsActivatorImpl(Set<String> opts) {
            super(opts);
        }
    }

    private static class NotExpectedOptionsActivatorImpl extends DefaultNotExpectedOptionsActivator {

        NotExpectedOptionsActivatorImpl(Set<String> opts) {
            super(opts);
        }

    }
    private final ExpectedOptionsActivator expected;
    private final NotExpectedOptionsActivator notExpected;

    protected DefaultExpectedAndNotExpectedOptionsActivator(Set<String> expectedOptions, Set<String> notExpectedOptions) {
        this.expected = new ExpectedOptionsActivatorImpl(expectedOptions);
        this.notExpected = new NotExpectedOptionsActivatorImpl(notExpectedOptions);
    }

    @Override
    public boolean isActivated(ProcessedCommand processedCommand) {
        if (!expected.isActivated(processedCommand)) {
            return false;
        }
        return notExpected.isActivated(processedCommand);
    }

    @Override
    public Set<String> getNotExpected() {
        return notExpected.getNotExpected();
    }

    @Override
    public Set<String> getExpected() {
        return expected.getExpected();
    }
}
