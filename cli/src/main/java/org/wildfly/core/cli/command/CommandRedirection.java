package org.wildfly.core.cli.command;

import java.util.List;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.parser.AeshLine;

/**
 * Implement workflow.
 *
 * @author jdenise
 */
public interface CommandRedirection {

    interface Registration {

        boolean isActive();

        void unregister() throws CommandException;

        CommandRedirection getRedirection();
    }

    List<String> getRedirectionCommands();

    default void handle(CliCommandContext ctx, AeshLine line) throws CommandException {
        if (getRedirectionCommands().contains(line.getWords().get(0))
                || line.getWords().contains("--help")
                || line.getWords().contains("-h")) {
            ctx.executeCommand(line.getOriginalInput());
            return;
        }
        addCommand(ctx, line);
    }

    void addCommand(CliCommandContext ctx, AeshLine line);

    void set(Registration registration);
}
