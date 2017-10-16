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
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.HashUtil;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCRIPTS;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author jdenise@redhat.com
 */
public class ClientContentXml_5 {

    public static final ClientContent.ClientContentXml DOMAIN = new ClientContent.ClientContentXml() {
        @Override
        public void parseManagementClientContent(XMLExtendedStreamReader reader,
                ModelNode address, Namespace expectedNs, List<ModelNode> list) throws XMLStreamException {
            parseDomainManagementClientContent(reader, address, expectedNs, list);
        }

        @Override
        public void initializeClientContent(ModelNode address, List<ModelNode> list) {
            initializeRolloutPlans(address, list);
            initializeScripts(address, list);
        }

        @Override
        public void writeManagementClientContent(XMLExtendedStreamWriter writer,
                ModelNode modelNode) throws XMLStreamException {
            writeDomainManagementClientContent(writer, modelNode);
        }
    };

    public static final ClientContent.ClientContentXml STANDALONE = new ClientContent.ClientContentXml() {
        @Override
        public void parseManagementClientContent(XMLExtendedStreamReader reader,
                ModelNode address, Namespace expectedNs, List<ModelNode> list) throws XMLStreamException {
            ClientContentXml_5.parseManagementClientContent(reader, address, expectedNs, list);
        }

        @Override
        public void initializeClientContent(ModelNode address, List<ModelNode> list) {
            initializeScripts(address, list);
        }

        @Override
        public void writeManagementClientContent(XMLExtendedStreamWriter writer,
                ModelNode modelNode) throws XMLStreamException {
            ClientContentXml_5.writeManagementClientContent(writer, modelNode);
        }
    };

    private static void parseDomainManagementClientContent(XMLExtendedStreamReader reader,
            ModelNode address, Namespace expectedNs, List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        boolean rolloutPlansAdded = false;
        boolean scriptsAdded = false;
        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ROLLOUT_PLANS: {
                    parseRolloutPlans(reader, address, list);
                    rolloutPlansAdded = true;
                    break;
                }
                case SCRIPTS: {
                    parseScripts(reader, address, list);
                    scriptsAdded = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (!rolloutPlansAdded) {
            initializeRolloutPlans(address, list);
        }
        if (!scriptsAdded) {
            initializeScripts(address, list);
        }
    }

    private static void parseManagementClientContent(XMLExtendedStreamReader reader,
            ModelNode address, Namespace expectedNs, List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        boolean scriptsAdded = false;
        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SCRIPTS: {
                    parseScripts(reader, address, list);
                    scriptsAdded = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (!scriptsAdded) {
            initializeScripts(address, list);
        }
    }

    private static void parseRolloutPlans(XMLExtendedStreamReader reader,
            ModelNode address, List<ModelNode> list) throws XMLStreamException {

        String hash = readStringAttributeElement(reader, Attribute.SHA1.getLocalName());

        ModelNode addAddress = address.clone().add(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS);
        ModelNode addOp = Util.getEmptyOperation(ADD, addAddress);
        try {
            addOp.get(HASH).set(HashUtil.hexStringToByteArray(hash));
        } catch (final Exception e) {
            throw ControllerLogger.ROOT_LOGGER.invalidSha1Value(e, hash,
                    Attribute.SHA1.getLocalName(), reader.getLocation());
        }

        list.add(addOp);
    }

    private static void parseScripts(XMLExtendedStreamReader reader,
            ModelNode address, List<ModelNode> list) throws XMLStreamException {

        String hash = readStringAttributeElement(reader, Attribute.SHA1.getLocalName());

        ModelNode addAddress = address.clone().add(MANAGEMENT_CLIENT_CONTENT, SCRIPTS);
        ModelNode addOp = Util.getEmptyOperation(ADD, addAddress);
        try {
            addOp.get(HASH).set(HashUtil.hexStringToByteArray(hash));
        } catch (final Exception e) {
            throw ControllerLogger.ROOT_LOGGER.invalidSha1Value(e, hash,
                    Attribute.SHA1.getLocalName(), reader.getLocation());
        }

        list.add(addOp);
    }

    private static void initializeRolloutPlans(ModelNode address, List<ModelNode> list) {

        ModelNode addAddress = address.clone().add(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS);
        ModelNode addOp = Util.getEmptyOperation(ADD, addAddress);
        list.add(addOp);
    }

    private static void initializeScripts(ModelNode address, List<ModelNode> list) {
        // NO-OP, we don't want this resource added in all cases.
    }

    private static void writeDomainManagementClientContent(XMLExtendedStreamWriter writer,
            ModelNode modelNode) throws XMLStreamException {
        boolean hasRolloutPlans = modelNode.hasDefined(ROLLOUT_PLANS) && modelNode.get(ROLLOUT_PLANS).hasDefined(HASH);
        boolean hasScripts = modelNode.hasDefined(SCRIPTS) && modelNode.get(SCRIPTS).hasDefined(HASH);
        boolean mustWrite = hasRolloutPlans || hasScripts; // || other elements we may add later
        if (mustWrite) {
            writer.writeStartElement(Element.MANAGEMENT_CLIENT_CONTENT.getLocalName());
            if (hasRolloutPlans) {
                writer.writeEmptyElement(Element.ROLLOUT_PLANS.getLocalName());
                writer.writeAttribute(Attribute.SHA1.getLocalName(),
                        HashUtil.bytesToHexString(modelNode.get(ROLLOUT_PLANS).get(HASH).asBytes()));
            }
            if (hasScripts) {
                writer.writeEmptyElement(Element.SCRIPTS.getLocalName());
                writer.writeAttribute(Attribute.SHA1.getLocalName(),
                        HashUtil.bytesToHexString(modelNode.get(SCRIPTS).get(HASH).asBytes()));
            }
            writer.writeEndElement();
        }
    }

    private static void writeManagementClientContent(XMLExtendedStreamWriter writer,
            ModelNode modelNode) throws XMLStreamException {
        boolean hasRolloutPlans = modelNode.hasDefined(SCRIPTS) && modelNode.get(SCRIPTS).hasDefined(HASH);
        boolean mustWrite = hasRolloutPlans;
        if (mustWrite) {
            writer.writeStartElement(Element.MANAGEMENT_CLIENT_CONTENT.getLocalName());
            if (hasRolloutPlans) {
                writer.writeEmptyElement(Element.SCRIPTS.getLocalName());
                writer.writeAttribute(Attribute.SHA1.getLocalName(),
                        HashUtil.bytesToHexString(modelNode.get(SCRIPTS).get(HASH).asBytes()));
            }
            writer.writeEndElement();
        }
    }
}
