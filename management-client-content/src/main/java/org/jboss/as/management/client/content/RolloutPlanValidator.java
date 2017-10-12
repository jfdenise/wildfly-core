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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jboss.as.controller.OperationFailedException;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.Assert;

/**
 *
 * @author jdenise@redhat.com
 */
public class RolloutPlanValidator extends AbstractParameterValidator {

    private static final List<String> ALLOWED_SERVER_GROUP_CHILDREN = Arrays.asList(ROLLING_TO_SERVERS, MAX_FAILURE_PERCENTAGE, MAX_FAILED_SERVERS);

    @Override
    public void validateParameter(String parameterName, ModelNode plan) throws OperationFailedException {
        Assert.assertNotNull(plan);
        if (!plan.hasDefined(ROLLOUT_PLAN)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(ROLLOUT_PLAN, ROLLOUT_PLAN, plan.toString()));
        }
        ModelNode rolloutPlan1 = plan.get(ROLLOUT_PLAN);

        final Set<String> keys;
        try {
            keys = rolloutPlan1.keys();
        } catch (IllegalArgumentException e) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
        }
        if (!keys.contains(IN_SERIES)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
        }
        if (keys.size() > 2 || keys.size() == 2 && !keys.contains(ROLLBACK_ACROSS_GROUPS)) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.unrecognizedChildren(ROLLOUT_PLAN, IN_SERIES + ", " + ROLLBACK_ACROSS_GROUPS, plan.toString()));
        }

        final ModelNode inSeries = rolloutPlan1.get(IN_SERIES);
        if (!inSeries.isDefined()) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
        }

        final List<ModelNode> groups = inSeries.asList();
        if (groups.isEmpty()) {
            throw new OperationFailedException(ClientContentLogger.LOGGER.inSeriesIsMissingGroups(plan.toString()));
        }

        for (ModelNode group : groups) {
            if (group.hasDefined(SERVER_GROUP)) {
                final ModelNode serverGroup = group.get(SERVER_GROUP);
                final Set<String> groupKeys;
                try {
                    groupKeys = serverGroup.keys();
                } catch (IllegalArgumentException e) {
                    throw new OperationFailedException(ClientContentLogger.LOGGER.serverGroupExpectsSingleChild(plan.toString()));
                }
                if (groupKeys.size() != 1) {
                    throw new OperationFailedException(ClientContentLogger.LOGGER.serverGroupExpectsSingleChild(plan.toString()));
                }
                validateInSeriesServerGroup(serverGroup.asProperty().getValue());
            } else if (group.hasDefined(CONCURRENT_GROUPS)) {
                final ModelNode concurrent = group.get(CONCURRENT_GROUPS);
                for (ModelNode child : concurrent.asList()) {
                    validateInSeriesServerGroup(child.asProperty().getValue());
                }
            } else {
                throw new OperationFailedException(ClientContentLogger.LOGGER.unexpectedInSeriesGroup(plan.toString()));
            }
        }
    }

    public static void validateInSeriesServerGroup(ModelNode serverGroup) throws OperationFailedException {
        if (serverGroup.isDefined()) {
            try {
                final Set<String> specKeys = serverGroup.keys();
                if (!ALLOWED_SERVER_GROUP_CHILDREN.containsAll(specKeys)) {
                    throw new OperationFailedException(ClientContentLogger.LOGGER.unrecognizedChildren(SERVER_GROUP, ALLOWED_SERVER_GROUP_CHILDREN.toString(), specKeys.toString()));
                }
            } catch (IllegalArgumentException e) {// ignore?
            }
        }
    }
}
