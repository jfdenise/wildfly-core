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
package org.jboss.as.cli.aesh.activator;

import java.util.HashSet;
import java.util.Set;
import org.jboss.aesh.cl.activation.OptionActivator;

/**
 *
 * Builder for Presence activator.
 *
 * @author jdenise@redhat.com
 */
public class PresenceOptionsActivatorBuilder {

    private final Set<String> expected = new HashSet<>();
    private final Set<String> notExpected = new HashSet<>();

    public PresenceOptionsActivatorBuilder expected(String expected) {
        this.expected.add(expected);
        return this;
    }

    public PresenceOptionsActivatorBuilder notExpected(String notExpected) {
        this.notExpected.add(notExpected);
        return this;
    }

    public OptionActivator create() {
        return new PresenceOptionsActivator(expected, notExpected);
    }

}
