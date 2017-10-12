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

import java.io.InvalidObjectException;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * @author jdenise@redhat.com
 */
@SuppressWarnings("deprecation")
@MessageLogger(projectCode = "WFLYSRV", length = 4)
public interface ClientContentLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ClientContentLogger LOGGER = Logger.getMessageLogger(ClientContentLogger.class, "org.jboss.as.management.client.content");

    @Message(id = Message.NONE, value = "%s is not valid. %s")
    InvalidObjectException invalidValue(String field, String expected);

    /**
     * Creates an exception message indicating that a parent is missing a
     * required child.
     *
     * @param parent the name of the parent element
     * @param child the name of the missing child element
     * @param parentSpec the complete string representation of the parent
     * element
     *
     * @return the error message
     */
    @Message(id = 16, value = "%s is missing %s: %s")
    String requiredChildIsMissing(String parent, String child, String parentSpec);

    /**
     * Creates an exception message indicating that a parent recognizes only the
     * specified children.
     *
     * @param parent the name of the parent element
     * @param children recognized children
     * @param parentSpec the complete string representation of the parent
     * element
     *
     * @return the error message
     */
    @Message(id = 17, value = "%s recognizes only %s as children: %s")
    String unrecognizedChildren(String parent, String children, String parentSpec);

    /**
     * Creates an exception message indicating that in-series is missing groups.
     *
     * @param rolloutPlan string representation of a rollout plan
     *
     * @return the error message
     */
    @Message(id = 18, value = IN_SERIES + " is missing groups: %s")
    String inSeriesIsMissingGroups(String rolloutPlan);

    /**
     * Creates an exception message indicating that server-group expects one and
     * only one child.
     *
     * @param rolloutPlan string representation of a rollout plan
     *
     * @return the error message
     */
    @Message(id = 19, value = SERVER_GROUP + " expects one and only one child: %s")
    String serverGroupExpectsSingleChild(String rolloutPlan);

    /**
     * Creates an exception message indicating that one of the groups in rollout
     * plan does not define neither server-group nor concurrent-groups.
     *
     * @param rolloutPlan string representation of a rollout plan
     *
     * @return the error message
     */
    @Message(id = 20, value = "One of the groups does not define neither " + SERVER_GROUP + " nor " + CONCURRENT_GROUPS + ": %s")
    String unexpectedInSeriesGroup(String rolloutPlan);

}
