/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.elytron;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Module;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wildfly.common.xml.DocumentBuilderFactoryUtil;
import org.wildfly.security.permission.PermissionUtil;

/**
 *
 * @author jdenise
 */
public class ServiceLoaderInitializer {

    //private static AcmeClientSpi ACMECLIENT;
    //private static Map<String, List<Provider>> PROVIDERS = new HashMap<>();
    private static Map<String, List<java.security.Permission>> PERMISSIONS = new HashMap<>();
    static class Permission {
        private final String className;
        private final String module;
        private final String targetName;
        private final String action;

        Permission(final String className, final String module, final String targetName, final String action) {
            this.className = className;
            this.module = module != null ? ModuleIdentifierUtil.parseCanonicalModuleIdentifier(module) : module;
            this.targetName = targetName;
            this.action = action;
        }

        public String getClassName() {
            return className;
        }

        public String getModule() {
            return module;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getAction() {
            return action;
        }
    }
    static {
        System.out.println("INITIALIZE ELYTRON PROVIDERS");
        //ACMECLIENT = ServiceLoader.load(AcmeClientSpi.class, ElytronSubsystemMessages.class.getClassLoader()).iterator().next();
        try {
//            List<String> modules = retrieveProviderModules();
//            System.out.println("FOUND MODULES " + modules);
            ModuleClassLoader loader = (ModuleClassLoader) ServiceLoaderInitializer.class.getClassLoader();
            Module mod = loader.getModule();
//            for (String moduleName : modules) {
//                Module module = mod.getModule(ModuleIdentifierUtil.parseCanonicalModuleIdentifier(moduleName));
//                Iterable<Provider> providers = org.jboss.modules.Module.findServices(Provider.class, new Predicate<Class<?>>() {
//                    @Override
//                    public boolean test(final Class<?> providerClass) {
//                        // We don't want to pick up JDK services resolved via JPMS definitions.
//                        return providerClass.getClassLoader() instanceof ModuleClassLoader;
//                    }
//                }, module.getClassLoader());
//                List<Provider> lst = new ArrayList<>();
//                PROVIDERS.put(moduleName, lst);
//                for (Provider p : providers) {
//                    System.out.println("Add provider " + p + " for Module " + moduleName);
//                    lst.add(p);
//                }
//            }
            Map<String, List<Permission>> permissions = retrievePermissions();
            for (String module : permissions.keySet()) {
                List<java.security.Permission> lst = new ArrayList<>();
                PERMISSIONS.put(module, lst);
                ClassLoader classLoader;
                if (module.equals("")) {
                    classLoader = PermissionMapperDefinitions.class.getClassLoader();
                } else {
                    classLoader = mod.getModule(ModuleIdentifierUtil.parseCanonicalModuleIdentifier(module)).getClassLoader();
                }
                for (Permission permission : permissions.get(module)) {
                    java.security.Permission p = PermissionUtil.createPermission(classLoader, permission.getClassName(), permission.getTargetName(), permission.getAction());
                    lst.add(p);
                    System.out.println("ADDING PERMISSION " + permission.getClassName() + " from module [" + module + "]");
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

//    static List<Provider> getProviders(String moduleName) {
//        return PROVIDERS.get(moduleName);
//    }

    static java.security.Permission getPermission(String moduleName, String className) throws Exception {
        moduleName = moduleName == null ? "" : moduleName;
        System.out.println("GEt permission [" + moduleName + "] " + className);
        List<java.security.Permission> lst = PERMISSIONS.get(moduleName);
        for (java.security.Permission p : lst) {
            if (p.getClass().getName().equals(className)) {
                return p;
            }
        }
        throw new Exception("Permission " + className + " not found");
    }

//    static AcmeClientSpi getAcmeClientSpi() {
//        return ACMECLIENT;
//    }

    private static List<String> retrieveProviderModules() throws Exception {
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
                    if ("profile".equals(n.getNodeName())) {
                        NodeList subsystems = n.getChildNodes();
                        for (int j = 0; j < subsystems.getLength(); j++) {
                            Node subsystem = subsystems.item(j);
                            if (subsystem instanceof Element) {
                                Element el = (Element) subsystem;
                                String attr = el.getAttribute("xmlns");
                                if (attr.startsWith("urn:wildfly:elytron")) {
                                    NodeList elems = el.getChildNodes();
                                    for (int k = 0; k < elems.getLength(); k++) {
                                        Node en = elems.item(k);
                                        if (en instanceof Element) {
                                            if ("providers".equals(en.getNodeName())) {
                                                NodeList providers = en.getChildNodes();
                                                for (int l = 0; l < providers.getLength(); l++) {
                                                    Node prov = providers.item(l);
                                                    if (prov instanceof Element) {
                                                        if ("provider-loader".equals(prov.getNodeName())) {
                                                            Element providerLoader = (Element) prov;
                                                            modules.add(providerLoader.getAttribute("module"));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return modules;
    }

    private static Map<String, List<Permission>> retrievePermissions() throws Exception {
        Map<String, List<Permission>> map = new HashMap<>();
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
                    if ("profile".equals(n.getNodeName())) {
                        NodeList subsystems = n.getChildNodes();
                        for (int j = 0; j < subsystems.getLength(); j++) {
                            Node subsystem = subsystems.item(j);
                            if (subsystem instanceof Element) {
                                Element el = (Element) subsystem;
                                String attr = el.getAttribute("xmlns");
                                if (attr.startsWith("urn:wildfly:elytron")) {
                                    NodeList elems = el.getChildNodes();
                                    for (int k = 0; k < elems.getLength(); k++) {
                                        Node en = elems.item(k);
                                        if (en instanceof Element) {
                                            if ("permission-sets".equals(en.getNodeName())) {
                                                System.out.println("SETS FOUND");
                                                NodeList permissionSets = en.getChildNodes();
                                                for (int l = 0; l < permissionSets.getLength(); l++) {
                                                    Node setNode = permissionSets.item(l);
                                                    if (setNode instanceof Element) {
                                                        if ("permission-set".equals(setNode.getNodeName())) {
                                                            System.out.println("SET FOUND");
                                                            NodeList permissions = setNode.getChildNodes();
                                                            for (int m = 0; m < permissions.getLength(); m++) {
                                                                Node permission = permissions.item(m);
                                                                if (permission instanceof Element) {
                                                                    if ("permission".equals(permission.getNodeName())) {
                                                                        Element permissionEl = (Element) permission;
                                                                        String clazz = permissionEl.getAttribute("class-name");
                                                                        System.out.println("PERMISSION FOUND " + clazz);
                                                                        String module = permissionEl.hasAttribute("module") ? permissionEl.getAttribute("module") : null;
                                                                        String action = permissionEl.hasAttribute("action") ? permissionEl.getAttribute("action") : null;
                                                                        String targetName = permissionEl.hasAttribute("target-name") ? permissionEl.getAttribute("target-name") : null;
                                                                        String moduleKey = module == null ? "" : module;
                                                                        List<Permission> permissionList = map.get(moduleKey);
                                                                        if (permissionList == null) {
                                                                            permissionList = new ArrayList<>();
                                                                            map.put(moduleKey, permissionList);
                                                                        }
                                                                        Permission p = new Permission(clazz, module, targetName, action);
                                                                        permissionList.add(p);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
}
