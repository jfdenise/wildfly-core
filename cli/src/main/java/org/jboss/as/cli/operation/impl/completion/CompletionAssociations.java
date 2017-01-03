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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.parsing.ParserUtil;

/**
 *
 * @author jdenise@redhat.com
 */
public class CompletionAssociations {

    private static CompletionAssociations INSTANCE;

    private final List<CompletionAssociation> associations = new ArrayList<>();

    public List<String> getCompletionContent(CommandContext ctx, OperationRequestAddress address,
            String operation, String property) throws OperationFormatException, IOException {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(address);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(property);
        for (CompletionAssociation ca : associations) {
            List<String> ret = ca.getData(ctx, address, operation, property);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public void addAssociation(String producer, String consumer) throws CommandFormatException {
        Objects.requireNonNull(producer);
        Objects.requireNonNull(consumer);
        addAssociation(Arrays.asList(producer), Arrays.asList(consumer));
    }

    public void addAssociation(List<String> producers, List<String> consumers) throws CommandFormatException {
        Objects.requireNonNull(producers);
        Objects.requireNonNull(consumers);
        List<CompletionProducer> completionProducers = new ArrayList<>();
        for (String producer : producers) {
            int i = producer.lastIndexOf(CompletionOperationProducer.END_OP);
            String prop = null;
            if (i >= 0) {
                if (i + CompletionOperationProducer.END_OP.length() < producer.length()) {
                    prop = producer.substring(i + CompletionOperationProducer.END_OP.length() + 1);
                    producer = producer.substring(0, i + CompletionOperationProducer.END_OP.length() - 1);
                }
            }
            DefaultCallbackHandler producerOperation = new DefaultCallbackHandler(false);
            ParserUtil.parse(producer, producerOperation);
            CompletionProducer completionProducer;
            // Do we have an operation name?
            if (producerOperation.hasOperationName()) {
                completionProducer = new CompletionOperationProducer(producerOperation, prop);
            } else if (producerOperation.getAddress().endsOnType()) {
                // Return the child list.
                completionProducer = new CompletionChildNamesProducer(producerOperation);
            } else {
                throw new CommandFormatException("Invalid producer syntax: " + producer);
            }
            completionProducers.add(completionProducer);
        }

        associations.add(new CompletionAssociation(completionProducers, consumers));
    }

    public static CompletionAssociations getDefault() throws CliInitializationException {
        if (INSTANCE == null) {
            // Read from config file.
            INSTANCE = CompletionFileReader.load();
        }
        return INSTANCE;
    }
}
