/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.wildfly.core.jar.boot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jboss.modules.FileSystemClassPathModuleFinder;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * Bootable jar Main class.
 *
 * @author jdenise
 */
public final class Main {

    private static final String SYSPROP_KEY_CLASS_PATH = "java.class.path";
    private static final String SYSPROP_KEY_MODULE_PATH = "module.path";
    private static final String SYSPROP_KEY_SYSTEM_MODULES = "jboss.modules.system.pkgs";

    private static final String JBOSS_MODULES_DIR_NAME = "modules";

    private static final String MODULE_ID_JAR_RUNTIME = "org.wildfly.bootable-jar";

    private static final String BOOTABLE_JAR = "org.wildfly.core.jar.runtime.BootableJar";
    private static final String BOOTABLE_JAR_RUN_METHOD = "run";

    private static final String INSTALL_DIR = "--install-dir";

    private static final String WILDFLY_RESOURCE = "/wildfly.zip";
    private static final String WILDFLY_BOOTABLE_TMP_DIR_PREFIX = "wildfly-bootable-server";
    static Instrumentation instrumentation;

    public static void main(String[] args) throws Exception {

        List<String> filteredArgs = new ArrayList<>();
        final List<String> agentJars = new ArrayList<>();
        Path installDir = null;
        for (String arg : args) {
            if (arg.startsWith(INSTALL_DIR)) {
                installDir = Paths.get(getValue(arg));
            } else if (arg.startsWith("-javaagent:")) {
                agentJars.add(arg.substring(11));
            } else {
                filteredArgs.add(arg);
            }
        }

        installDir = installDir == null ? Files.createTempDirectory(WILDFLY_BOOTABLE_TMP_DIR_PREFIX) : installDir;
        long t = System.currentTimeMillis();
        try (InputStream wf = Main.class.getResourceAsStream(WILDFLY_RESOURCE)) {
            if (wf == null) {
                throw new Exception("Resource " + WILDFLY_RESOURCE + " doesn't exist, can't run.");
            }
            unzip(wf, installDir.toFile());
        }
        runBootableJar(installDir, filteredArgs, System.currentTimeMillis() - t, agentJars);
    }

    private static String getValue(String arg) {
        int sep = arg.indexOf("=");
        if (sep == -1 || sep == arg.length() - 1) {
            throw new RuntimeException("Invalid argument " + arg + ", no value provided");
        }
        return arg.substring(sep + 1);
    }

    private static void runBootableJar(Path jbossHome, List<String> arguments, Long unzipTime, List<String> agentJars) throws Exception {
        final String modulePath = jbossHome.resolve(JBOSS_MODULES_DIR_NAME).toAbsolutePath().toString();
        ModuleLoader moduleLoader = setupModuleLoader(modulePath);
        handleAgents(moduleLoader, agentJars);
        final Module bootableJarModule;
        try {
            bootableJarModule = moduleLoader.loadModule(MODULE_ID_JAR_RUNTIME);
        } catch (final ModuleLoadException mle) {
            throw new Exception(mle);
        }

        final ModuleClassLoader moduleCL = bootableJarModule.getClassLoader();
        final Class<?> bjFactoryClass;
        try {
            bjFactoryClass = moduleCL.loadClass(BOOTABLE_JAR);
        } catch (final ClassNotFoundException cnfe) {
            throw new Exception(cnfe);
        }
        Method runMethod;
        try {
            runMethod = bjFactoryClass.getMethod(BOOTABLE_JAR_RUN_METHOD, Path.class, List.class, ModuleLoader.class, ModuleClassLoader.class, Long.class);
        } catch (final NoSuchMethodException nsme) {
            throw new Exception(nsme);
        }
        runMethod.invoke(null, jbossHome, arguments, moduleLoader, moduleCL, unzipTime);
    }

    private static void handleAgents(ModuleLoader moduleLoader, List<String> agentJars) throws Exception {
        if (!agentJars.isEmpty()) {
            final Instrumentation instrumentation = Main.instrumentation;
            if (instrumentation == null) {
                // we have to self-attach (todo later)
                System.err.println("Not started in agent mode (self-attach not supported yet)");
                //usage();
                System.exit(1);
            }
            final ModuleLoader agentLoader = new ModuleLoader(new FileSystemClassPathModuleFinder(moduleLoader));
            for (String agentJarArg : agentJars) {
                final String agentJar;
                final String agentArgs;
                final int i = agentJarArg.indexOf('=');
                if (i > 0) {
                    agentJar = agentJarArg.substring(0, i);
                    if (agentJarArg.length() > (i + 1)) {
                        agentArgs = agentJarArg.substring(i + 1);
                    } else {
                        agentArgs = "";
                    }
                } else {
                    agentJar = agentJarArg;
                    agentArgs = "";
                }

                final Module agentModule;
                try {
                    agentModule = agentLoader.loadModule(new File(agentJar).getAbsolutePath());
                } catch (ModuleLoadException ex) {
                    System.err.printf("Cannot load agent JAR %s: %s", agentJar, ex);
                    System.exit(1);
                    throw new IllegalStateException();
                }
                final ModuleClassLoader classLoader = agentModule.getClassLoader();
                final InputStream is = classLoader.getResourceAsStream("META-INF/MANIFEST.MF");
                final Manifest manifest;
                if (is == null) {
                    System.err.printf("Agent JAR %s has no manifest", agentJar);
                    System.exit(1);
                    throw new IllegalStateException();
                }
                try {
                    manifest = new Manifest();
                    manifest.read(is);
                    is.close();
                } catch (IOException e) {
                    try {
                        is.close();
                    } catch (IOException e2) {
                        e2.addSuppressed(e);
                        throw e2;
                    }
                    throw e;
                }
                // Note that this does not implement agent invocation as defined on
                // https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html. This is also not
                // done on the system class path which means some agents that rely on that may not work well here.
                final Attributes attributes = manifest.getMainAttributes();
                final String preMainClassName = attributes.getValue("Premain-Class");
                if (preMainClassName != null) {
                    final Class<?> preMainClass = Class.forName(preMainClassName, true, classLoader);
                    Object[] premainArgs;
                    Method premain;
                    try {
                        premain = preMainClass.getDeclaredMethod("premain", String.class, Instrumentation.class);
                        premainArgs = new Object[]{agentArgs, instrumentation};
                    } catch (NoSuchMethodException ignore) {
                        // If the method is not found we should check for the string only method
                        premain = preMainClass.getDeclaredMethod("premain", String.class);
                        premainArgs = new Object[]{agentArgs};
                    } catch (Exception e) {
                        System.out.printf("Failed to find premain method: %s", e);
                        System.exit(1);
                        throw new IllegalStateException();
                    }
                    try {
                        premain.invoke(null, premainArgs);
                    } catch (InvocationTargetException e) {
                        System.out.printf("Execution of premain method failed: %s", e.getCause());
                        System.exit(1);
                        throw new IllegalStateException();
                    }
                } else {
                    System.out.printf("Agent JAR %s has no premain method", agentJar);
                    System.exit(1);
                    throw new IllegalStateException();
                }
            }
        }
    }

    private static void unzip(InputStream wf, File dir) throws Exception {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(wf)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(dir, fileName);
                if (fileName.endsWith("/")) {
                    newFile.mkdirs();
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                    continue;
                }
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
        }
    }

    private static String trimPathToModulesDir(String modulePath) {
        int index = modulePath.indexOf(File.pathSeparator);
        return index == -1 ? modulePath : modulePath.substring(0, index);
    }

    // Copied from Embedded, lightly updated.
    private static ModuleLoader setupModuleLoader(final String modulePath) {
        assert modulePath != null : "modulePath not null";

        // verify the first element of the supplied modules path exists, and if it does not, stop and allow the user to correct.
        // Once modules are initialized and loaded we can't change Module.BOOT_MODULE_LOADER (yet).
        final Path moduleDir = Paths.get(trimPathToModulesDir(modulePath));
        if (Files.notExists(moduleDir) || !Files.isDirectory(moduleDir)) {
            throw new RuntimeException("The first directory of the specified module path " + modulePath + " is invalid or does not exist.");
        }

        final String classPath = System.getProperty(SYSPROP_KEY_CLASS_PATH);
        try {
            // Set up sysprop env
            System.clearProperty(SYSPROP_KEY_CLASS_PATH);
            System.setProperty(SYSPROP_KEY_MODULE_PATH, modulePath);

            final StringBuilder packages = new StringBuilder("org.jboss.modules");
            String custompackages = System.getProperty(SYSPROP_KEY_SYSTEM_MODULES);
            if (custompackages != null) {
                packages.append(",").append(custompackages);
            }
            System.setProperty(SYSPROP_KEY_SYSTEM_MODULES, packages.toString());

            // Get the module loader
            return Module.getBootModuleLoader();
        } finally {
            // Return to previous state for classpath prop
            if (classPath != null) {
                System.setProperty(SYSPROP_KEY_CLASS_PATH, classPath);
            }
        }
    }
}
