/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.impl.aesh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.wildfly.core.cli.command.aesh.activator.DefaultExpectedAndNotExpectedOptionsActivator;
import org.wildfly.core.cli.command.aesh.activator.DefaultExpectedOptionsActivator;
import org.wildfly.core.cli.command.aesh.activator.DefaultNotExpectedOptionsActivator;
import org.wildfly.core.cli.command.aesh.activator.HiddenActivator;

/**
 * A bunch of commands to test for help.
 *
 * @author jdenise@redhat.com
 */
public class Commands {

    static List<Class<? extends Command>> TESTS_STANDALONE = new ArrayList<>();

    static {
        for (Class<?> clazz : Commands.Standalone.class.getDeclaredClasses()) {
            TESTS_STANDALONE.add((Class<? extends Command>) clazz);
        }
    }

    /**
     * Commands that don't have domain dependent options
     */
    public static class Standalone {

        /**
         * Simplest command.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command0 implements TestCommand, Command {

            @Override
            public String getSynopsis() {
                return "command1";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with options.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command1 implements TestCommand, Command {

            @Option(name = "opt1-with-value", hasValue = true)
            String opt1;
            @Option(name = "opt2-without-value", hasValue = false)
            String opt2;
            @Option(name = "opt3-with-value", hasValue = true)
            String opt3;
            @Arguments()
            List<String> args;

            @Override
            public String getSynopsis() {
                return "command1 [argument] [--opt1-with-value <a string>] [--opt2-without-value] [--opt3-with-value <a string>]";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with required option.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command2 implements TestCommand, Command {

            @Option(name = "opt1-with-value", hasValue = true)
            String opt1;
            @Option(name = "opt2-without-value", hasValue = false, required = true)
            String opt2;
            @Option(name = "opt3-with-value", hasValue = true, required = true)
            String opt3;
            @Arguments()
            List<String> args;

            @Override
            public String getSynopsis() {
                return "command1 --opt2-without-value --opt3-with-value <a string> [argument] [--opt1-with-value <a string>]";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with simple conflict between 2 options.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command3 implements TestCommand, Command {

            public static class Opt3Activator extends DefaultNotExpectedOptionsActivator {

                public Opt3Activator() {
                    super("opt2-without-value");
                }
            };
            @Option(name = "opt1-with-value", hasValue = true)
            String opt1;
            @Option(name = "opt2-without-value", hasValue = false, required = true)
            String opt2;
            @Option(name = "opt3-with-value", hasValue = true, required = true, activator = Opt3Activator.class)
            String opt3;

            @Override
            public String getSynopsis() {
                return "command1 ( --opt3-with-value <a string> | --opt2-without-value ) [--opt1-with-value <a string>]";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with simple conflict between 2 options that depend onto the
         * same option.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command4 implements TestCommand, Command {

            public static class Opt2Activator extends DefaultExpectedOptionsActivator {

                public Opt2Activator() {
                    super("opt1-with-value");
                }
            };

            public static class Opt3Activator extends DefaultExpectedAndNotExpectedOptionsActivator {

                private static final Set<String> EXPECTED = new HashSet<>();
                private static final Set<String> NOT_EXPECTED = new HashSet<>();

                static {
                    // Argument.
                    EXPECTED.add("opt1-with-value");
                    NOT_EXPECTED.add("opt2-without-value");
                }

                public Opt3Activator() {
                    super(EXPECTED, NOT_EXPECTED);
                }
            }
            @Option(name = "opt1-with-value", hasValue = true)
            String opt1;
            @Option(name = "opt2-without-value", hasValue = false, required = true, activator = Opt2Activator.class)
            String opt2;
            @Option(name = "opt3-with-value", hasValue = true, required = true, activator = Opt3Activator.class)
            String opt3;

            @Override
            public String getSynopsis() {
                return "command1 [--opt1-with-value <a string>] ( --opt3-with-value <a string> | --opt2-without-value )";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with simple conflict between 2 options that depend onto the
         * argument.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command5 implements TestCommand, Command {

            public static class Opt2Activator extends DefaultExpectedOptionsActivator {

                public Opt2Activator() {
                    super("");
                }
            };

            public static class Opt3Activator extends DefaultExpectedAndNotExpectedOptionsActivator {

                private static final Set<String> EXPECTED = new HashSet<>();
                private static final Set<String> NOT_EXPECTED = new HashSet<>();

                static {
                    // Argument.
                    EXPECTED.add("");
                    NOT_EXPECTED.add("opt2-without-value");
                }

                public Opt3Activator() {
                    super(EXPECTED, NOT_EXPECTED);
                }
            }
            @Option(name = "opt1-with-value", hasValue = true)
            String opt1;
            @Option(name = "opt2-without-value", hasValue = false, required = true, activator = Opt2Activator.class)
            String opt2;
            @Option(name = "opt3-with-value", hasValue = true, required = true, activator = Opt3Activator.class)
            String opt3;
            @Arguments()
            List<String> args;

            @Override
            public String getSynopsis() {
                return "command1 [argument] ( --opt3-with-value <a string> | --opt2-without-value ) [--opt1-with-value <a string>]";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with simple conflict between 2 options that depend onto the
         * argument and an option.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command6 implements TestCommand, Command {

            public static class Opt2Activator extends DefaultExpectedOptionsActivator {

                public Opt2Activator() {
                    super("", "opt1-with-value");
                }
            };

            public static class Opt3Activator extends DefaultExpectedAndNotExpectedOptionsActivator {

                private static final Set<String> EXPECTED = new HashSet<>();
                private static final Set<String> NOT_EXPECTED = new HashSet<>();

                static {
                    // Argument.
                    EXPECTED.add("");
                    EXPECTED.add("opt1-with-value");
                    NOT_EXPECTED.add("opt2-without-value");
                }

                public Opt3Activator() {
                    super(EXPECTED, NOT_EXPECTED);
                }
            }
            @Option(name = "opt1-with-value", hasValue = true)
            String opt1;
            @Option(name = "opt2-without-value", hasValue = false, required = true, activator = Opt2Activator.class)
            String opt2;
            @Option(name = "opt3-with-value", hasValue = true, required = true, activator = Opt3Activator.class)
            String opt3;
            @Arguments()
            List<String> args;

            @Override
            public String getSynopsis() {
                return "command1 [argument] [--opt1-with-value <a string>] ( --opt3-with-value <a string> | --opt2-without-value )";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with conflict between an option and a 2 other options and the
         * argument. The 2 other options depend on the argument. That is similar
         * to Patch info.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command7 implements TestCommand, Command {

            public static class NoStreamsActivator extends DefaultNotExpectedOptionsActivator {

                public NoStreamsActivator() {
                    super("streams");
                }
            };

            public static class NoPatchIdActivator extends DefaultNotExpectedOptionsActivator {

                public NoPatchIdActivator() {
                    super("");
                }
            };

            public static class PatchIdNoStreamsActivator extends DefaultExpectedAndNotExpectedOptionsActivator {

                private static final Set<String> EXPECTED = new HashSet<>();
                private static final Set<String> NOT_EXPECTED = new HashSet<>();

                static {
                    // Argument.
                    EXPECTED.add("");
                    NOT_EXPECTED.add("streams");
                }

                public PatchIdNoStreamsActivator() {
                    super(EXPECTED, NOT_EXPECTED);
                }
            }

            @Option(name = "patch-stream", hasValue = true, required = false, activator = PatchIdNoStreamsActivator.class)
            private String patchStream;

            @Arguments(activator = NoStreamsActivator.class)
            private List<String> patchIdArg;

            @Option(hasValue = false, shortName = 'v', required = false, activator = PatchIdNoStreamsActivator.class)
            boolean verbose;

            @Option(hasValue = false, required = false, activator = NoPatchIdActivator.class)
            boolean streams;

            @Override
            public String getSynopsis() {
                return "command1 ( [--streams] | [argument] [--verbose] [--patch-stream <a string>] )";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with hidden options and arguments
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command8 implements TestCommand, Command {

            @Option(name = "opt1", hasValue = true, required = false, activator = HiddenActivator.class)
            private String patchStream;

            @Arguments(activator = HiddenActivator.class)
            private List<String> patchIdArg;

            @Option(hasValue = false, shortName = 'v', required = false, activator = HiddenActivator.class)
            boolean verbose;

            @Option(hasValue = false, required = false, activator = HiddenActivator.class)
            boolean streams;

            @Override
            public String getSynopsis() {
                return "command1";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        /**
         * Command with conflict between 2 groups of 2 options. Each group
         * composed of an option that depends on another one.
         *
         * @author jdenise@redhat.com
         */
        @CommandDefinition(name = "command1", description = "")
        public static class Command9 implements TestCommand, Command {

            public static class Opt3Activator extends DefaultNotExpectedOptionsActivator {

                public Opt3Activator() {
                    super("opt2-depends-on-opt1-conflict-with-opt4");
                }
            };

            public static class Opt4Activator extends DefaultExpectedAndNotExpectedOptionsActivator {

                private static final Set<String> EXPECTED = new HashSet<>();
                private static final Set<String> NOT_EXPECTED = new HashSet<>();

                static {
                    EXPECTED.add("opt3-conflict-with-opt2");
                    NOT_EXPECTED.add("opt2-depends-on-opt1-conflict-with-opt4");
                }

                public Opt4Activator() {
                    super(EXPECTED, NOT_EXPECTED);
                }
            }

            public static class Opt1Activator extends DefaultNotExpectedOptionsActivator {

                public Opt1Activator() {
                    super("opt4-depends-on-opt3-conflict-with-opt2");
                }
            };


            public static class Opt2Activator extends DefaultExpectedAndNotExpectedOptionsActivator {

                private static final Set<String> EXPECTED = new HashSet<>();
                private static final Set<String> NOT_EXPECTED = new HashSet<>();

                static {
                    EXPECTED.add("opt1-conflict-with-opt4");
                    NOT_EXPECTED.add("opt4-depends-on-opt3-conflict-with-opt2");
                }

                public Opt2Activator() {
                    super(EXPECTED, NOT_EXPECTED);
                }
            }
            @Option(name = "opt1-conflict-with-opt4", hasValue = false, activator = Opt1Activator.class)
            String opt1;
            @Option(name = "opt2-depends-on-opt1-conflict-with-opt4", hasValue = false, activator = Opt2Activator.class)
            String opt2;
            @Option(name = "opt3-conflict-with-opt2", hasValue = false, activator = Opt3Activator.class)
            String opt3;
            @Option(name = "opt4-depends-on-opt3-conflict-with-opt2", hasValue = false, activator = Opt4Activator.class)
            String opt4;

            @Override
            public String getSynopsis() {
                return "command1 ( [--opt3-conflict-with-opt2] [--opt4-depends-on-opt3-conflict-with-opt2] | "
                        + "[--opt1-conflict-with-opt4] [--opt2-depends-on-opt1-conflict-with-opt4] )";
            }

            @Override
            public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }
    }
}
