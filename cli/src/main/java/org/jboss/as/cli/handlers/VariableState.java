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
package org.jboss.as.cli.handlers;

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise
 */
public class VariableState {

    private final String name;
    private final String value;
    private final String path;
    private final boolean resolveExpressions;

    VariableState(String name, String value, String path, boolean resolveExpressions) {
        this.name = name;
        this.value = value;
        this.path = path;
        this.resolveExpressions = resolveExpressions;

    }

    public ModelNode getValue(CommandContext ctx) throws CommandLineException {
        ModelNode val = resolveExpressions
                ? ArgumentValueConverter.RESOLVE_EXPRESSIONS.fromString(ctx, value)
                : ArgumentValueConverter.DEFAULT.fromString(ctx, value);
        if (path != null) {
            String[] pathArray = path.split("\\.");
            ModelNode mn = new ModelNode();
            mn.set(name, val);
            val = parsePath(pathArray, mn);
        }
        return val;
    }

    public static ModelNode parsePath(String[] path, ModelNode targetValue) throws CommandLineException {
        for (String name : path) {
            boolean array = false;
            int arrayIndex = 0;
            int openIndex = name.indexOf("[");
            if (openIndex > 0) {
                array = true;
                int closeIndex = name.indexOf("]");
                if (closeIndex <= 0) {
                    throw new CommandLineException("If condition, invalid array syntax for " + name);
                }
                arrayIndex = Integer.valueOf(name.substring(openIndex + 1, closeIndex));
                name = name.substring(0, openIndex);
            }
            if (!targetValue.has(name)) {
                return null;
            } else {
                targetValue = targetValue.get(name);
                if (array) {
                    targetValue = targetValue.get(arrayIndex);
                }
            }
        }
        return targetValue == null ? null : targetValue;
    }

    public static VariableState buildVariable(String var, CommandContext ctx, boolean resolveExpressions) {
        if (var == null) {
            return null;
        }
        VariableState retVariable = null;
        var = var.trim();
        String path = null;
        String varValue = null;
        if (resolveExpressions && var.startsWith("${")) {
            retVariable = new VariableState(var, var, null, true);
        } else {
            if (var.startsWith("$")) {
                path = var.substring(1);
                int dotIndex = var.indexOf(".");
                int end = dotIndex < 0 ? var.length() : dotIndex;
                var = var.substring(1, end);
                int arrayIndex = var.indexOf("[");
                if (arrayIndex > 0) {
                    var = var.substring(0, arrayIndex);
                }
                varValue = ctx.getVariable(var);
            }
            if (varValue != null) {
                retVariable = new VariableState(var, varValue, path, false);
            }
        }
        return retVariable;
    }
}
