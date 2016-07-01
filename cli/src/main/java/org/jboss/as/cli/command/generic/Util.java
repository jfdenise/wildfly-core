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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 *
 * @author jfdenise
 */
public class Util {

    interface AttributeDescription {

        String getName();

        ModelType getType();

        String getAccess();

        boolean isWriteAllowed();

        String getDescription();

        ModelNode getProperty(String name);

        boolean getBooleanProperty(String name);
    }

    private static final int DASH_OFFSET = 22;

    public static ModelNode initRequest(CommandContext ctx, NodeType nodeType) throws CommandFormatException {
        ModelNode request = new ModelNode();
        ModelNode address = request.get(org.jboss.as.cli.Util.ADDRESS);
        // XXX JFDENISE, WE WILL SEE IF WE CAN HAVE THIS METHOD TO REPLACE ALL INIT REQIUESTS...
        if (nodeType.dependsOnProfile() && ctx.isDomainMode()) {
            // XXX JFDENISE< MUST RETRIEVE THE PROFILE AND CONVEY IT THERE
            final String profileName = null; //profile.getValue(ctx.getParsedCommandLine());
            if (profileName == null) {
                throw new CommandFormatException("WARNING: --profile argument is required for the complete description.");
            }
            address.add(org.jboss.as.cli.Util.PROFILE, profileName);
        }
        for (OperationRequestAddress.Node node : nodeType.getAddress()) {
            address.add(node.getType(), node.getName());
        }
        address.add(nodeType.getType(), "?");
        return request;
    }

    public static List<Property> getNodeProperties(CommandContext context, NodeType nodeType) throws CommandFormatException {
        final ModelNode request = initRequest(context, nodeType);
        request.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.READ_RESOURCE_DESCRIPTION);
        ModelNode result;
        try {
            result = context.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        if (!result.hasDefined(org.jboss.as.cli.Util.RESULT)) {
            return Collections.emptyList();
        }
        result = result.get(org.jboss.as.cli.Util.RESULT);
        if (!result.hasDefined(org.jboss.as.cli.Util.ATTRIBUTES)) {
            return Collections.emptyList();
        }
        return result.get(org.jboss.as.cli.Util.ATTRIBUTES).asPropertyList();
    }

    public static Iterator<AttributeDescription>
            getAttributeDescriptions(CommandContext ctx, NodeType nodeType) throws CommandLineException {

        if (ctx.getModelControllerClient() == null) {
            throw new CommandLineException("Failed to load target attributes: not connected");
        }
        ModelNode request = org.jboss.as.cli.command.generic.Util.initRequest(ctx, nodeType);
        if (request == null) {
            return Collections.emptyIterator();
        }
        request.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.READ_RESOURCE_DESCRIPTION);
        if (ctx.getConfig().isAccessControl()) {
            request.get(org.jboss.as.cli.Util.ACCESS_CONTROL).set(org.jboss.as.cli.Util.COMBINED_DESCRIPTIONS);
        }
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return Collections.emptyIterator();
        }
        if (!result.hasDefined(org.jboss.as.cli.Util.RESULT)) {
            return Collections.emptyIterator();
        }
        result = result.get(org.jboss.as.cli.Util.RESULT);
        if (!result.hasDefined(org.jboss.as.cli.Util.ATTRIBUTES)) {
            return Collections.emptyIterator();
        }

        final ModelNode accessControl;
        if (ctx.getConfig().isAccessControl()) {
            if (result.has(org.jboss.as.cli.Util.ACCESS_CONTROL)) {
                accessControl = result.get(org.jboss.as.cli.Util.ACCESS_CONTROL);
            } else {
                accessControl = null;
            }
        } else {
            accessControl = null;
        }

        return getAttributeIterator(result.get(org.jboss.as.cli.Util.ATTRIBUTES).asPropertyList(), accessControl);
    }

    public static List<String> getSupportedCommands(CommandContext ctx,
            NodeType nodeType, Set<String> excludedOps, Map<String, Object> customHandlers) throws CommandLineException {
        final ModelNode request = initRequest(ctx, nodeType);
        request.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.READ_OPERATION_NAMES);
        if (ctx.getConfig().isAccessControl()) {
            request.get(org.jboss.as.cli.Util.ACCESS_CONTROL).set(true);
        }
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            throw new CommandLineException("Failed to load a list of commands.", e);
        }
        if (!result.hasDefined(org.jboss.as.cli.Util.RESULT)) {
            throw new CommandLineException("Operation names aren't available.");
        }
        final List<ModelNode> nodeList = result.get(org.jboss.as.cli.Util.RESULT).asList();
        final List<String> supportedCommands = new ArrayList<String>(nodeList.size());
        if (!nodeList.isEmpty()) {
            for (ModelNode node : nodeList) {
                final String opName = node.asString();
                if (!excludedOps.contains(opName) && (customHandlers == null || !customHandlers.containsKey(opName))) {
                    supportedCommands.add(opName);
                }
            }
        }
        if (customHandlers != null) {
            supportedCommands.addAll(customHandlers.keySet());
        }
        Collections.sort(supportedCommands);
        return supportedCommands;
    }

    public static Iterator<AttributeDescription> getAttributeIterator(final List<Property> props, ModelNode accessControl) {
        final ModelNode attrAccessControl;
        if (accessControl != null) {
            if (accessControl.has(org.jboss.as.cli.Util.DEFAULT)) {
                final ModelNode def = accessControl.get(org.jboss.as.cli.Util.DEFAULT);
                if (def.has(org.jboss.as.cli.Util.ATTRIBUTES)) {
                    attrAccessControl = def.get(org.jboss.as.cli.Util.ATTRIBUTES);
                } else {
                    attrAccessControl = null;
                }
            } else {
                attrAccessControl = null;
            }
        } else {
            attrAccessControl = null;
        }
        return new Iterator<AttributeDescription>() {

            final Iterator<Property> properties = props.iterator();
            private Property current;
            private final AttributeDescription descr
                    = new AttributeDescription() {

                @Override
                public String getName() {
                    return current.getName();
                }

                @Override
                public ModelType getType() {
                    final ModelNode value = getProperty(org.jboss.as.cli.Util.TYPE);
                    return value == null ? null : value.asType();
                }

                @Override
                public String getAccess() {
                    if (attrAccessControl != null && attrAccessControl.has(current.getName())) {
                        final ModelNode accessSpec = attrAccessControl.get(current.getName());
                        final StringBuilder buf = new StringBuilder();
                        if (accessSpec.get(org.jboss.as.cli.Util.READ).asBoolean()) {
                            buf.append(org.jboss.as.cli.Util.READ).append('-');
                        }
                        if (accessSpec.get(org.jboss.as.cli.Util.WRITE).asBoolean()) {
                            buf.append(org.jboss.as.cli.Util.WRITE);
                            if (buf.length() == 5) {
                                buf.append("-only");
                            }
                        } else {
                            buf.append("only");
                        }
                        return buf.toString();
                    } else {
                        final ModelNode value = getProperty(org.jboss.as.cli.Util.ACCESS_TYPE);
                        return value == null ? null : value.asString();
                    }
                }

                @Override
                public boolean isWriteAllowed() {
                    if (attrAccessControl != null && attrAccessControl.has(current.getName())) {
                        final ModelNode accessSpec = attrAccessControl.get(current.getName());
                        return accessSpec.get(org.jboss.as.cli.Util.WRITE).asBoolean();
                    }
                    final ModelNode value = getProperty(org.jboss.as.cli.Util.ACCESS_TYPE);
                    if (value == null) {
                        return false;
                    }
                    return org.jboss.as.cli.Util.READ_WRITE.equals(value.asString());
                }

                @Override
                public String getDescription() {
                    final ModelNode value = getProperty(org.jboss.as.cli.Util.DESCRIPTION);
                    return value == null ? null : value.asString();
                }

                @Override
                public ModelNode getProperty(String name) {
                    if (current.getValue().has(name)) {
                        return current.getValue().get(name);
                    }
                    return null;
                }

                @Override
                public boolean getBooleanProperty(String name) {
                    if (current.getValue().has(name)) {
                        return current.getValue().get(name).asBoolean();
                    }
                    return false;
                }
            };

            @Override
            public boolean hasNext() {
                return properties.hasNext();
            }

            @Override
            public AttributeDescription next() {
                current = properties.next();
                return descr;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static ModelType asType(ModelNode type) {
        if (type == null) {
            return null;
        }
        try {
            return type.asType();
        } catch (IllegalArgumentException e) {
            // the value type is a structure
            return null;
        }
    }

    public static ModelNode getOperationDescription(CommandContext ctx, String operationName, NodeType nodeType) throws CommandLineException {
        // CCC JFDENISE TODO customHandlers
//        if (customHandlers != null) {
//            final OperationCommandWithDescription handler = customHandlers.get(operationName);
//            if (handler != null) {
//                return handler.getOperationDescription(ctx);
//            }
//        }
        if (ctx.getModelControllerClient() == null) {
            throw new CommandLineException("Failed to load operation description: not connected");
        }

        ModelNode request = initRequest(ctx, nodeType);
        if (request == null) {
            return null;
        }
        request.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.READ_OPERATION_DESCRIPTION);
        request.get(org.jboss.as.cli.Util.NAME).set(operationName);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            throw new CommandFormatException("Failed to execute read-operation-description.", e);
        }
        if (!result.hasDefined(org.jboss.as.cli.Util.RESULT)) {
            String msg = org.jboss.as.cli.Util.getFailureDescription(result);
            if (msg == null) {
                msg = "Failed to load description for '" + operationName + "': " + result;
            }
            throw new CommandLineException(msg);
        }
        return result.get(org.jboss.as.cli.Util.RESULT);
    }

    public static void printProperties(CommandContext ctx, String propertyId,
            Iterator<AttributeDescription> props) {
        final Map<String, StringBuilder> requiredProps = new LinkedHashMap<String, StringBuilder>();
        requiredProps.put(propertyId, new StringBuilder().append("Required argument in commands which identifies the instance to execute the command against."));
        final Map<String, StringBuilder> optionalProps = new LinkedHashMap<String, StringBuilder>();

        String accessType = null;
        while (props.hasNext()) {
            AttributeDescription attr = props.next();
            //final ModelNode value = attr.getValue();

            // filter metrics
            accessType = attr.getAccess();
//            if("metric".equals(accessType)) {
//                continue;
//            }

            final boolean required = attr.getBooleanProperty(org.jboss.as.cli.Util.REQUIRED);
            final StringBuilder descr = new StringBuilder();

            final ModelType modelType = attr.getType();
            final String type = modelType == null ? "no type info" : modelType.toString();
            final String attrDescr = attr.getDescription();
            if (attrDescr != null) {
                descr.append('(');
                descr.append(type);
                if (accessType != null) {
                    descr.append(',').append(accessType);
                }
                descr.append(") ");
                descr.append(attrDescr);
            } else if (descr.length() == 0) {
                descr.append("no description.");
            }

            if (required) {
                if (propertyId != null && propertyId.equals(attr.getName())) {
                    if (descr.charAt(descr.length() - 1) != '.') {
                        descr.append('.');
                    }
                    requiredProps.get(propertyId).insert(0, ' ').insert(0, descr.toString());
                } else {
                    requiredProps.put("--" + attr.getName(), descr);
                }
            } else {
                optionalProps.put("--" + attr.getName(), descr);
            }
        }

        ctx.printLine("\n");
        if (accessType == null) {
            ctx.printLine("REQUIRED ARGUMENTS:\n");
        }
        for (String argName : requiredProps.keySet()) {
            formatProperty(ctx, argName, requiredProps.get(argName));
        }

        if (!optionalProps.isEmpty()) {
            if (accessType == null) {
                ctx.printLine("\n\nOPTIONAL ARGUMENTS:\n");
            }
            for (String argName : optionalProps.keySet()) {
                formatProperty(ctx, argName, optionalProps.get(argName));
            }
        }
    }

    public static void formatProperty(CommandContext ctx, String argName, final CharSequence descr) {

        final StringBuilder prop = new StringBuilder();
        prop.append(' ').append(argName);
        int spaces = DASH_OFFSET - prop.length();
        do {
            prop.append(' ');
            --spaces;
        } while (spaces >= 0);

        int terminalWidth = ctx.getTerminalWidth();
        if (terminalWidth <= 0) {
            terminalWidth = 80;
        }

        int dashIndex = prop.length();
        int textOffset = dashIndex + 3;
        int textLength = terminalWidth - textOffset;
        prop.append(" - ");

        if (descr.length() <= textLength) {
            prop.append(descr);
            prop.append(org.jboss.as.cli.Util.LINE_SEPARATOR);
        } else {
            int lineStart = 0;
            int lineNo = 1;
            while (lineStart < descr.length()) {
                prop.ensureCapacity(terminalWidth);
                if (lineStart > 0) {
                    if (lineNo == 3 && dashIndex > DASH_OFFSET) {
                        textOffset = DASH_OFFSET + 2;
                        textLength = terminalWidth - textOffset;
                    }
                    for (int i = 0; i < textOffset; ++i) {
                        prop.append(' ');
                    }
                }
                int lastCharIndex = lineStart + textLength;

                if (lastCharIndex >= descr.length()) {
                    lastCharIndex = descr.length();
                    prop.append(descr.subSequence(lineStart, lastCharIndex));
                    lineStart = lastCharIndex;
                } else {
                    while (lastCharIndex >= lineStart && !Character.isWhitespace(descr.charAt(lastCharIndex))) {
                        --lastCharIndex;
                    }
                    if (lastCharIndex <= lineStart) {
                        lastCharIndex = lineStart + textLength;
                        prop.append(descr.subSequence(lineStart, lastCharIndex));
                        lineStart = lastCharIndex;
                    } else {
                        prop.append(descr.subSequence(lineStart, lastCharIndex));
                        lineStart = lastCharIndex + 1;
                    }
                }
                prop.append(org.jboss.as.cli.Util.LINE_SEPARATOR);
                ++lineNo;
            }
        }
        ctx.printLine(prop.toString());
    }

    public static void formatText(CommandContext ctx, CharSequence text, int offset) {
        int terminalWidth = ctx.getTerminalWidth();
        if (terminalWidth <= 0) {
            terminalWidth = 80;
        }
        final StringBuilder target = new StringBuilder();
        if (offset >= terminalWidth) {
            target.append(text);
        } else {
            int startIndex = 0;
            while (startIndex < text.length()) {
                if (startIndex > 0) {
                    target.append(org.jboss.as.cli.Util.LINE_SEPARATOR);
                }
                for (int i = 0; i < offset; ++i) {
                    target.append(' ');
                }
                int endIndex = startIndex + terminalWidth - offset;
                if (endIndex > text.length()) {
                    endIndex = text.length();
                    target.append(text.subSequence(startIndex, endIndex));
                    startIndex = endIndex;
                } else {
                    while (endIndex >= startIndex && !Character.isWhitespace(text.charAt(endIndex))) {
                        --endIndex;
                    }
                    if (endIndex <= startIndex) {
                        endIndex = startIndex + terminalWidth - 2;
                        target.append(text.subSequence(startIndex, endIndex));
                        startIndex = endIndex;
                    } else {
                        target.append(text.subSequence(startIndex, endIndex));
                        startIndex = endIndex + 1;
                    }
                }
            }
        }
        ctx.printLine(target.toString());
    }
}
