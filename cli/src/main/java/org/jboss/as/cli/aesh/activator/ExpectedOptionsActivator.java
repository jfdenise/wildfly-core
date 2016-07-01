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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;

/**
 *
 * Search for a set of options, activate the option if all options found.
 *
 * @author jdenise@redhat.com
 */
public class ExpectedOptionsActivator implements OptionActivator {

    private final Set<String> options;

    public ExpectedOptionsActivator(String... opts) {
        options = new HashSet<>(Arrays.asList(opts));
    }

    public ExpectedOptionsActivator(Set<String> opts) {
        options = opts;
    }

    @Override
    public boolean isActivated(ProcessedCommand processedCommand) {
        boolean found = true;
        for (String opt : options) {
            ProcessedOption processedOption = processedCommand.findLongOptionNoActivatorCheck(opt);
            found &= processedOption != null && processedOption.getValue() != null;
        }
        return found;
    }
}
