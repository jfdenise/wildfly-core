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
package org.jboss.as.cli.operation.impl.completion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;

/**
 *
 * @author jdenise@redhat.com
 */
public abstract class CompletionAbstractProducer implements CompletionProducer {

    private final List<String> keys = new ArrayList<>();
    protected final DefaultCallbackHandler producerOperation;

    protected CompletionAbstractProducer(DefaultCallbackHandler producerOperation) throws CommandFormatException {
        Objects.requireNonNull(producerOperation);
        this.producerOperation = producerOperation;
        for (OperationRequestAddress.Node node : producerOperation.getAddress()) {
            if (node.getName() != null && node.getName().equals("*")) {
                keys.add(node.getType());
            }
        }
    }

    @Override
    public Map<String, String> resolvePath(OperationRequestAddress address) {
        Objects.requireNonNull(address);
        Map<String, String> mapping = new HashMap<>();
        // Retrieve all the values that need to be applied to the producer address.
        for (OperationRequestAddress.Node node : address) {
            if (keys.contains(node.getType())) {
                mapping.put(node.getType(), node.getName());
            }
        }
        return mapping;
    }

    @Override
    public OperationRequestAddress getAddress() {
        return producerOperation.getAddress();
    }
}
