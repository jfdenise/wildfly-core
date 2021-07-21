/*
Copyright 2021 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.cli.handlers.ifelse;

import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class SizeOperation extends ComparisonOperation {

    static final String SYMBOL = "#=";

    SizeOperation() {
        super(SYMBOL);
    }

    @Override
    protected boolean compare(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        ModelNode array = (ModelNode) left;
        ModelNode size = (ModelNode) right;
        try {
            return array.asList().size() == size.asInt();
        } catch(IllegalArgumentException ex) {
            return false;
        }
    }
}
