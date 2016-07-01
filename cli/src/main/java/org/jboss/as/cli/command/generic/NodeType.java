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
package org.jboss.as.cli.command.generic;

import java.util.Iterator;
import java.util.Objects;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.parsing.ParserUtil;

/**
 *
 * @author jfdenise
 */
public class NodeType {

    private final OperationRequestAddress nodeAddress;

    private final boolean dependsOnProfile;
    private final String nodeType;
    private final String path;
    public NodeType(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Node type is " + (path == null ? "null." : "empty."));
        }

        if (path.startsWith("/profile=") || path.startsWith("profile=")) {
            int nextSep = path.indexOf('/', 7);
            if (nextSep < 0) {
                throw new IllegalArgumentException("Failed to determine the path after the profile in '" + path + "'.");
            }
            path = path.substring(nextSep);
        }

        nodeAddress = new DefaultOperationRequestAddress();
        CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(nodeAddress);
        try {
            ParserUtil.parseOperationRequest(path, handler);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException("Failed to parse nodeType: " + e.getMessage());
        }

        if (nodeAddress.endsOnType()) {
            nodeType = nodeAddress.toParentNode().getType();
        } else {
            throw new IllegalArgumentException("Node address must end on type");
        }

        final Iterator<OperationRequestAddress.Node> iterator = nodeAddress.iterator();
        if (iterator.hasNext()) {
            final String firstType = iterator.next().getType();
            // XXX JFDENISE??? REALLY????
            dependsOnProfile = Util.SUBSYSTEM.equals(firstType) || Util.PROFILE.equals(firstType);
        } else {
            dependsOnProfile = false;
        }
        this.path = path;
    }

    public OperationRequestAddress getAddress() {
        return nodeAddress;
    }

    public String getType() {
        return nodeType;
    }

    public boolean dependsOnProfile() {
        return dependsOnProfile;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NodeType)) {
            return false;
        }
        NodeType nt = (NodeType) other;
        return path.equals(nt.path);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.path);
        return hash;
    }
}
