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
package org.jboss.as.cli.parsing.operation;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.parsing.ParsingContext;


/**
 *
 * @author jfdenise
 */
public class NodeListState extends DefaultParsingState {

    public static final NodeListState INSTANCE = new NodeListState();
    public static final String ID = "NODE_LIST";

    NodeListState() {
        this(NodeState.INSTANCE);
    }

    NodeListState(final NodeState nodeState) {
        super(ID);
        putHandler(':', GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
        setDefaultHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                ctx.enterState(nodeState);
            }
        });
        setEnterHandler(new CharacterHandler(){

            @Override
            public void handle(ParsingContext ctx)
                    throws CommandFormatException {
                ctx.enterState(nodeState);
            }
        });
        setReturnHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                if(ctx.getCharacter() == ':') {
                    GlobalCharacterHandlers.LEAVE_STATE_HANDLER.handle(ctx);
                }
            }});
        setIgnoreWhitespaces(true);
    }
}
