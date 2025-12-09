/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wildfly.common.xml.DocumentBuilderFactoryUtil;

/**
 *
 * @author jdenise
 */
public class ExtensionLoader {

    private static final Map<String, List<Extension>> EXTENSIONS = new HashMap<>();

    static {
        try {
            ModuleClassLoader m = (ModuleClassLoader) ExtensionLoader.class.getClassLoader();
            ModuleLoader moduleLoader = m.getModule().getModuleLoader();
            List<String> extensions = retrieveExtensions();
            for (String moduleName : extensions) {
                final org.jboss.modules.Module module = moduleLoader.loadModule(moduleName);
                List<Extension> lst = new ArrayList<>();
                EXTENSIONS.put(moduleName, lst);
                for (final Extension extension : module.loadService(Extension.class)) {
                    lst.add(extension);
                    System.out.println("ADDING EXTENSION " + extension);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public static void init() {

    }
    public static List<Extension> getExtensions(String moduleName) {
        return EXTENSIONS.get(moduleName);
    }
    public static Map<String, List<Extension>> getAllExtensions() {
        return EXTENSIONS;
    }
    private static List<String> retrieveExtensions() throws Exception {
        List<String> modules = new ArrayList<>();
        Path configFile = Paths.get(System.getProperty("jboss.server.base.dir")).resolve("configuration/standalone.xml");
        try (FileInputStream fileInputStream = new FileInputStream(configFile.toFile())) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactoryUtil.create();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(fileInputStream);
            Element root = document.getDocumentElement();

            NodeList lst = root.getChildNodes();
            for (int i = 0; i < lst.getLength(); i++) {
                Node n = lst.item(i);
                if (n instanceof Element) {
                    if ("extensions".equals(n.getNodeName())) {
                        NodeList extensions = n.getChildNodes();
                        for (int j = 0; j < extensions.getLength(); j++) {
                            Node ext = extensions.item(j);
                            if (ext instanceof Element) {
                                if ("extension".equals(ext.getNodeName())) {
                                    Element el = (Element) ext;
                                    modules.add(el.getAttribute("module"));
                                }
                            }
                        }
                    }
                }
            }
        }
        return modules;
    }
}
