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

import java.util.List;
import java.util.Objects;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class CompletionChildNamesProducer extends CompletionAbstractProducer {

    private final String type;

    CompletionChildNamesProducer(DefaultCallbackHandler producerOperation) throws CommandFormatException {
        super(producerOperation);
        Objects.requireNonNull(producerOperation);
        OperationRequestAddress address = producerOperation.getAddress();
        // Remove the last part.
        type = address.toParentNode().getType();
    }

    @Override
    public ModelNode transferProducerContent(OperationRequestBuilder builder) throws OperationFormatException {
        Objects.requireNonNull(builder);
        builder.setOperationName(Util.READ_CHILDREN_NAMES);
        builder.addProperty("child-type", type);
        return builder.buildRequest();
    }

    @Override
    public void extractData(ModelNode mn, List<String> data) {
        Objects.requireNonNull(mn);
        Objects.requireNonNull(data);
        if (mn.hasDefined(Util.OUTCOME) && mn.get(Util.OUTCOME).asString().equals(Util.SUCCESS)) {
            ModelNode result = mn.get(Util.RESULT);
            if (result.isDefined()) {
                for (ModelNode r : result.asList()) {
                    data.add(r.asString());
                }
            }
        }
    }

}
