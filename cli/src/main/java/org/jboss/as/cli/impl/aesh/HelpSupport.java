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
package org.jboss.as.cli.impl.aesh;

import org.wildfly.core.cli.command.aesh.activator.HiddenActivator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import org.aesh.util.Config;
import org.aesh.command.option.Arguments;
import org.aesh.command.CommandDefinition;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.activator.OptionActivator;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.internal.ProcessedOptionBuilder;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.Command;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.wildfly.core.cli.command.aesh.activator.DomainOptionActivator;
import org.wildfly.core.cli.command.aesh.activator.ExpectedOptionsActivator;
import org.wildfly.core.cli.command.aesh.activator.NotExpectedOptionsActivator;
import org.wildfly.core.cli.command.aesh.activator.StandaloneOptionActivator;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author jdenise@redhat.com
 */
public class HelpSupport {

    // Field name (String[]) that advertises the expected options.
    // Fallback when Activator can't extend ExpectedOptionsActivator
    public static final String WF_CLI_EXPECTED_OPTIONS = "WF_CLI_EXPECTED_OPTIONS";
    private static final String TAB = "    ";
    private static final String OPTION_PREFIX = "--";
    private static final String OPTION_SUFFIX = "  - ";

    static void printHelp(CommandContext ctx) {
        ctx.printLine(printHelp(ctx, "help"));
    }

    public static String printHelp(CommandContext ctx, String filename) {
        filename = "help/" + filename + ".txt";
        InputStream helpInput = WildFlySecurityManager.getClassLoaderPrivileged(CommandHandlerWithHelp.class).getResourceAsStream(filename);
        if (helpInput != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(helpInput));
            try {
                /*                String helpLine = reader.readLine();
                while(helpLine != null) {
                    ctx.printLine(helpLine);
                    helpLine = reader.readLine();
                }
                 */
                return format(ctx, reader);
            } catch (java.io.IOException e) {
                return "Failed to read " + filename + ". " + e.getLocalizedMessage();
            } finally {
                StreamUtils.safeClose(reader);
            }
        } else {
            return "Failed to locate command description " + filename;
        }
    }

    public static String format(CommandContext ctx, BufferedReader reader) throws IOException {

        StringBuilder builder = new StringBuilder();
        int width = ctx.getTerminalWidth();
        if (width <= 0) {
            width = 80;
        }
        String line = reader.readLine();

        while (line != null) {
            final String next = reader.readLine();

            if (line.length() < width) {
                builder.append(line).append("\n");
            } else {
                int offset = 0;
                if (next != null && !next.isEmpty()) {
                    int i = 0;
                    while (i < next.length()) {
                        if (!Character.isWhitespace(next.charAt(i))) {
                            offset = i;
                            break;
                        }
                        ++i;
                    }
                } else {
                    int i = 0;
                    while (i < line.length()) {
                        if (!Character.isWhitespace(line.charAt(i))) {
                            offset = i;
                            break;
                        }
                        ++i;
                    }
                }

                final char[] offsetArr;
                if (offset == 0) {
                    offsetArr = null;
                } else {
                    offsetArr = new char[offset];
                    Arrays.fill(offsetArr, ' ');
                }

                int endLine = width;
                while (endLine >= 0) {
                    if (Character.isWhitespace(line.charAt(endLine - 1))) {
                        break;
                    }
                    --endLine;
                }
                if (endLine < 0) {
                    endLine = width;
                }

                builder.append(line.substring(0, endLine)).append("\n");

                int lineIndex = endLine;
                while (lineIndex < line.length()) {
                    int startLine = lineIndex;
                    endLine = Math.min(startLine + width - offset, line.length());

                    while (startLine < endLine) {
                        if (!Character.isWhitespace(line.charAt(startLine))) {
                            break;
                        }
                        ++startLine;
                    }
                    if (startLine == endLine) {
                        startLine = lineIndex;
                    }

                    endLine = startLine + width - offset;
                    if (endLine > line.length()) {
                        endLine = line.length();
                    } else {
                        while (endLine > startLine) {
                            if (Character.isWhitespace(line.charAt(endLine - 1))) {
                                --endLine;
                                break;
                            }
                            --endLine;
                        }
                        if (endLine == startLine) {
                            endLine = Math.min(startLine + width - offset, line.length());
                        }
                    }
                    lineIndex = endLine;

                    if (offsetArr != null) {
                        final StringBuilder lineBuf = new StringBuilder();
                        lineBuf.append(offsetArr);
                        lineBuf.append(line.substring(startLine, endLine));
                        builder.append(lineBuf.toString()).append("\n");
                    } else {
                        builder.append(line.substring(startLine, endLine)).append("\n");
                    }
                }
            }

            line = next;
        }
        return builder.toString();
    }

    private static final Map<Class<?>, String> VALUES = new HashMap<>();

    static {
        VALUES.put(BigDecimal.class, "a big decimal");
        VALUES.put(Boolean.class, "true|false");
        VALUES.put(BigInteger.class, "a big integer");
        VALUES.put(byte[].class, "bytes array");
        VALUES.put(Double.class, "a double");
        VALUES.put(Expression.class, "an expression");
        VALUES.put(Integer.class, "an integer");
        VALUES.put(List.class, "a list");
        VALUES.put(Long.class, "a long");
        VALUES.put(Object.class, "an object");
        VALUES.put(Property.class, "a property");
        VALUES.put(String.class, "a string");
        VALUES.put(ModelType.class, "a type");
    }

    private static class Expression {

    }

    public static Class<?> getClassFromType(ModelType mt) {
        Class<?> clazz = String.class;
        switch (mt) {
            case BIG_DECIMAL: {
                clazz = BigDecimal.class;
                break;
            }
            case BOOLEAN: {
                clazz = Boolean.class;
                break;
            }
            case BIG_INTEGER: {
                clazz = BigInteger.class;
                break;
            }
            case BYTES: {
                clazz = byte[].class;
                break;
            }
            case DOUBLE: {
                clazz = Double.class;
                break;
            }
            case EXPRESSION: {
                clazz = Expression.class;
                break;
            }
            case INT: {
                clazz = Integer.class;
                break;
            }
            case LIST: {
                clazz = List.class;
                break;
            }
            case LONG: {
                clazz = Long.class;
                break;
            }
            case OBJECT: {
                clazz = Object.class;
                break;
            }
            case PROPERTY: {
                clazz = Property.class;
                break;
            }
            case STRING: {
                clazz = String.class;
                break;
            }
            case TYPE: {
                clazz = ModelType.class;
                break;
            }
            default: {
                clazz = String.class;
                break;
            }
        }
        return clazz;
    }

    public static String printHelp(CommandContext ctx, ModelNode mn) {
        try {
            // Build a ProcessedCommand from the ModelNode
            String commandName = mn.get("operation-name").asString();
            String desc = mn.get(Util.DESCRIPTION).asString();
            ModelNode props = mn.get(Util.REQUEST_PROPERTIES);
            ProcessedCommand<?> pcommand = new ProcessedCommandBuilder().
                    name(commandName).description(desc).create();
            for (String prop : props.keys()) {
                ModelNode p = props.get(prop);
                Class<?> clazz = getClassFromType(p.get(Util.TYPE).asType());
                String pdesc = p.get(Util.TYPE).asString() + ", "
                        + p.get(Util.DESCRIPTION).asString();
                ProcessedOption opt = ProcessedOptionBuilder.builder().name(prop).
                        required(true).
                        hasValue(true).
                        description(pdesc).type(clazz).build();
                pcommand.addOption(opt);
            }

            String content = getCommandHelp(null, Collections.emptyList(), null, Collections.emptyList(),
                    pcommand.getOptions(), pcommand,
                    null, commandName, pcommand, true);
            if (mn.hasDefined("reply-properties")) {
                ModelNode reply = mn.get("reply-properties");
                // Add response value
                StringBuilder builder = new StringBuilder();
                builder.append(content);

                builder.append("RETURN VALUE");

                builder.append(Config.getLineSeparator());
                builder.append(Config.getLineSeparator());

                if (reply.hasDefined("type")) {
                    builder.append(reply.get("type").asString()).append(". ");
                }
                if (reply.hasDefined("description")) {
                    builder.append(reply.get("description").asString());
                }
                builder.append(Config.getLineSeparator());
                builder.append(Config.getLineSeparator());
                content = builder.toString();
            }
            return content;
        } catch (Exception ex) {
            // XXX OK.
            return null;
        }
    }

    public static String getSubCommandHelp(String parentCommand,
            CommandLineParser<Command> parser) {
        String commandName = parser.getProcessedCommand().name();
        return getCommandHelp(parentCommand, commandName, parser);
    }

    public static String getCommandHelp(CommandLineParser<Command> parser) {
        String commandName = parser.getProcessedCommand().name();
        return getCommandHelp(null, commandName, parser);
    }

    private static String getCommandHelp(String parentName, String commandName,
            CommandLineParser<Command> parser) {

        // First retrieve deprecated options.
        Set<String> deprecated = new HashSet<>();
        // All inherited names (used to resolve bndle keys)
        List<String> superNames = new ArrayList<>();
        retrieveDeprecated(deprecated, parser.getCommand().getClass(), superNames);
        retrieveHidden(deprecated, parser.getProcessedCommand());

        List<CommandLineParser<Command>> parsers = parser.getAllChildParsers();

        ResourceBundle bundle = getBundle(parser.getCommand());

        ProcessedCommand<?> pcommand = retrieveDescriptions(bundle, parentName,
                parser.getProcessedCommand(), superNames);

        List<ProcessedOption> opts = new ArrayList<>();
        for (ProcessedOption o : pcommand.getOptions()) {
            if (!deprecated.contains(o.name())) {
                opts.add(o);
            }
        }
        Collections.sort(opts, (ProcessedOption o1, ProcessedOption o2) -> {
            return o1.name().compareTo(o2.name());
        });
        ProcessedOption arg = deprecated.contains("") ? null : pcommand.getArgument();

        return getCommandHelp(bundle, superNames, arg, parsers, opts, pcommand,
                parentName, commandName, parser.getProcessedCommand(), false);
    }

    private static String getCommandHelp(ResourceBundle bundle, List<String> superNames,
            ProcessedOption arg,
            List<CommandLineParser<Command>> parsers,
            List<ProcessedOption> opts,
            ProcessedCommand<?> pcommand,
            String parentName,
            String commandName,
            ProcessedCommand<?> origCommand, boolean isOperation) {
        StringBuilder builder = new StringBuilder();
        builder.append(Config.getLineSeparator());

        // Compute synopsis.
        builder.append("SYNOPSIS").append(Config.getLineSeparator());
        builder.append(Config.getLineSeparator());
        String synopsis = getValue(bundle, parentName, commandName, superNames, "synopsis");
        if (synopsis == null) {
            //Synopsis option tab
            StringBuilder tabBuilder = new StringBuilder();
            if (parentName != null) {
                tabBuilder.append(parentName).append(" ");
            }
            tabBuilder.append(commandName).append(" ");
            StringBuilder synopsisTab = new StringBuilder();
            for (int i = 0; i < tabBuilder.toString().length() + TAB.length(); i++) {
                synopsisTab.append(" ");
            }
            // 2 cases, standalone and domain
            List<ProcessedOption> standalone = retrieveStandaloneOptions(opts);
            if (standalone.size() == opts.size()
                    && (arg == null || !(arg.activator() instanceof DomainOptionActivator))) {
                synopsis = generateSynopsis(bundle, parentName, commandName, opts,
                        arg, parsers != null && parsers.size() > 0, superNames, isOperation, false);
                builder.append(splitAndFormat(synopsis, 80, TAB, 0, synopsisTab.toString()));
            } else {
                builder.append("Standalone mode:").append(Config.getLineSeparator());
                builder.append(Config.getLineSeparator());
                synopsis = generateSynopsis(bundle, parentName, commandName, standalone,
                        arg, parsers != null && parsers.size() > 0, superNames, isOperation, false);
                builder.append(splitAndFormat(synopsis, 80, TAB, 0, synopsisTab.toString()));
                builder.append(Config.getLineSeparator());
                builder.append(Config.getLineSeparator());
                builder.append("Domain mode:").append(Config.getLineSeparator());
                builder.append(Config.getLineSeparator());
                List<ProcessedOption> domain = retrieveDomainOptions(opts);
                synopsis = generateSynopsis(bundle, parentName, commandName, domain,
                        arg, parsers != null && parsers.size() > 0, superNames, isOperation, true);
                builder.append(splitAndFormat(synopsis, 80, TAB, 0, synopsisTab.toString()));
            }
        }
        builder.append(Config.getLineSeparator());
        builder.append(Config.getLineSeparator());
        builder.append("DESCRIPTION").append(Config.getLineSeparator());
        builder.append(Config.getLineSeparator());
        builder.append(HelpSupport.splitAndFormat(pcommand.description(), 80, TAB, 0, TAB));
        builder.append(Config.getLineSeparator());

        if (origCommand.getAliases() != null
                && !origCommand.getAliases().isEmpty()) {
            builder.append("ALIASES").append(Config.getLineSeparator());
            builder.append(Config.getLineSeparator());
            for (String a : origCommand.getAliases()) {
                builder.append(HelpSupport.TAB);
                builder.append(a).append(Config.getLineSeparator());
            }
            builder.append(Config.getLineSeparator());
        }

        builder.append(printOptions(opts,
                arg, isOperation)).
                append(Config.getLineSeparator());

        // Sub Commands
        builder.append(printActions(bundle, parentName, commandName, parsers,
                superNames));
        return builder.toString();
    }

    private static String printActions(ResourceBundle bundle,
            String parentName,
            String commandName,
            List<CommandLineParser<Command>> parsers,
            List<String> superNames) {
        StringBuilder builder = new StringBuilder();
        if (parsers != null && parsers.size() > 0) {
            builder.append("ACTIONS")
                    .append(Config.getLineSeparator()).
                    append(Config.getLineSeparator());
            builder.append("Type \"help ").append(commandName).append(" <action>\" for more details.")
                    .append(Config.getLineSeparator()).
                    append(Config.getLineSeparator());
            List<ProcessedCommand> actions = new ArrayList<>();
            for (CommandLineParser child : parsers) {
                actions.add(retrieveDescriptions(bundle, commandName,
                        child.getProcessedCommand(), superNames));
            }
            // Retrieve the tab length
            int maxActionName = 0;
            for (ProcessedCommand pc : actions) {
                String name = createActionName(pc.name(), pc.name().length());
                if (name.length() > maxActionName) {
                    maxActionName = name.length();
                }
            }
            StringBuilder tabBuilder = new StringBuilder();
            for (int i = 0; i < maxActionName; i++) {
                tabBuilder.append(" ");
            }
            String tab = tabBuilder.toString();
            Collections.sort(actions, (o1, o2) -> o1.name().compareTo(o2.name()));
            for (ProcessedCommand pc : actions) {
                String name = createActionName(pc.name(), maxActionName);
                builder.append(name);
                // Extract first line...
                int length = 77 - tab.length();
                String line = extractFirstLine(pc.description(), length);
                builder.append(line).append("...").append(Config.getLineSeparator()).
                        append(Config.getLineSeparator());
            }
        }
        return builder.toString();
    }

    private static void retrieveHidden(Set<String> deprecated, ProcessedCommand<Command> cmd) {
        if (cmd.getArgument() != null && cmd.getArgument().activator() instanceof HiddenActivator) {
            deprecated.add("");
        }
        for (ProcessedOption po : cmd.getOptions()) {
            if (po.activator() instanceof HiddenActivator) {
                deprecated.add(po.name());
            }
        }
    }

    private static void retrieveDeprecated(Set<String> deprecated, Class clazz, List<String> superNames) {
        for (Field field : clazz.getDeclaredFields()) {
            processField(deprecated, field);
        }

        if (clazz.getSuperclass() != null) {
            Class<?> sup = clazz.getSuperclass();
            if (sup.getAnnotation(CommandDefinition.class) != null) {
                CommandDefinition cd = (CommandDefinition) sup.getAnnotation(CommandDefinition.class);
                superNames.add(cd.name());
            }
            if (sup.getAnnotation(GroupCommandDefinition.class) != null) {
                GroupCommandDefinition gcd = (GroupCommandDefinition) sup.getAnnotation(GroupCommandDefinition.class);
                superNames.add(gcd.name());
            }
            retrieveDeprecated(deprecated, sup, superNames);
        }
    }

    private static void processField(Set<String> deprecated, Field field) {
        Deprecated dep;
        if ((dep = field.getAnnotation(Deprecated.class)) != null) {
            Option o;
            if ((o = field.getAnnotation(Option.class)) != null) {
                String name = o.name();
                if (name == null || name.isEmpty()) {
                    name = field.getName();
                }
                deprecated.add(name);
            } else {
                OptionList ol;
                if ((ol = field.getAnnotation(OptionList.class)) != null) {
                    String name = ol.name();
                    if (name == null || name.isEmpty()) {
                        name = field.getName();
                    }
                    deprecated.add(name);
                } else {
                    Arguments arg;
                    if ((arg = field.getAnnotation(Arguments.class)) != null) {
                        deprecated.add("");
                    }
                }
            }
        }
    }

    private static String generateSynopsis(ResourceBundle bundle,
            String parentName,
            String commandName,
            List<ProcessedOption> opts,
            ProcessedOption arg,
            boolean hasActions,
            List<String> superNames,
            boolean isOperation, boolean domain) {

        // First lookup all dependencies.
        // Key is the option (one for any option),
        // value is the Dependency (contains the list of dependsOn)
        // for the key to be usable.
        Map<ProcessedOption, Dependency> dependencies
                = retrieveDependencies(opts, arg, domain);

        StringBuilder synopsisBuilder = new StringBuilder();
        if (parentName != null) {
            synopsisBuilder.append(parentName).append(" ");
        }
        synopsisBuilder.append(commandName);
        if (isOperation && !opts.isEmpty()) {
            synopsisBuilder.append("(");
        } else {
            synopsisBuilder.append(" ");
        }
        boolean hasOptions = arg != null || !opts.isEmpty();
        if (hasActions && hasOptions) {
            synopsisBuilder.append(" [");
        }
        if (hasActions) {
            synopsisBuilder.append(" <action>");
        }
        if (hasActions && hasOptions) {
            synopsisBuilder.append(" ] || [");
        }
        ProcessedOption opt;
        while ((opt = retrieveNextOption(dependencies)) != null) {
            String content = addSynopsisOption(bundle, dependencies,
                    parentName,
                    commandName, superNames, opt, isOperation);
            if (content != null) {
                synopsisBuilder.append(content.trim());
                if (isOperation) {
                    if (!dependencies.isEmpty()) {
                        synopsisBuilder.append(",");
                    } else {
                        synopsisBuilder.append(")");
                    }
                }
                synopsisBuilder.append(" ");
            }
        }
        if (hasActions && hasOptions) {
            synopsisBuilder.append(" ]");
        }
        return synopsisBuilder.toString();
    }

    private static ProcessedOption retrieveNextOption(Map<ProcessedOption, Dependency> dependencies) {
        if (dependencies.isEmpty()) {
            return null;
        }

        // We must sort the options according to their dependencies.
        // First options that have dependencies and are not listed in dependencies
        // from others.
        // Then sort these by name.
        // Argument being the first one.
        List<ProcessedOption> options = new ArrayList<>();
        for (Entry<ProcessedOption, Dependency> entry : dependencies.entrySet()) {
            Dependency d = entry.getValue();
            if (!d.dependsOn.isEmpty()) {
                // check that it is not present as a dependency.
                boolean found = false;
                for (Entry<ProcessedOption, Dependency> e : dependencies.entrySet()) {
                    if (e.getValue().dependsOn.contains(d)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    options.add(d.option);
                }
            }
        }
        if (options.isEmpty()) {
            // we can return any...
            for (Entry<ProcessedOption, Dependency> e : dependencies.entrySet()) {
                options.add(e.getKey());
            }
        }
        // sort options.
        Collections.sort(options, (ProcessedOption o1, ProcessedOption o2) -> {
            String name = o1.name();
            // headers are last one in synopsis.
            if (name.equals("headers")) {
                return 1;
            }
            return o1.name().compareTo(o2.name());
        });
        return options.get(0);
    }

    private static String addSynopsisOption(ResourceBundle bundle,
            Map<ProcessedOption, Dependency> dependencies,
            String parentName,
            String commandName,
            List<String> superNames,
            ProcessedOption opt,
            boolean isOperation) {
        if (!dependencies.containsKey(opt)) {
            return null;
        }
        StringBuilder synopsisBuilder = new StringBuilder();
        // Do we have dependencies?
        Dependency dep = dependencies.remove(opt);
        boolean foundConflict = false;
        List<Dependency> sameGroup = new ArrayList<>();
        if (!dep.conflictWith.isEmpty()) {
            for (Dependency d : dep.conflictWith) {
                // If we have common dependencies with the conflict, must be added prior to the conflict.
                // For example:
                // A conflict with B and depends on C.
                // B conflict with A and depends on C.
                // The resulting must be C (A | B)
                for (Dependency dd : dep.dependsOn) {
                    if (d.dependsOn.contains(dd)) {
                        String content = addSynopsisOption(bundle, dependencies,
                                parentName, commandName, superNames, dd.option, isOperation);
                        if (content != null) {
                            synopsisBuilder.append(content.startsWith(" ") ? "" : " ");
                            synopsisBuilder.append(content);
                        }
                    }
                }
                if (dependencies.containsKey(d.option)) {
                    if (!foundConflict) {
                        synopsisBuilder.append(" (");
                    }
                    foundConflict = true;
                    /**
                     * So we have a conflict between A (dep) and B (d). A could
                     * depends on option C. B could be in conflict with C (or
                     * any option O that depends on C) This gives us: ( B | C
                     * A).
                     *
                     */
                    // Retrieve all the conflicts of B on which A depends (directly or
                    // indirectly and remove them from B.
                    List<Dependency> toRemove = retrieveDependedConflicts(dep, d);
                    d.conflictWith.removeAll(toRemove);

                    /**
                     * A conflict B. A depends on D. B conflict C. C depends on
                     * D. So conflict between B and C has been removed. C that
                     * is in conflict with B must be added after D in the same
                     * group as A. Such conflicts that share a dependency are
                     * added once conflicts have been treated.
                     */
                    for (Dependency removed : toRemove) {
                        if (!dep.dependsOn.contains(removed)) {
                            sameGroup.add(removed);
                        }
                    }
                    String content = addSynopsisOption(bundle, dependencies,
                            parentName, commandName, superNames, d.option, isOperation);
                    if (content != null) {
                        synopsisBuilder.append(content.startsWith(" ") ? "" : " ");
                        synopsisBuilder.append(content);
                    }
                }
            }
            if (foundConflict) {
                synopsisBuilder.append(" | ");
            }
        }

        if (!sameGroup.isEmpty()) {
            for (Dependency d : sameGroup) {
                String content = addSynopsisOption(bundle, dependencies,
                        parentName, commandName, superNames, d.option, isOperation);
                if (content != null) {
                    synopsisBuilder.append(content.startsWith(" ") ? "" : " ");
                    synopsisBuilder.append(content);
                }
            }
            synopsisBuilder.append(" ");
        }

        if (!dep.dependsOn.isEmpty()) {
            for (Dependency d : dep.dependsOn) {
                String content = addSynopsisOption(bundle, dependencies,
                        parentName, commandName, superNames, d.option, isOperation);
                if (content != null) {
                    synopsisBuilder.append(content.startsWith(" ") ? "" : " ");
                    synopsisBuilder.append(content);
                }
            }
            synopsisBuilder.append(" ");
        }
        if (!opt.isRequired()) {
            synopsisBuilder.append("[");
        }
        if (opt.name().equals("")) {
            String value = getValue(bundle, parentName, commandName, superNames, "arguments.value");
            synopsisBuilder.append(value == null ? "argument" : value);
        } else {
            if (isOperation) {
                synopsisBuilder.append(opt.name()).append("=");
            } else {
                synopsisBuilder.append("--").append(opt.name());
            }
            if (opt.hasValue()) {
                String val;
                if (isOperation) {
                    val = VALUES.get(opt.type());
                } else {
                    val = getValue(bundle, parentName, commandName, superNames, "option."
                            + opt.name() + ".value");
                    val = val == null ? VALUES.get(opt.type()) : val;
                    synopsisBuilder.append(" ");
                }

                // XXX JFDENISE< what about '='
                synopsisBuilder.append("<").append(val).append(">");
            }
        }
        if (!opt.isRequired()) {
            synopsisBuilder.append("]");
        }
        if (foundConflict) {
            synopsisBuilder.append(" ) ");
        }
        return synopsisBuilder.toString();
    }

    private static List<Dependency> retrieveDependedConflicts(Dependency option, Dependency conflict) {
        List<Dependency> conflicts = new ArrayList<>();
        List<Dependency> seen = new ArrayList<>();
        for (Dependency d : conflict.conflictWith) {
            if (d != option) { // the conflict is not the current option we can check it.
                if (option.dependsOn.contains(d)) {
                    conflicts.add(d);
                } else {
                    boolean found = findCommonDependency(option.dependsOn, d, seen);
                    if (found) {
                        conflicts.add(d);
                    }
                }
            }
        }
        return conflicts;
    }

    // Check that a given conflict depends on a dependency presents in the passed depends set.
    private static boolean findCommonDependency(Set<Dependency> depends, Dependency conflict, List<Dependency> seen) {
        if (seen.contains(conflict)) {
            return false;
        }
        seen.add(conflict);
        for (Dependency d : depends) {
            for (Dependency dc : conflict.dependsOn) {
                // Same dependency
                if (dc == d) {
                    return true;
                }
                boolean ret = findCommonDependency(depends, dc, seen);
                if (ret) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String printOptions(List<ProcessedOption> opts,
            ProcessedOption arg, boolean isOperation) {
        int width = 80;
        StringBuilder sb = new StringBuilder();
        if (opts.size() > 0) {
            sb.append(Config.getLineSeparator()).append("OPTIONS").append(Config.getLineSeparator());
            sb.append(Config.getLineSeparator());
        }
        // Retrieve the tab length
        int maxOptionName = 0;
        for (ProcessedOption o : opts) {
            String name = createOptionName(o.name(), o.name().length(), o.shortName(), isOperation);
            if (name.length() > maxOptionName) {
                maxOptionName = name.length();
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxOptionName; i++) {
            builder.append(" ");
        }
        String tab = builder.toString();
        for (ProcessedOption o : opts) {
            String name = createOptionName(o.name(), maxOptionName, o.shortName(), isOperation);
            sb.append(name);
            sb.append(HelpSupport.splitAndFormat(o.description(), width, "", name.length(), tab));
            sb.append(Config.getLineSeparator());
        }
        if (arg != null) {
            sb.append(Config.getLineSeparator()).append("ARGUMENT").append(Config.getLineSeparator());
            sb.append(Config.getLineSeparator());
            sb.append(HelpSupport.splitAndFormat(arg.description(), width, HelpSupport.TAB, 0, HelpSupport.TAB));
        }
        return sb.toString();
    }

    private static String createOptionName(String name, int maxOptionName, String shortName, boolean isOperation) {
        if (shortName != null) {
            name = name + " or (-" + shortName + ")";
        }
        StringBuilder builder = new StringBuilder(name);
        String prefix = isOperation ? "" : OPTION_PREFIX;
        while (builder.length() < (maxOptionName - TAB.length()
                - prefix.length() - OPTION_SUFFIX.length())) {
            builder.append(" ");
        }
        return TAB + prefix + builder.toString() + OPTION_SUFFIX;
    }

    private static String createActionName(String name, int maxOptionName) {
        StringBuilder builder = new StringBuilder(name);
        while (builder.length() < (maxOptionName - TAB.length()
                - OPTION_SUFFIX.length())) {
            builder.append(" ");
        }
        return HelpSupport.TAB + builder.toString() + OPTION_SUFFIX;
    }

    private static ResourceBundle getBundle(Command c) {
        Class<? extends Command> clazz = c.getClass();
        String s = clazz.getPackage().getName() + "." + "command_resources";
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle(s, Locale.getDefault(),
                    c.getClass().getClassLoader());
        } catch (MissingResourceException ex) {
            // Ok, will fallback on null.
        }
        return bundle;
    }

    private static String getValue(ResourceBundle bundle, String parentName,
            String commandName, List<String> superNames, String key) {
        if (bundle == null) {
            return null;
        }
        String value = null;
        try {
            String k = parentName == null ? commandName + "." + key : parentName + "." + commandName
                    + "." + key;
            value = bundle.getString(k);
        } catch (MissingResourceException ex) {
            //OK, try inherited option/arguments
            for (String superName : superNames) {
                try {
                    String k = parentName == null ? superName + "." + key : parentName + "." + superName
                            + "." + key;
                    value = bundle.getString(k);
                } catch (MissingResourceException ex2) {
                    // Ok, no key.
                    continue;
                }
                break;
            }
        }
        if (value != null) { // could be a reference to another option
            if (value.startsWith("${") && value.endsWith("}")) {
                String k = value.substring(2, value.length() - 1);
                try {
                    value = bundle.getString(k);
                } catch (MissingResourceException ex2) {
                    // Ok, missing key
                }
            }
        }
        return value;
    }

    private static ProcessedCommand retrieveDescriptions(ResourceBundle bundle,
            String parentName,
            ProcessedCommand<?> pc, List<String> superNames) {
        try {
            if (bundle == null) {
                return pc;
            }
            String desc = pc.description();
            String bdesc = getValue(bundle, parentName, pc.name(), superNames, "description");
            if (bdesc != null) {
                desc = bdesc;
            }
            ProcessedCommandBuilder builder = new ProcessedCommandBuilder().name(pc.name()).description(desc);

            if (pc.getArgument() != null) {
                String argDesc = pc.getArgument().description();
                String bargDesc = getValue(bundle, parentName, pc.name(), superNames, "arguments.description");
                if (bargDesc != null) {
                    argDesc = bargDesc;
                }
                ProcessedOption newArg = ProcessedOptionBuilder.builder().name("").
                        optionType(pc.getArgument().getOptionType()).
                        type(String.class).
                        activator(pc.getArgument().activator()).
                        valueSeparator(pc.getArgument().getValueSeparator()).
                        required(pc.getArgument().isRequired()).description(argDesc).build();
                builder.argument(newArg);
            }

            for (ProcessedOption opt : pc.getOptions()) {
                String optDesc = opt.description();
                String boptDesc = getValue(bundle, parentName, pc.name(), superNames, "option." + opt.name() + ".description");
                if (boptDesc != null) {
                    optDesc = boptDesc;
                }
                ProcessedOption newOption = ProcessedOptionBuilder.builder().name(opt.name()).
                        optionType(opt.getOptionType()).
                        type(String.class).
                        activator(opt.activator()).
                        valueSeparator(opt.getValueSeparator()).
                        shortName(opt.shortName() == null ? 0 : opt.shortName().charAt(0)).
                        required(opt.isRequired()).
                        description(optDesc).build();
                builder.addOption(newOption);
            }
            return builder.create();
        } catch (Exception ex) {
            Logger.getLogger(HelpSupport.class).warn("Error building description " + ex);
            // fallback to ProcessedCommand.
        }
        return pc;
    }

    private static String extractFirstLine(String content, int width) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        if (content.length() <= width) {
            return content;
        }
        String line = null;
        StringBuilder builder = new StringBuilder();
        for (char c : content.toCharArray()) {
            if (c == '\n') {
                line = builder.toString();
                line = removeLastBlanks(line);
                break;
            } else {
                builder.append(c);
                if (builder.length() == width) {
                    line = builder.toString();
                    if (!line.endsWith(" ")) { // Need to truncate after the last ' '
                        line = removeLastBlanks(line);
                        int index = line.lastIndexOf(" ");
                        index = index < 0 ? 0 : index;
                        line = line.substring(0, index);
                        break;
                    } else {
                        line = builder.toString();
                        line = removeLastBlanks(line);
                        break;
                    }
                }
            }
        }
        return line;
    }

    private static String splitAndFormat(String content, int width, String firstTab, int firstOffset, String otherTab) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        if (content.length() <= width - firstTab.length() - firstOffset) {
            return firstTab + content + "\n";
        }
        StringBuilder builder = new StringBuilder();

        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (char c : content.toCharArray()) {
            if (c == '\n') {
                builder.append(first ? firstTab : otherTab);
                String line = b.toString();
                line = removeLastBlanks(line);
                builder.append(line);
                builder.append("\n");
                b = new StringBuilder();
                first = false;
            } else {
                b.append(c);
                String tab = first ? firstTab : otherTab;
                if (b.length() == width - tab.length() - (first ? firstOffset : 0)) {
                    builder.append(tab);
                    String line = b.toString();
                    if (!line.endsWith(" ")) { // Need to truncate after the last ' '
                        line = removeLastBlanks(line);
                        int index = line.lastIndexOf(" ");
                        index = index < 0 ? 0 : index;
                        String remain = line.substring(index);
                        remain = remain.trim();
                        builder.append(line.substring(0, index));
                        builder.append("\n");
                        b = new StringBuilder();
                        b.append(remain);
                        first = false;
                    } else {
                        builder.append(removeLastBlanks(line));
                        builder.append("\n");
                        b = new StringBuilder();
                        first = false;
                    }
                }
            }
        }

        if (b.length() > 0) {
            builder.append(first ? firstTab : otherTab);
            String line = b.toString();
            line = removeLastBlanks(line);
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    private static String removeLastBlanks(String line) {
        int num = 0;
        for (int i = line.length() - 1; i > 0; i--) {
            if (line.charAt(i) == ' ') {
                num += 1;
            } else {
                break;
            }
        }
        return line.substring(0, line.length() - num);
    }

    private static class Dependency {

        private ProcessedOption option;
        private final Set<Dependency> dependsOn = new HashSet<>();
        private final Set<Dependency> conflictWith = new HashSet<>();

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Dependency)) {
                return false;
            }
            Dependency dep = (Dependency) other;
            return option.name().equals(dep.option.name());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 13 * hash + Objects.hashCode(this.option.name());
            return hash;
        }
    }

    private static Map<ProcessedOption, Dependency> retrieveDependencies(List<ProcessedOption> opts,
            ProcessedOption arg, boolean domain) {
        Map<ProcessedOption, Dependency> dependencies = new IdentityHashMap<>();
        if (arg != null) {
            List<ProcessedOption> argExpected = retrieveExpected(arg.activator(),
                    opts, null, domain);
            Dependency d = new Dependency();
            d.option = arg;
            dependencies.put(arg, d);
            for (ProcessedOption e : argExpected) {
                Dependency de = new Dependency();
                de.option = e;
                dependencies.put(e, de);
                d.dependsOn.add(de);
            }
            List<ProcessedOption> argNotExpected = retrieveNotExpected(arg.activator(),
                    opts, null, domain);
            for (ProcessedOption e : argNotExpected) {
                Dependency depDep = dependencies.get(e);
                if (depDep == null) {
                    depDep = new Dependency();
                    depDep.option = e;
                    dependencies.put(e, depDep);
                }
                depDep.option = e;
                dependencies.put(e, depDep);
                d.conflictWith.add(depDep);
                depDep.conflictWith.add(d);
            }
        }
        for (ProcessedOption opt : opts) {
            List<ProcessedOption> expected = retrieveExpected(opt.activator(), opts, arg, domain);
            Dependency optDep = dependencies.get(opt);
            if (optDep == null) {
                optDep = new Dependency();
                optDep.option = opt;
                dependencies.put(opt, optDep);
            }
            for (ProcessedOption e : expected) {
                Dependency depDep = dependencies.get(e);
                if (depDep == null) {
                    depDep = new Dependency();
                    depDep.option = e;
                    dependencies.put(e, depDep);
                }
                optDep.dependsOn.add(depDep);
            }
            List<ProcessedOption> notExpected = retrieveNotExpected(opt.activator(), opts, arg, domain);
            for (ProcessedOption e : notExpected) {
                Dependency depDep = dependencies.get(e);
                if (depDep == null) {
                    depDep = new Dependency();
                    depDep.option = e;
                    dependencies.put(e, depDep);
                }
                optDep.conflictWith.add(depDep);
                depDep.conflictWith.add(optDep);
            }
        }
        return dependencies;
    }

    private static List<ProcessedOption> retrieveNotExpected(OptionActivator activator, List<ProcessedOption> opts, ProcessedOption arg, boolean domain) {
        List<ProcessedOption> notExpected = new ArrayList<>();
        if (activator == null) {
            return notExpected;
        }
        if (activator instanceof NotExpectedOptionsActivator) {
            for (String s : ((NotExpectedOptionsActivator) activator).getNotExpected()) {
                if (s == null || s.equals("")) { // argument
                    if (arg != null) {
                        if (isDomainCompliant(arg, domain)) {
                            notExpected.add(arg);
                        }
                    }
                } else {
                    for (ProcessedOption opt : opts) {
                        if (s.equals(opt.name())) {
                            if (isDomainCompliant(opt, domain)) {
                                notExpected.add(opt);
                            }
                        }
                    }
                }
            }
        }
        return notExpected;
    }

    private static boolean isDomainCompliant(ProcessedOption arg, boolean domain) {
        if (domain) {
            // This option is only valid in non domain mode.
            if (arg.activator() instanceof StandaloneOptionActivator) {
                return false;
            }
        } else // This option is only valid in non domain mode.
        {
            if (arg.activator() instanceof DomainOptionActivator) {
                return false;
            }
        }
        return true;
    }

    private static List<ProcessedOption> retrieveExpected(OptionActivator activator, List<ProcessedOption> opts, ProcessedOption arg, boolean domain) {
        List<ProcessedOption> expected = new ArrayList<>();
        if (activator == null) {
            return expected;
        }
        try {
            String[] obj = (String[]) activator.getClass().getField(WF_CLI_EXPECTED_OPTIONS).get(null);
            for (String s : obj) {
                if (s == null || s.equals("")) { // argument
                    if (arg != null) {
                        if (isDomainCompliant(arg, domain)) {
                            expected.add(arg);
                        }
                    }
                } else {
                    for (ProcessedOption opt : opts) {
                        if (s.equals(opt.name())) {
                            if (isDomainCompliant(opt, domain)) {
                                expected.add(opt);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // XXX OK, no field.
        }

        if (activator instanceof ExpectedOptionsActivator) {
            for (String s : ((ExpectedOptionsActivator) activator).getExpected()) {
                if (s == null || s.equals("")) { // argument
                    if (arg != null) {
                        if (isDomainCompliant(arg, domain)) {
                            expected.add(arg);
                        }
                    }
                } else {
                    for (ProcessedOption opt : opts) {
                        if (s.equals(opt.name())) {
                            if (isDomainCompliant(opt, domain)) {
                                expected.add(opt);
                            }
                        }
                    }
                }
            }
        }
        return expected;
    }

    private static List<ProcessedOption> retrieveStandaloneOptions(List<ProcessedOption> opts) {
        List<ProcessedOption> standalone = new ArrayList<>();
        for (ProcessedOption opt : opts) {
            if (!(opt.activator() instanceof DomainOptionActivator)) {
                standalone.add(opt);
            }
        }
        return standalone;
    }

    private static List<ProcessedOption> retrieveDomainOptions(List<ProcessedOption> opts) {
        List<ProcessedOption> domain = new ArrayList<>();
        for (ProcessedOption opt : opts) {
            if ((opt.activator() instanceof DomainOptionActivator
                    || !(opt.activator() instanceof StandaloneOptionActivator))) {
                domain.add(opt);
            }
        }
        return domain;
    }
}
