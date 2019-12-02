/*
Copyright 2019 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.cli.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import org.jboss.as.controller.client.impl.AdditionalBootCliScriptInvoker;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * CLI script invoker. This is instantiated inside a server during boot in
 * admin-mode.
 *
 * @author jdenise
 */
public class BootScriptInvoker implements AdditionalBootCliScriptInvoker {

    private static final Logger logger = Logger.getLogger(BootScriptInvoker.class);

    @Override
    public void runCliScript(ModelControllerClient client, File file) {
        logger.info("Executing CLI Script invoker for file " + file);

        String log = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.cli.boot.script.logging", "false");
        final LogManager logManager = LogManager.getLogManager();
        // Turnoff logger
        if (!Boolean.parseBoolean(log)) {
            if (logManager instanceof org.jboss.logmanager.LogManager) {
                org.jboss.logmanager.LogManager jbossLogManager = (org.jboss.logmanager.LogManager) logManager;
                jbossLogManager.getLogger(CommandContext.class.getName()).setLevel(Level.OFF);
            }
        }
        CommandContextImpl ctx;
        String props = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.cli.boot.script.properties", null);

        if (props != null) {
            File propsFile = new File(props);
            if (!propsFile.exists()) {
                // TODO i18n
                throw new RuntimeException("Could not find file " + propsFile.getAbsolutePath());
            }
            handleProperties(propsFile);
        }
        File logFile = null;
        String logFilePath = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.cli.boot.script.output.file", null);
        if (logFilePath != null) {
            logFile = new File(logFilePath);
        }

        try {
            OutputStream output = null;
            if (logFile != null) {
                output = new FileOutputStream(logFile);
            }
            ctx = new CommandContextImpl(output);
            ctx.bindClient(client);

            processFile(file, ctx);
        } catch (Exception ex) {
            try {
                logger.error("Error applying " + file + " CLI script:");
                for (String line : Files.readAllLines(file.toPath())) {
                    logger.error(line);
                }
                logger.error("CLI execution output:");
                for (String line : Files.readAllLines(logFile.toPath())) {
                    logger.error(line);
                }
            } catch (IOException ex1) {
                RuntimeException rtex = new RuntimeException(ex1);
                rtex.addSuppressed(ex);
                throw rtex;
            }
            throw new RuntimeException(ex);
        }
        logger.info("Done executing CLI Script invoker for file " + file);
    }

    private static void processFile(File file, final CommandContext cmdCtx) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (cmdCtx.getExitCode() == 0 && !cmdCtx.isTerminated() && line != null) {
                logger.debug("Executing command " + line.trim());
                cmdCtx.handleSafe(line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            logger.error("Unexpected exception processing commands ", e);
            throw new IllegalStateException("Failed to process file '" + file.getAbsolutePath() + "'", e);
        } finally {
            StreamUtils.safeClose(reader);
        }
        String warnFile = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.cli.boot.script.warn.file", null);
        if (warnFile != null) {
            File warns = new File(warnFile);
            if (warns.exists()) {
                for (String line : Files.readAllLines(warns.toPath())) {
                    logger.warn(line);
                }
            }

        }
        String errorFile = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.cli.boot.script.error.file", null);
        if (errorFile != null) {
            File errors = new File(errorFile);
            if (errors.exists()) {
                logger.error("Error applying " + file + " CLI script. The Operations were executed but "
                        + "there were unexpected values. See list of errors in " + errors);
                for (String line : Files.readAllLines(errors.toPath())) {
                    logger.error(line);
                }
            }

        }
        if (cmdCtx.getExitCode() != 0 || cmdCtx.isTerminated()) {
            throw new RuntimeException("Error applying " + file + " CLI script.");
        }
    }

    private static void handleProperties(File propertiesFile) {
        final Properties props = new Properties();
        try ( InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(propertiesFile), StandardCharsets.UTF_8)) {
            props.load(inputStreamReader);
            for (String key : props.stringPropertyNames()) {
                WildFlySecurityManager.setPropertyPrivileged(key, props.getProperty(key));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (String key : props.stringPropertyNames()) {
            WildFlySecurityManager.setPropertyPrivileged(key, props.getProperty(key));
        }
    }

}
