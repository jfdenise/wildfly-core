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
package org.jboss.as.cli.parsing.operation;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.EnterStateCharacterHandler;
import org.jboss.as.cli.parsing.ExpressionBaseState;
import org.jboss.as.cli.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.WordCharacterHandler;


/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyState extends ExpressionBaseState {

    /**
     * This state monitors the current location in order to identify the case
     * where the end of content is reached although no Property value has been
     * provided. When the end of content is detected, then the
     * PropertyValueGenerator
     */
    private class PropertyDefaultState extends WordCharacterHandler {

        public PropertyDefaultState() {
            super(false, false);
        }

        @Override
        public void doHandle(ParsingContext ctx) throws CommandFormatException {
            super.doHandle(ctx);
            // Last character and no '=' separator, this is an implicit value
            if (ctx.getLocation() == ctx.getInput().length() - 1) {
                ctx.enterState(valueGenerator);
            }
        }
    }

    private final PropertyValueGeneratorState valueGenerator;
    private final NameValueSeparatorState separator;
    public static final PropertyState INSTANCE = new PropertyState();
    public static final String ID = "PROP";

    PropertyState() {
        this(PropertyValueState.INSTANCE);
    }

    PropertyState(PropertyValueState valueState) {
        this(true, ',', valueState, ')');
    }

    PropertyState(boolean isOperation, char propSeparator, char... listEnd) {
        this(isOperation, propSeparator,
                new PropertyValueState(propSeparator, listEnd), listEnd);
    }

    PropertyState(boolean isOperation, char propSeparator,
            PropertyValueState valueState, char... listEnd) {
        super(ID);
        valueGenerator = new PropertyValueGeneratorState(valueState);
        separator = new NameValueSeparatorState(valueState);
        setIgnoreWhitespaces(true);
        setEnterHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                WordCharacterHandler.IGNORE_LB_ESCAPE_OFF.handle(ctx);
            }
        });

        for (int i = 0; i < listEnd.length; ++i) {
            if (isOperation) {
                // Reaching a terminal character means that we have reached
                // the end of a property without having reached a value,
                // we need to generate a value.
                enterState(listEnd[i], valueGenerator);
            } else { // This is another usage of Property (e.g.:RolloutPlan)
                putHandler(listEnd[i],
                        GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
            }
        }
        enterState('=', separator);
        if (isOperation) {
            enterState(propSeparator, valueGenerator);
        }
        setDefaultHandler(isOperation ? new PropertyDefaultState()
                : WordCharacterHandler.IGNORE_LB_ESCAPE_OFF);
        setReturnHandler(GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
    }

    private static class NameValueSeparatorState extends DefaultParsingState {
        NameValueSeparatorState(PropertyValueState valueState) {
            super("NAME_VALUE_SEPARATOR");
            setDefaultHandler(new EnterStateCharacterHandler(valueState));
            setReturnHandler(GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
            setIgnoreWhitespaces(true);
        }
    };

    /**
     * This state generates a value for a property that has no value.
     */
    private class PropertyValueGeneratorState extends DefaultParsingState {

        PropertyValueGeneratorState(PropertyValueState valueState) {
            super("PROPERTY_VALUE_GENERATOR");
            setEnterHandler(new CharacterHandler() {
                @Override
                public void handle(ParsingContext ctx)
                        throws CommandFormatException {
                    // Compute a new input with the proper name=value syntax.
                    ctx.updateInput((context) -> {
                        StringBuilder builder = new StringBuilder();
                        char ch = context.getCharacter();
                        // We have found a terminal character.
                        // Otherwise this is end of content
                        boolean isEndOfValue = ch == ',' || ch == ')';
                        // If end of content is detected 1 char prior to actual
                        // end of content, need to move the current location by 1.
                        int offset = isEndOfValue ? 0 : 1;
                        int location = isEndOfValue ? context.getLocation()
                                : context.getLocation() + offset;
                        builder.append(context.getInput().substring(0, location));
                        builder.append("=");
                        builder.append(getImplicitValue(context));
                        builder.append(context.getInput().substring(location));
                        // offset is 0 when terminal charecter found, otherwise 1.
                        context.advanceLocation(offset);

                        return builder.toString();
                    });
                    ctx.enterState(separator);
                }
            });
            setReturnHandler(GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
        }
    };

    private String getImplicitValue(ParsingContext ctx) {
        // Only Boolean for now
        return "true";
    }
}
