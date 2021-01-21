/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.core.jar.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author jdenise
 */
@SuppressWarnings("unchecked")
class Yaml2Cli {

    static class FeatureParameterSpec {

        final String name;
        final String defaultValue;
        final boolean isId;

        FeatureParameterSpec(String name, String defaultvalue, boolean isId) {
            this.name = name;
            this.defaultValue = defaultvalue;
            this.isId = isId;
        }

        boolean isFeatureId() {
            return isId;
        }

        boolean hasDefaultValue() {
            return defaultValue != null;
        }

        String getDefaultValue() {
            return defaultValue;
        }
    }

    static class FeatureSpec {

        private final String op;
        private final String opParams;
        private final String addrParams;
        private final Map<String, FeatureParameterSpec> params = new HashMap<>();
        private final Set<String> providedCapabilities = new HashSet<>();
        private final Set<String> requiredCapabilities = new HashSet<>();
        private final Set<String> featureRefs = new HashSet<>();
        private final String name;
        private final String branch;

        FeatureSpec(Properties prop, String name) {
            this.name = name;
            op = prop.getProperty("spec.op");
            branch = prop.getProperty("spec.feature.branch");
            opParams = prop.getProperty("spec.op.params");
            addrParams = prop.getProperty("spec.addr.params");
            String[] paramNames = prop.getProperty("spec.params").split(",");
            for (String p : paramNames) {
                String defaultValue = prop.getProperty("spec.param." + p + ".default");
                boolean isId = prop.getProperty("spec.param." + p + ".id") != null;
                params.put(p, new FeatureParameterSpec(p, defaultValue, isId));
            }
            String refs = prop.getProperty("spec.refs");
            if (refs != null) {
                String[] split = refs.split(",");
                for (String s : split) {
                    featureRefs.add(s);
                }
            }
            String required = prop.getProperty("spec.capabilities.required");
            if (required != null) {
                String[] split = required.split(",");
                for (String s : split) {
                    requiredCapabilities.add(s);
                }
            }
            String provided = prop.getProperty("spec.capabilities.provided");
            if (provided != null) {
                String[] split = provided.split(",");
                for (String s : split) {
                    providedCapabilities.add(s);
                }
            }
        }

        Set<String> getFeatureRefs() {
            return featureRefs;
        }

        Set<String> getRequiredCapabilities() {
            return requiredCapabilities;
        }

        Set<String> getProvidedCapabilities() {
            return providedCapabilities;
        }

        boolean hasOp() {
            return op != null;
        }

        String getOp() {
            return op;
        }

        String getOpParams() {
            return opParams;
        }

        String getAddrParams() {
            return addrParams;
        }

        boolean hasParam(String p) {
            return params.containsKey(p);
        }

        FeatureParameterSpec getParam(String p) {
            return params.get(p);
        }

        String getBranch() {
            return branch;
        }
    }

    static class CapabilityNode {

        List<String> values = new ArrayList<>();
        CapabilityNode next;
    }

    static class ResolvedFeature {

        private final List<String> commands = new ArrayList<>();
        private final FeatureSpec spec;
        private final List<String> addArguments = new ArrayList<>();
        private final List<String> writeAttributes = new ArrayList<>();
        private final List<String> listAdd = new ArrayList<>();
        private final Map<String, Object> args = new HashMap<>();
        private final Map<String, String> resolvedIds;
        private final String resolvedPath;
        private final String address;
        private final Set<String> subComplexFeaturesRequiredCapabilities = new HashSet<>();
        private final Set<String> subComplexFeaturesProvidedCapabilities = new HashSet<>();

        ResolvedFeature(FeatureSpec spec, Map<String, String> resolvedIds, String resolvedPath, String address) {
            this.spec = spec;
            this.resolvedIds = resolvedIds;
            this.resolvedPath = resolvedPath;
            this.address = address;
        }

        String getResolvedPath() {
            return resolvedPath;
        }

        String getAddress() {
            return address;
        }

        Set<String> getResolvedProvidedCapabilities() {
            Map<String, Object> all = new HashMap<>();
            all.putAll(args);
            for (String k : resolvedIds.keySet()) {
                all.put(k, resolvedIds.get(k));
            }
            Set<String> ret = getResolvedCapabilities(spec.getProvidedCapabilities(), all);
            ret.addAll(subComplexFeaturesProvidedCapabilities);
            return ret;
        }

        Set<String> getResolvedRequiredCapabilities() {
            Set<String> ret = getResolvedCapabilities(spec.getRequiredCapabilities(), args);
            ret.addAll(subComplexFeaturesRequiredCapabilities);
            return ret;
        }

        Set<String> getResolvedCapabilities(Set<String> capabilities, Map<String, Object> args) {
            Set<String> ret = new HashSet<>();
            for (String provided : capabilities) {
                String[] split = provided.split("\\.");
                boolean found = true;
                CapabilityNode current = null;
                CapabilityNode root = null;
                for (int i = 0; i < split.length; i++) {
                    String s = split[i];
                    if ("$profile".equals(s)) {
                        continue;
                    }
                    if (current == null) {
                        current = new CapabilityNode();
                        root = current;
                    } else {
                        CapabilityNode next = new CapabilityNode();
                        current.next = next;
                        current = next;
                    }
                    if (s.startsWith("$")) {
                        s = s.substring(1);
                        Object val = args.get(s);
                        if (val == null) {
                            found = false;
                            break;
                        } else {
                            if (val instanceof String) {
                                current.values.add((String) val);
                            } else {
                                if (val instanceof List) {
                                    List<Object> lst = (List) val;
                                    for (Object obj : lst) {
                                        current.values.add(obj.toString());
                                    }
                                }
                            }
                        }
                    } else {
                        current.values.add(s);
                    }
                }
                if (found) {
                    ret.addAll(nodeToString(root));
                }
            }
            return ret;
        }

        List<String> nodeToString(CapabilityNode node) {
            if (node.next != null) {
                List<String> subPaths = nodeToString(node.next);
                List<String> paths = new ArrayList<>();
                for (String v : node.values) {
                    for (String sp : subPaths) {
                        paths.add(v + "." + sp);
                    }
                }
                return paths;
            } else {
                return node.values;
            }
        }

        List<String> getCommands() {
            return this.commands;
        }

        void addCommands(List<String> commands) {
            this.commands.addAll(commands);
        }

        void retrieveParamsAndAttributes(Map<String, FeatureSpec> allSpecs, Map<String, Object> map) throws Exception {
            if (map == null) {
                return;
            }
            String op = spec.getOp();
            String[] opParams = spec.getOpParams().split(",");
            Set<String> toRemove = new HashSet<>();
            for (String arg : map.keySet()) {
                // XXX What about complexTypes....They are there but also in a featueSpec...
                // Should check about complexTypes there, append to the path the paramterr and see.
                String complexSpec = spec.name + "." + arg;
                FeatureSpec complex = allSpecs.get(complexSpec);
                if (spec.hasParam(arg)) {
                    FeatureParameterSpec ps = spec.getParam(arg);
                    if (ps.isFeatureId()) {
                        throw new Exception("Can't have a featureid at this point for " + map);
                    }
                    toRemove.add(arg);
                    Object val = map.get(arg);
                    // Add to the add.
                    if ("add".equals(op)) {
                        boolean found = false;
                        for (String k : opParams) {
                            if (arg.equals(k)) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            addArguments.add(arg + "=" + val);
                        }
                    }
                    if (val instanceof List) {
                        List<Object> lst = (List) val;
                        for (Object v : lst) {
                            listAdd.add("name=" + arg + ", value=" + v);
                        }
                    } else {
                        writeAttributes.add("name=" + arg + ", value=" + val);
                    }

                    if (val instanceof Map && complex != null) {
                        // Handle capabilities
                        // We can ignore refs, they should be the parent feature.
                        Map<String, Object> m = (Map<String, Object>) val;
                        ResolvedFeature f = new ResolvedFeature(complex, resolvedIds, complexSpec, null);
                        f.retrieveParamsAndAttributes(allSpecs, m);
                        subComplexFeaturesRequiredCapabilities.addAll(f.getResolvedRequiredCapabilities());
                        subComplexFeaturesProvidedCapabilities.addAll(f.getResolvedProvidedCapabilities());
                    }
                    args.put(arg, val);
                }
            }
            for (String a : toRemove) {
                map.remove(a);
            }
        }

        private void addCommands() {
            boolean hasAdd = "add".equals(spec.getOp());
            boolean hasAttributes = !writeAttributes.isEmpty() || !listAdd.isEmpty();
            if (hasAdd) {
                commands.add("if (outcome != success) of " + address + ":read-resource");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < addArguments.size(); i++) {
                    builder.append(addArguments.get(i));
                    if (i < addArguments.size() - 1) {
                        builder.append(",");
                    }
                }
                commands.add(address + ":" + spec.getOp() + "(" + builder.toString() + ")");
                if (hasAttributes) {
                    commands.add("else");
                }
            }

            // Only one of the 2 lists contains content
            for (String w : writeAttributes) {
                commands.add(address + ":write-attribute(" + w + ")");
            }
            for (String w : listAdd) {
                commands.add(address + ":list-add(" + w + ")");
            }
            if (hasAdd) {
                commands.add("end-if");
            }
        }
    }

    static Path transform(Path yamlFile, Path jBossHome) throws Exception {
        long time = System.currentTimeMillis();
        Path featuresDir = jBossHome.resolve("galleon-features");
        List<ResolvedFeature> commands = doTransform(yamlFile, featuresDir);
        Path cliScript = null;
        if (!commands.isEmpty()) {
            cliScript = jBossHome.resolve("yaml-to-cli-generated-script.txt");
            System.out.println("Yaml transformed onto CLI in " + cliScript);
            List<String> orderedCommands = orderCommands(commands);
            Files.write(cliScript, orderedCommands);
        }
        System.out.println("yaml to cli took " + (System.currentTimeMillis() - time));
        return cliScript;
    }

    static List<String> orderCommands(List<ResolvedFeature> resolvedFeatures) {
        List<String> lst = new ArrayList<>();
        // Others are outside this graph, are in existing config.
        Map<String, ResolvedFeature> localProvidedCapabilities = new HashMap<>();
        Map<String, FeatureSpec> localFeatureSpec = new HashMap<>();
        Map<String, List<String>> branches = new HashMap<>();
        for (ResolvedFeature feature : resolvedFeatures) {
            System.out.println(feature.resolvedPath);
            if (!feature.getResolvedRequiredCapabilities().isEmpty()) {
                System.out.println("Required capabilities:");
                for (String c : feature.getResolvedRequiredCapabilities()) {
                    System.out.println("  " + c);
                }
            }
            if (!feature.getResolvedProvidedCapabilities().isEmpty()) {
                System.out.println("Provided capabilities:");
                for (String c : feature.getResolvedProvidedCapabilities()) {
                    System.out.println("  " + c);
                    localProvidedCapabilities.put(c, feature);
                }
            }
            if (!feature.spec.getFeatureRefs().isEmpty()) {
                System.out.println("Feature Refs:");
                for (String c : feature.spec.getFeatureRefs()) {
                    System.out.println("  " + c);
                    localProvidedCapabilities.put(c, feature);
                }
            }
            // Add the feature-spec name for refs that can be treated as dependency
            localFeatureSpec.put(feature.spec.name, feature.spec);
        }
        List<ResolvedFeature> capabilitiesOrderedFeatures = new ArrayList<>();
        List<ResolvedFeature> orderedFeatures = new ArrayList<>();
        List<ResolvedFeature> unOrderedFeatures = new ArrayList<>();
        unOrderedFeatures.addAll(resolvedFeatures);
        while (!unOrderedFeatures.isEmpty()) {
            Iterator<ResolvedFeature> it = unOrderedFeatures.iterator();
            while (it.hasNext()) {
                ResolvedFeature f = it.next();
                System.out.println("DEALING WITH " + f.resolvedPath);
                boolean noDependency = true;
                for (String c : f.getResolvedRequiredCapabilities()) {
                    if (localProvidedCapabilities.containsKey(c)) {
                        noDependency = false;
                        System.out.println("Capability " + c + " is provided by " + localProvidedCapabilities.get(c).resolvedPath);
                        break;
                    }
                }
                // No capabilities dependency in this graph
                if (noDependency) {
                    it.remove();
                    capabilitiesOrderedFeatures.add(f);
                    // We need to remove the capabilities that this feature provide,
                    // They are now considered to be present in the config.
                    for (String provided : f.getResolvedProvidedCapabilities()) {
                        localProvidedCapabilities.remove(provided);
                    }
                } else {
                    System.out.println("Reordering " + f.resolvedPath);
                }
            }
        }
        System.out.println("CAPABILITIES RE-ORDERING DONE");
        // order base on ref dependencies (enforce subsystem is added prior to features
        // present in the subsystem.
        while (!capabilitiesOrderedFeatures.isEmpty()) {
            Iterator<ResolvedFeature> it = capabilitiesOrderedFeatures.iterator();
            while (it.hasNext()) {
                ResolvedFeature f = it.next();
                boolean noDependency = true;
                for (String c : f.spec.getFeatureRefs()) {
                    if (localFeatureSpec.containsKey(c)) {
                        noDependency = false;
                        System.out.println(f.resolvedPath + " has dependency on " + c);
                        break;
                    }
                }
                if (noDependency) {
                    it.remove();
                    orderedFeatures.add(f);
                    // We need to remove thespec
                    // It is now considered to be present in the config.
                    localFeatureSpec.remove(f.spec.name);
                } else {
                    System.out.println("Reordering " + f.resolvedPath);
                }
            }
        }
        System.out.println("ORDERED PATHS");
        for (ResolvedFeature rf : orderedFeatures) {
            if (!rf.getCommands().isEmpty()) {
                lst.add("# " + rf.resolvedPath);
                lst.addAll(rf.getCommands());
            }
        }
        return lst;
    }

    static List<ResolvedFeature> doTransform(Path yamlFile, Path features) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(yamlFile.toFile())) {
            return transform(inputStream, features);
        }
    }

    static List<ResolvedFeature> transform(InputStream inputStream, Path features) throws Exception {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);
        // skip wildfly-bootable
        Map<String, Object> v = (Map) config.values().iterator().next();
        Map<String, FeatureSpec> allSpecs = getAllSpecs(features);
        List<ResolvedFeature> commands = new ArrayList<>();
        for (String k : v.keySet()) {
            Object value = v.get(k);
            commands.addAll(getSpecs2(k, "/", value, allSpecs, new ArrayList<>(), k));
        }
        return commands;
    }

    private static Map<String, FeatureSpec> getAllSpecs(Path features) throws Exception {
        Map<String, FeatureSpec> ret = new HashMap<>();
        String[] files = features.toFile().list();
        for (String f : files) {
            File specFile = new File(features.toFile(), f);
            Properties props = new Properties();
            try (FileInputStream inputStream = new FileInputStream(specFile)) {
                props.load(inputStream);
            }
            String name = f.substring(0, f.lastIndexOf(".properties"));
            ret.put(name, new FeatureSpec(props, name));
        }
        return ret;

    }

    private static List<ResolvedFeature> getSpecs2(String path, String address, Object value, Map<String, FeatureSpec> allSpecs, List<String> values, String resolvedPath) throws Exception {
        FeatureSpec spec = allSpecs.get(path);
        List<ResolvedFeature> ret = new ArrayList<>();
        if (spec != null) {
            // we have a spec can be null or a Map only.
            Map<String, Object> map = (Map) value;
            //System.out.println(map);
            // if map is null, means we need to add the feature only: eg: subsystem.keycloak
            if (map == null) {
                if (spec.hasOp()) {
                    address = address + formatPath(path);
                    ResolvedFeature feature = new ResolvedFeature(spec, Collections.emptyMap(), path, address);
                    ret.add(feature);
                    feature.addCommands();
                } else {
                    // We can't create the feature, should be auto created by the server.
                    // Nothing to do, no more items in the map we are at the end of a branch.
                }
            } else {
                // At this point or we have the path to be a feature and we don't need an id
                // or we need an id.
                //We can check the spec for that
                //System.out.println("SPEC " + spec);
                ResolvedFeature feature = isResolvedPath(spec, path, values, resolvedPath);
                if (feature != null) {
                    ret.add(feature);
                    // We have a fully named feature. The map is composed of attributes or next features.
                    address = feature.getAddress();
                    if (spec.hasOp()) {
                        processResolvedFeature(allSpecs, map, feature);
                    }
                    // We can have remaining keys in the next value, we must generate features for it
                    navigatePath(path, address, map, allSpecs, ret, values, resolvedPath);
                } else {
                    // We need to find next id to resolve the feature.
                    // eg: system-property.aaa.value=foo
                    int dot = path.lastIndexOf(".");
                    String last = dot == -1 ? path : path.substring(dot + 1);
                    for (String idValue : map.keySet()) {
                        //System.out.println("ADD VALUE " + idValue);
                        String currentResolvedPath = resolvedPath + "." + idValue;
                        values.add(idValue);
                        Map<String, Object> nextValue = (Map) map.get(idValue);
                        String resourceAddress = address + last + "=" + idValue + "/";
                        // System.out.println("PATH " + path);
                        //System.out.println("ADDRESS " + resourceAddress);
                        if (spec.hasOp()) {
                            Map<String, String> resolvedIds = new HashMap<>();
                            resolvedIds.put(last, idValue);
                            feature = new ResolvedFeature(spec, resolvedIds, path + "." + idValue, resourceAddress);
                            ret.add(feature);
                            processResolvedFeature(allSpecs, nextValue, feature);
                        }
                        // We can have remaining keys in the next value, we must generate features for it
                        if (nextValue != null) {
                            // If we have some remaining keys they must be of type map otherwise it means that the  element is unknown,
                            for (String k : nextValue.keySet()) {
                                //System.out.println(k + "=>" + map.get(k));
                                Object val = nextValue.get(k);
                                if (val != null && !(val instanceof Map)) {
                                    throw new Exception("Invalid element " + k + " for " + path + "." + idValue);
                                }
                            }
                            navigatePath(path, resourceAddress, nextValue, allSpecs, ret, values, currentResolvedPath);
                        }
                        values.remove(values.size() - 1);
                    }
                }
            }
        } else {
            // Null spec
            if (value instanceof Map) {
                Map<String, Object> map = (Map) value;
                navigatePath(path, address, map, allSpecs, ret, values, resolvedPath);
            } else {
                if (value != null) {
                    throw new Exception("Invalid path " + path);
                }
            }
        }
        return ret;
    }

    private static String formatPath(String path) {
        String[] split = path.split("\\.");
        boolean eq = true;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            builder.append(s);
            if (eq) {
                builder.append("=");
            } else {
                builder.append("/");
            }
            eq = !eq;
        }
        return builder.toString();
    }

    private static void processResolvedFeature(Map<String, FeatureSpec> allSpecs, Map<String, Object> map, ResolvedFeature feature) throws Exception {
        feature.retrieveParamsAndAttributes(allSpecs, map);
        feature.addCommands();
    }

    // A resolved path means that we don't need more items in the path,
    // the feature is complete and we can generate commands for it.
    private static ResolvedFeature isResolvedPath(FeatureSpec spec, String path, List<String> values, String resolvedPath) throws Exception {
        // Unfortunately we can't rely on jboss-op/op-address, not all features have an op (eg: read-only that can't be created".
        String[] items = path.split("\\.");
        StringBuilder b = new StringBuilder();
        int valuesIndex = 0;
        Map<String, String> resolvedIds = new HashMap<>();
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            if (spec.hasParam(item)) {
                FeatureParameterSpec p = spec.getParam(item);
                if (item == null) {
                    throw new Exception("Invalid path " + path);
                }
                if (p.isFeatureId()) {
                    if (p.hasDefaultValue()) {
                        if ("GLN_UNDEFINED".equals(p.getDefaultValue())) {
                            continue;
                        }
                    }
                    b.append(item);
                    String val = ".XXX_NOT_RESOLVED";
                    if (p.hasDefaultValue()) {
                        val = p.getDefaultValue();
                        b.append(".").append(val);
                        if (i < items.length - 1) {
                            i += 1;
                            String next = items[i];
                            if (!next.equals(p.getDefaultValue())) {
                                throw new Exception("Invalid path " + path);
                            }
                        } else {
                            throw new Exception("Invalid path " + path);
                        }
                    } else {
                        if (values != null && (valuesIndex < values.size())) {
                            val = values.get(valuesIndex);
                            b.append(".").append(val);
                            valuesIndex += 1;
                        } else {
                            b.append(val);
                        }
                    }
                    resolvedIds.put(item, val);
                    if (i < items.length - 1) {
                        b.append(".");
                    }
                }
            } else {
                throw new Exception("Invalid path " + path);
            }
        }
        //System.out.println("Path" + path + " FEATURE_SPEC ID " + b.toString());
        //System.out.println(" RESOLVED PATH " + resolvedPath);
        //System.out.println(" COMPUTED " + b.toString());
        if (resolvedPath.equals(b.toString())) {
            return new ResolvedFeature(spec, resolvedIds, resolvedPath, "/" + formatPath(resolvedPath));
        } else {
            return null;
        }
    }

    // Continue to search for features present in this path.
    private static void navigatePath(String currentPath, String currentAddress, Map<String, Object> map,
            Map<String, FeatureSpec> allSpecs, List<ResolvedFeature> commands, List<String> values, String resolvedPath) throws Exception {
        for (String k : map.keySet()) {
            //System.out.println(k + "=>" + map.get(k));
            Object val = map.get(k);
            if (val != null && !(val instanceof Map)) {
                throw new Exception("Invalid yaml path: " + currentPath);
            }
            Map<String, Object> nextValue = (Map) val;
            String newPath = currentPath + "." + k;
            //System.out.println("New Path " + newPath);
            commands.addAll(getSpecs2(newPath, currentAddress, nextValue, allSpecs, values, resolvedPath + "." + k));
        }
    }
}
