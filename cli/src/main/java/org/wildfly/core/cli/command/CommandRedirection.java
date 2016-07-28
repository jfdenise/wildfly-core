package org.wildfly.core.cli.command;

import java.util.List;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.parser.AeshLine;

/**
 * Implement workflow.
 *
 * @author jdenise
 */
public abstract class CommandRedirection {

    private Registration registration;

    public interface Registration {

        boolean isActive();

        void unregister() throws CommandException;

        CommandRedirection getRedirection();
    }

    public abstract List<String> getRedirectionCommands();

    public void handle(CliCommandContext ctx, AeshLine line) throws CommandException {
        if (getRedirectionCommands().contains(line.getWords().get(0))
                || line.getWords().contains("--help")
                || line.getWords().contains("-h")) {
            ctx.executeCommand(line.getOriginalInput());
            return;
        }
        addCommand(ctx, line);
    }

    public abstract void addCommand(CliCommandContext ctx, AeshLine line);

    public void set(Registration registration) {
        this.registration = registration;
    }

    public Registration getRegistration() {
        return registration;
    }
}
