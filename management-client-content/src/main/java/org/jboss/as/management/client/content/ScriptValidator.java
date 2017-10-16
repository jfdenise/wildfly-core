/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.management.client.content;

import java.util.List;
import org.jboss.as.controller.OperationFailedException;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLI;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMMANDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSOLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DMR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LEVEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCRIPT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STANDALONE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAGS;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.Assert;

/**
 *
 * @author jdenise@redhat.com
 */
public class ScriptValidator extends AbstractParameterValidator {

    @Override
    public void validateParameter(String parameterName, ModelNode script) throws OperationFailedException {
        Assert.assertNotNull(script);
        if (!script.hasDefined(COMMANDS)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, COMMANDS, script.toString()));
        }
        if (!script.hasDefined(DESCRIPTION)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, DESCRIPTION, script.toString()));
        }
        if (!script.hasDefined(TAGS)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, TAGS, script.toString()));
        }
        if (!script.hasDefined(MODE)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, MODE, script.toString()));
        }
        if (!script.hasDefined(LEVEL)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, LEVEL, script.toString()));
        }
        List<ModelNode> commands = script.get(COMMANDS).asList();
        if (commands.isEmpty()) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, COMMANDS, script.toString()));
        }
        List<ModelNode> tags = script.get(TAGS).asList();
        if (tags.isEmpty()) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(SCRIPT, TAGS, script.toString()));
        }
        String level = script.get(LEVEL).asString();
        if (!level.equals(CLI) && !level.equals(DMR) && !level.equals(CONSOLE)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.invalidValue(level,
                    DMR + ", " + CLI + ", " + CONSOLE + " are supported"));
        }
        String mode = script.get(MODE).asString();
        if (!mode.equals(STANDALONE) && !mode.equals(DOMAIN)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.invalidValue(mode,
                    STANDALONE + ", " + DOMAIN + " are supported"));
        }
    }
}
