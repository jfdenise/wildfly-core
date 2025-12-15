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
public class PermissionsPreLoader {
    public static PermissionsPreLoader INSTANCE = new PermissionsPreLoader();
    private static final Map<String, List<java.security.Permission>> PERMISSIONS = new HashMap<>();
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
    System.out.println("INIT PERMISSIONS IN STATIC");
        try {
            ModuleClassLoader loader = (ModuleClassLoader) PermissionsPreLoader.class.getClassLoader();
            Module mod = loader.getModule();
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
                    System.out.println("ELYTRON, ADDING PERMISSION " + permission.getClassName() + " from module [" + module + "]");
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    java.security.Permission getPermission(String moduleName, String className) throws Exception {
        moduleName = moduleName == null ? "" : moduleName;
        List<java.security.Permission> lst = PERMISSIONS.get(moduleName);
        System.out.println("GET THE PERMISSIONS from " + PERMISSIONS);
        for (java.security.Permission p : lst) {
            System.out.println("GET PERMISSION " + p.getClass().getName());
            if (p.getClass().getName().equals(className)) {
                System.out.println(" OK GET PERMISSION " + p.getClass().getName());
                return p;
            }
        }
        throw new Exception("Permission " + className + " not found");
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
                                                NodeList permissionSets = en.getChildNodes();
                                                for (int l = 0; l < permissionSets.getLength(); l++) {
                                                    Node setNode = permissionSets.item(l);
                                                    if (setNode instanceof Element) {
                                                        if ("permission-set".equals(setNode.getNodeName())) {
                                                            NodeList permissions = setNode.getChildNodes();
                                                            for (int m = 0; m < permissions.getLength(); m++) {
                                                                Node permission = permissions.item(m);
                                                                if (permission instanceof Element) {
                                                                    if ("permission".equals(permission.getNodeName())) {
                                                                        Element permissionEl = (Element) permission;
                                                                        String clazz = permissionEl.getAttribute("class-name");
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
