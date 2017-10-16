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
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCRIPT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCRIPTS;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Client content
 *
 * @author jdenise@redhat.com
 */
public class ClientContent {

    public interface ClientContentXml {

        void writeManagementClientContent(XMLExtendedStreamWriter writer, ModelNode modelNode) throws XMLStreamException;

        void initializeClientContent(ModelNode address, List<ModelNode> list);

        void parseManagementClientContent(XMLExtendedStreamReader reader, ModelNode address, Namespace expectedNs, List<ModelNode> list) throws XMLStreamException;
    }

    public static ChainedTransformationDescriptionBuilder buildTransformerChain(ModelVersion CURRENT) {
        ChainedTransformationDescriptionBuilder chainedBuilder =
                TransformationDescriptionBuilder.Factory.createChainedInstance(PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT), CURRENT);
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(CURRENT, ModelVersion.create(4, 1, 0));
        builder.discardChildResource(PathElement.pathElement(SCRIPTS));
        ResourceTransformationDescriptionBuilder scriptsBuilder = builder.addChildResource(PathElement.pathElement(SCRIPTS));
        scriptsBuilder.discardOperations(ADD);
        return chainedBuilder;
    }

    public static void register(ManagementResourceRegistration resourceRegistration, ContentRepository contentRepository) {

        resourceRegistration.registerSubModel(
                new ManagedDMRContentTypeResourceDefinition(contentRepository, SCRIPT,
                        PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, SCRIPTS),
                        new ScriptValidator(), Descriptions.getResourceDescriptionResolver(SCRIPTS),
                        Descriptions.getResourceDescriptionResolver(SCRIPT)));
    }

    public static void registerDomain(ManagementResourceRegistration resourceRegistration, ContentRepository contentRepository) {
        register(resourceRegistration, contentRepository);
        resourceRegistration.registerSubModel(
                new ManagedDMRContentTypeResourceDefinition(contentRepository, ROLLOUT_PLAN,
                        PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS),
                        new RolloutPlanValidator(), Descriptions.getResourceDescriptionResolver(ROLLOUT_PLANS),
                        Descriptions.getResourceDescriptionResolver(ROLLOUT_PLAN)));
    }

}
