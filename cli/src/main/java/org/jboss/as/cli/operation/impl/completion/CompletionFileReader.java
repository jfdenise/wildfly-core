/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.operation.impl.completion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.impl.Namespace;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;
import org.wildfly.security.manager.WildFlySecurityManager;

class CompletionFileReader {

    private static final String WILDFLY_COMPLETION_CONFIG = "wildfly.cli.completion.config";
    private static final String CURRENT_WORKING_DIRECTORY = "user.dir";
    private static final String WILDFLY_COMPLETION_FILE = "wildfly-cli-completion.xml";

    private static final String COMPLETION = "completion";
    private static final String PRODUCER = "producer";
    private static final String CONSUMER = "consumer";

    private static final String WILDFLY_CLI_COMPLETION = "wildfly-cli-completion";

    static CompletionAssociations load() throws CliInitializationException {
        File wildflyCompletionFile = findCompletionFileFromSystemProperty();

        if (wildflyCompletionFile == null) {
            wildflyCompletionFile = findCompletionFileInCurrentDirectory();
        }

        if (wildflyCompletionFile == null) {
            wildflyCompletionFile = findCompletionFileInJBossHome();
        }

        if (wildflyCompletionFile == null) {
            return new CompletionAssociations();
        } else {
            return parse(wildflyCompletionFile);
        }
    }

    private static File findCompletionFileFromSystemProperty() {
        final String jbossCliConfig = WildFlySecurityManager.getPropertyPrivileged(WILDFLY_COMPLETION_CONFIG, null);
        if (jbossCliConfig == null) {
            return null;
        }

        return new File(jbossCliConfig);
    }

    private static File findCompletionFileInCurrentDirectory() {
        final String currentDir = WildFlySecurityManager.getPropertyPrivileged(CURRENT_WORKING_DIRECTORY, null);
        if (currentDir == null) {
            return null;
        }

        File jbossCliFile = new File(currentDir, WILDFLY_COMPLETION_FILE);

        if (!jbossCliFile.exists()) {
            return null;
        }

        return jbossCliFile;
    }

    private static File findCompletionFileInJBossHome() {
        final String jbossHome = WildFlySecurityManager.getEnvPropertyPrivileged("JBOSS_HOME", null);
        if (jbossHome == null) {
            return null;
        }

        File jbossCliFile = new File(jbossHome + File.separatorChar + "bin", WILDFLY_COMPLETION_FILE);

        if (!jbossCliFile.exists()) {
            return null;
        }

        return jbossCliFile;
    }

    private static CompletionAssociations parse(File f) throws CliInitializationException {
        if (f == null) {
            throw new CliInitializationException("The file argument is null.");
        }
        if (!f.exists()) {
            return new CompletionAssociations();
        }

        CompletionAssociations config = new CompletionAssociations();

        BufferedInputStream input = null;
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            final XMLElementReader<CompletionAssociations> reader = new CliCompletionConfigReader();
            for (Namespace current : Namespace.cliValues()) {
                mapper.registerRootElement(new QName(current.getUriString(), WILDFLY_CLI_COMPLETION), reader);
            }
            FileInputStream is = new FileInputStream(f);
            input = new BufferedInputStream(is);
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            mapper.parseDocument(config, streamReader);
            streamReader.close();
        } catch (FileNotFoundException | FactoryConfigurationError | XMLStreamException t) {
            throw new CliInitializationException("Failed to parse " + f.getAbsolutePath(), t);
        } finally {
            StreamUtils.safeClose(input);
        }
        return config;
    }

    private static String resolveString(String str) throws XMLStreamException {
        if (str == null) {
            return null;
        }
        if (str.startsWith("${") && str.endsWith("}")) {
            str = str.substring(2, str.length() - 1);
            final String resolved = WildFlySecurityManager.getPropertyPrivileged(str, null);
            if (resolved == null) {
                throw new XMLStreamException("Failed to resolve '" + str + "' to a non-null value.");
            }
            str = resolved;
        }
        return str;
    }

    static class CliCompletionConfigReader implements XMLElementReader<CompletionAssociations> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, CompletionAssociations config) throws XMLStreamException {
            String localName = reader.getLocalName();
            if (!WILDFLY_CLI_COMPLETION.equals(localName)) {
                throw new XMLStreamException("Unexpected element: " + localName);
            }

            Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
            for (Namespace current : Namespace.cliValues()) {
                if (readerNS.equals(current)) {
                    switch (readerNS) {
                        default: {
                            try {
                                readCLICompletionElement_3_1(reader, readerNS, config);
                            } catch (CommandFormatException ex) {
                                throw new XMLStreamException(ex);
                            }
                        }
                    }
                    return;
                }
            }
            throw new XMLStreamException("Unexpected element: " + localName);
        }

        public void readCLICompletionElement_3_1(XMLExtendedStreamReader reader,
                Namespace expectedNs, CompletionAssociations config)
                throws XMLStreamException, CommandFormatException {
            List<String> producers = null;
            List<String> consumers = null;
            while (reader.hasNext()) {
                int tag = reader.nextTag();
                if (tag == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    switch (localName) {
                        case PRODUCER: {
                            if (producers == null) {
                                producers = new ArrayList<>();
                            }
                            final String resolved = resolveString(reader.getElementText());
                            producers.add(resolved);
                            break;
                        }
                        case CONSUMER: {
                            if (consumers == null) {
                                consumers = new ArrayList<>();
                            }
                            final String resolved = resolveString(reader.getElementText());
                            consumers.add(resolved);
                            break;
                        }
                    }
                } else if (tag == XMLStreamConstants.END_ELEMENT) {
                    final String localName = reader.getLocalName();
                    switch (localName) {
                        case COMPLETION: {
                            config.addAssociation(producers, consumers);
                            producers = null;
                            consumers = null;
                            break;
                        }
                        case WILDFLY_CLI_COMPLETION: {
                            return;
                        }
                    }
                }
            }
        }
    }
}
