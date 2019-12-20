/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.parsing.arguments;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.BackQuotesState;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.ParsingStaticClearer;
import org.jboss.as.cli.parsing.QuotesState;
import org.jboss.as.cli.parsing.WordCharacterHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public class NonObjectArgumentValueState extends DefaultParsingState {

    public static final String ID = ArgumentValueState.ID;

    public static NonObjectArgumentValueState INSTANCE = new NonObjectArgumentValueState();

    static {
        ParsingStaticClearer.add(NonObjectArgumentValueState.class);
    }
    public static void staticClear() {
        INSTANCE = null;
    }
    public NonObjectArgumentValueState() {
        super(ID);
        setEnterHandler(new CharacterHandler() {
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                final char ch = ctx.getCharacter();
                switch(ch) {
                    case '"':
                        ctx.enterState(QuotesState.QUOTES_EXCLUDED);
                        break;
                    case '$':
                        ctx.enterState(ExpressionValueState.INSTANCE);
                        break;
                    case '`':
                        ctx.enterState(BackQuotesState.QUOTES_INCLUDED);
                        break;
                    default:
                        ctx.getCallbackHandler().character(ctx);
                }
            }});
        setDefaultHandler(WordCharacterHandler.IGNORE_LB_ESCAPE_ON);
        enterState('"', QuotesState.QUOTES_INCLUDED);
        enterState('`', BackQuotesState.QUOTES_INCLUDED);
        enterState('$', ExpressionValueState.INSTANCE);
    }
}
