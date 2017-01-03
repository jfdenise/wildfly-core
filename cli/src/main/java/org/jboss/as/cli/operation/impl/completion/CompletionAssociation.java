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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.cli.operation.OperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class CompletionAssociation {

    private final List<PathMatcher> consumers = new ArrayList<>();
    private final List<CompletionProducer> producers;

    CompletionAssociation(List<CompletionProducer> producers, List<String> consumers) throws CommandFormatException {
        Objects.requireNonNull(producers);
        Objects.requireNonNull(consumers);
        for (String consumer : consumers) {
            Objects.requireNonNull(consumer);
            this.consumers.add(createPathMatcher(consumer));
        }
        this.producers = producers;
    }

    private PathMatcher createPathMatcher(String path) {
        return FileSystems.getDefault().getPathMatcher("glob:" + path);
    }

    public List<String> getData(CommandContext ctx, OperationRequestAddress address,
            String operation, String property) throws OperationFormatException, IOException {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(address);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(property);
        List<String> result = null;
        if (matches(address, operation, property)) {
            result = new ArrayList<>();
            for (CompletionProducer producer : producers) {
                List<ModelNode> ops = getCompletionProducerRequests(ctx, producer, address);
                if (ops != null) {
                    for (ModelNode op : ops) {
                        ModelControllerClient client = ctx.getModelControllerClient();
                        if (client != null) {
                            ModelNode response = client.execute(op);
                            producer.extractData(response, result);
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean matches(OperationRequestAddress address,
            String operation, String property) {
        StringBuilder build = new StringBuilder();
        build.append("/");
        for (OperationRequestAddress.Node node : address) {
            build.append(node.getType()).append("=").append(node.getName()).append("/");
        }
        String path = build.toString();
        // Remove the last "/"
        String cons = path.substring(0, path.length() - 1) + ":" + operation + "(" + property + ")";
        for (PathMatcher consumer : consumers) {
            if (consumer.matches(Paths.get(cons))) {
                return true;
            }
        }
        return false;
    }

    private List<ModelNode> getCompletionProducerRequests(CommandContext ctx,
            CompletionProducer producer, OperationRequestAddress address) throws OperationFormatException, IOException {
        Map<String, String> mapping = producer.resolvePath(address);
        Stack<OperationRequestAddress> addressResolutionStack = new Stack<>();
        addressResolutionStack.push(producer.getAddress());
        List<ModelNode> operations = new ArrayList<>();
        while (!addressResolutionStack.empty()) {
            OperationRequestAddress addr = addressResolutionStack.pop();
            Iterator<Node> it = addr.iterator();
            boolean complete = true;
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            while (it.hasNext()) {
                Node node = it.next();
                String value = node.getName();
                if (mapping.containsKey(node.getType())) {
                    value = mapping.get(node.getType());
                    builder.addNode(node.getType(), value);
                } else if (node.getName().equals("*")) {
                    complete = false;
                    boolean mustContinue = false;
                    // We need to call on all children.
                    // Retrieve the suffix and consume fully the address.
                    List<Node> suffix = new ArrayList<>();
                    while (it.hasNext()) {
                        Node snode = it.next();
                        if (snode.getType().equals("*")) {
                            mustContinue = true;
                        }
                        suffix.add(snode);
                    }
                    List<String> children = getChildren(ctx, builder.getAddress(), node.getType());
                    OperationRequestAddress radical = builder.getAddress();
                    for (String child : children) {
                        DefaultOperationRequestBuilder childOp = new DefaultOperationRequestBuilder(radical);
                        childOp.addNode(node.getType(), child);
                        for (Node n : suffix) {
                            childOp.addNode(n.getType(), n.getName());
                        }
                        if (mustContinue) {
                            addressResolutionStack.push(childOp.getAddress());
                        } else {
                            // OK, the operation address is complete
                            operations.add(producer.transferProducerContent(childOp));
                        }
                    }
                } else {
                    builder.addNode(node.getType(), value);
                }
            }
            if (complete) {
                operations.add(producer.transferProducerContent(builder));
            }
        }
        return operations;
    }

    private List<String> getChildren(CommandContext ctx, OperationRequestAddress addr,
            String childType) throws OperationFormatException, IOException {
        OperationRequestBuilder builder = new DefaultOperationRequestBuilder(addr);
        builder.setOperationName(Util.READ_CHILDREN_NAMES);
        builder.addProperty(Util.CHILD_TYPE, childType);
        ModelControllerClient client = ctx.getModelControllerClient();
        List<String> children = new ArrayList<>();
        if (client != null) {
            ModelNode response = client.execute(builder.buildRequest());
            if (response.hasDefined(Util.OUTCOME)
                    && response.get(Util.OUTCOME).asString().equals(Util.SUCCESS)) {
                ModelNode result = response.get(Util.RESULT);
                for (ModelNode r : result.asList()) {
                    children.add(r.asString());
                }
            }
        }
        return children;
    }
}
