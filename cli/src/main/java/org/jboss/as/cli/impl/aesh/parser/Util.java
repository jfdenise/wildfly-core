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
package org.jboss.as.cli.impl.aesh.parser;

import org.aesh.cl.internal.ProcessedOption;

/**
 *
 * @author jdenise@redhat.com
 */
public class Util {

    public static String updateOptionValue(ProcessedOption option, String line, int offset) {
        if (offset < 0 || offset > line.length() - 1) {
            offset = line.length() - 1;
        }
        String value = line.substring(0, offset + 1);
        if (option != null) {
            option.addValue(value);
        }
        if (offset == line.length() - 1) {
            return "";
        } else {
            return line.substring(offset + 1);
        }
    }
}
