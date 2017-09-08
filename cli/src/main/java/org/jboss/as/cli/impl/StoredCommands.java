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
package org.jboss.as.cli.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.parser.CommandLineParserException;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandLineException;
import static org.jboss.as.cli.impl.CliConfigImpl.CliConfigReader.assertExpectedNamespace;
import org.jboss.as.cli.impl.aesh.commands.StoredCommandCommand;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author jdenise@redhat.com
 */
public class StoredCommands {

    private static final String CLI_STORED_COMMANDS_FILE = "cli-stored-commands.xml";
    private static final String CLI_STORED_COMMANDS = "cli-stored-commands";
    private static final String STORED_COMMAND = "stored-command";

    public static class StoredCommand {

        private final String name;
        private final List<String> cmds = new ArrayList<>();

        public StoredCommand(String name) {
            this.name = name;
        }

        public StoredCommand(String name, List<String> cmds) {
            this(name);
            this.cmds.addAll(cmds);
        }

        void addCommand(String cmd) {
            cmds.add(cmd);
        }

        public List<String> getCommands() {
            return Collections.unmodifiableList(cmds);
        }

        public String getName() {
            return name;
        }
    }

    static class StoredCommandsWriter implements XMLElementWriter<List<StoredCommand>> {

        @Override
        public void writeContent(XMLExtendedStreamWriter streamWriter, List<StoredCommand> value) throws XMLStreamException {

        }
    }

    static class StoredCommandsReader implements XMLElementReader<List<StoredCommand>> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<StoredCommand> commands) throws XMLStreamException {
            String localName = reader.getLocalName();
            if (!CLI_STORED_COMMANDS.equals(localName)) {
                throw new XMLStreamException("Unexpected element: " + localName);
            }

            Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
            for (Namespace current : Namespace.cliValues()) {
                if (readerNS.equals(current)) {
                    switch (readerNS) {
                        default:
                            readStoredCommands_1_0(reader, readerNS, commands);
                            break;
                    }
                    return;
                }
            }
            throw new XMLStreamException("Unexpected element: " + localName);
        }

        public void readStoredCommands_1_0(XMLExtendedStreamReader reader, Namespace expectedNs, List<StoredCommand> commands) throws XMLStreamException {
            boolean cliStoredCommandsEnded = false;
            while (reader.hasNext() && !cliStoredCommandsEnded) {
                int tag = reader.nextTag();
                assertExpectedNamespace(reader, expectedNs);
                if (tag == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals("stored-command")) {
                        String name = reader.getAttributeValue(null, "name");
                        StoredCommand sc = new StoredCommand(name);
                        commands.add(sc);
                        readStoredCommand_1_0(reader, expectedNs, sc);
                    } else {
                        throw new XMLStreamException("Unexpected element: " + localName);
                    }
                } else if (tag == XMLStreamConstants.END_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals(CLI_STORED_COMMANDS)) {
                        cliStoredCommandsEnded = true;
                    }
                }
            }
        }

        public void readStoredCommand_1_0(XMLExtendedStreamReader reader, Namespace expectedNs, StoredCommand command) throws XMLStreamException {
            boolean storedCommandEnded = false;
            while (reader.hasNext() && !storedCommandEnded) {
                int tag = reader.nextTag();
                assertExpectedNamespace(reader, expectedNs);
                if (tag == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals("command")) {
                        String cmd = reader.getElementText();
                        command.addCommand(cmd);
                    } else {
                        throw new XMLStreamException("Unexpected element: " + localName);
                    }
                } else if (tag == XMLStreamConstants.END_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals(STORED_COMMAND)) {
                        storedCommandEnded = true;
                    }
                }
            }
        }
    }

    public static List<StoredCommand> loadCommands() throws CliInitializationException {
        File f = findFileInJBossHome();
        if (f == null) {
            return Collections.emptyList();
        }
        return parse(f);
    }

    public static void saveCommand(String name, List<String> recorded) throws CliInitializationException, IOException, CommandLineException, CommandLineParserException {
        saveCommand(new StoredCommand(name, recorded));
    }

    private static void saveCommand(StoredCommand cmd) throws CliInitializationException, CommandLineException, CommandLineParserException {
        final String jbossHome = WildFlySecurityManager.getEnvPropertyPrivileged("JBOSS_HOME", null);
        if (jbossHome == null) {
            throw new CliInitializationException("No JBOSS_HOME.");
        }

        File jbossCliFile = new File(jbossHome + File.separatorChar + "bin", CLI_STORED_COMMANDS_FILE);

        try {
            if (!jbossCliFile.exists()) {
                jbossCliFile.createNewFile();
            }
            save(jbossCliFile, cmd);
        } catch (Exception ex) {
            throw new CliInitializationException(ex);
        }
    }

    private static void save(File f, StoredCommand newCommand) throws Exception {
        if (f == null) {
            throw new CliInitializationException("The file argument is null.");
        }
        DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(f);

        addCommand(document, newCommand);

        TransformerFactory tFactory
                = TransformerFactory.newInstance();
        Transformer transformer
                = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(f);
        transformer.transform(source, result);
    }

    private static void addCommand(Document document, StoredCommand command) {
        Element rootElem = document.createElement("stored-command");
        rootElem.setAttribute("name", command.getName());
        for (String cmd : command.getCommands()) {
            Element childElem = document.createElement("command");
            childElem.appendChild(document.createCDATASection(cmd));
            rootElem.appendChild(childElem);
        }
        document.getElementsByTagName("cli-stored-commands").item(0).appendChild(rootElem);
    }

    private static List<StoredCommand> parse(File f) throws CliInitializationException {
        if (f == null) {
            throw new CliInitializationException("The file argument is null.");
        }

        List<StoredCommand> commands = new ArrayList<>();

        BufferedInputStream input = null;
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            final XMLElementReader<List<StoredCommand>> reader = new StoredCommandsReader();
            for (Namespace current : Namespace.cliValues()) {
                mapper.registerRootElement(new QName(current.getUriString(), CLI_STORED_COMMANDS), reader);
            }
            FileInputStream is = new FileInputStream(f);
            input = new BufferedInputStream(is);
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            mapper.parseDocument(commands, streamReader);
            streamReader.close();
        } catch (Throwable t) {
            throw new CliInitializationException("Failed to parse " + f.getAbsolutePath(), t);
        } finally {
            StreamUtils.safeClose(input);
        }
        return commands;
    }

    private static File findFileInJBossHome() {
        final String jbossHome = WildFlySecurityManager.getEnvPropertyPrivileged("JBOSS_HOME", null);
        if (jbossHome == null) {
            return null;
        }

        File jbossCliFile = new File(jbossHome + File.separatorChar + "bin", CLI_STORED_COMMANDS_FILE);

        if (!jbossCliFile.exists()) {
            return null;
        }

        return jbossCliFile;
    }

    public static CommandContainer newStoredCommand(StoredCommand sc) throws CommandLineParserException {
        CommandContainer container = new AeshCommandContainerBuilder().create(new StoredCommandCommand(sc));
        ProcessedCommand cmd = container.getParser().getProcessedCommand();
        container = new AeshCommandContainer(
                new CommandLineParserBuilder()
                .processedCommand(new ProcessedCommandBuilder().
                        activator(cmd.getActivator()).
                        addOptions(cmd.getOptions()).
                        aliases(cmd.getAliases()).
                        arguments(cmd.getArguments()).
                        argument(cmd.getArgument()).
                        command(cmd.getCommand()).
                        description(cmd.description()).
                        name(sc.getName()).
                        populator(cmd.getCommandPopulator()).
                        resultHandler(cmd.resultHandler()).
                        validator(cmd.validator()).
                        create()).create());
        return container;
    }
}
