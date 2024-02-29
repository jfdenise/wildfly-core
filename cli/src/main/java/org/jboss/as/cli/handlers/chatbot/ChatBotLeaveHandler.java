/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.cli.handlers.chatbot;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;

/**
 *
 * @author jfdenise
 */
public class ChatBotLeaveHandler extends CommandHandlerWithHelp {

    public ChatBotLeaveHandler() {
        super("bye", false);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return ChatBotControlFlow.get(ctx) != null;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        final ChatBotControlFlow forCF = ChatBotControlFlow.get(ctx);
        if (forCF == null) {
            throw new CommandLineException("bye! is not available outside 'chat'");
        }
        ctx.print("Bye! Bye!");
        forCF.run(ctx);
    }
}
