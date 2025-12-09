/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.graal;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 *
 * @author jdenise
 */
public class GraalRecorder {

    public static class Record {

        public String id;
        public Object content;
    }

    public static class UnresolvedRecord {

        public String id;
        public Path file;
    }
    private static final Map<String, Map<String, List<Record>>> RECORDS = new TreeMap<>();
    private static final Map<String, Map<String, List<UnresolvedRecord>>> LOADED_RECORDS = new TreeMap<>();

    public static void record(String key, String deploymentName, Object metadata, String id) {
        Map<String, List<Record>> local = RECORDS.get(key);
        if (local == null) {
            local = new HashMap<>();
            RECORDS.put(key, local);
        }
        List<Record> recs = local.get(deploymentName);
        if (recs == null) {
            recs = new ArrayList<>();
            local.put(deploymentName, recs);
        }
        Record rec = new Record();
        rec.id = id;
        rec.content = metadata;
        recs.add(rec);
    }

    public static void store(Path jbossHome) throws IOException {
        Path p = jbossHome.resolve("graal-recording");
        System.out.println("Store Graal recording in " + p);
        Files.createDirectories(p);
        for (String key : RECORDS.keySet()) {
            Path keyPath = p.resolve(key);
            Files.createDirectories(keyPath);
            Map<String, List<Record>> records = RECORDS.get(key);
            for (String k : records.keySet()) {
                for (Record rec : records.get(k)) {
                    Path dir = keyPath.resolve(k);
                    Files.createDirectories(dir);
                    Path file = dir.resolve(rec.id + ".ser");
                    try (FileOutputStream fout = new FileOutputStream(file.toFile())) {
                        try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
                            oos.writeObject(rec.content);
                        }
                    }
                }
            }
        }
    }

    public static void load(Path jbossHome) throws Exception {
        Path p = jbossHome.resolve("graal-recording");
        System.out.println("Loading Graal recording from " + p);
        try (Stream<Path> keys = Files.list(p)) {
            final Iterator<Path> i = keys.iterator();
            while (i.hasNext()) {
                Path keyPath = i.next();
                String key = keyPath.getFileName().toString();
                Map<String, List<UnresolvedRecord>> map = new HashMap<>();
                LOADED_RECORDS.put(key, map);
                try (Stream<Path> deployments = Files.list(keyPath)) {
                    final Iterator<Path> dep = deployments.iterator();
                    while (dep.hasNext()) {
                        Path depPath = dep.next();
                        String moduleName = depPath.getFileName().toString();
                        System.out.println("Loading records for " + moduleName);
                        List<UnresolvedRecord> records = new ArrayList<>();
                        map.put(moduleName, records);
                        try (Stream<Path> ser = Files.list(depPath)) {
                            final Iterator<Path> iser = ser.iterator();
                            while (iser.hasNext()) {
                                Path file = iser.next();
                                UnresolvedRecord rec = new UnresolvedRecord();
                                rec.id = file.getFileName().toString();
                                rec.file = file;
                                records.add(rec);
                            }
                        }
                    }
                }
            }
        }
    }

    public static Map<String, List<UnresolvedRecord>> getUnresolvedRecords(String key) throws Exception {
        Map<String, List<UnresolvedRecord>> records = LOADED_RECORDS.get(key);
        if (records == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(records);
    }

    public static Map<String, List<Record>> resolve(String key) throws Exception {
        Map<String, List<Record>> records = RECORDS.get(key);
        if (records == null) {
            return Collections.emptyMap();
        }
        for (String k : records.keySet()) {
            List<Record> recs = records.get(k);
            for (Record rec : recs) {
                if (rec.id == null) {
                    Path file = (Path) rec.content;
                    rec.id = file.getFileName().toString();
                    FileInputStream streamIn = new FileInputStream(file.toFile());
                    ObjectInputStream objectIS = new ObjectInputStream(streamIn);
                    rec.content = objectIS.readObject();
                }
            }
        }
        return Collections.unmodifiableMap(records);
    }
}
