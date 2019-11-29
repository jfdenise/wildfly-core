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
package org.jboss.as.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.jboss.as.controller.client.impl.AdditionalBootCliScriptInvoker;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author jdenise
 */
public class BootScriptInvoker implements AdditionalBootCliScriptInvoker {

    @Override
    public void runCliScript(ModelControllerClient client, File file) {
        CommandContext ctx;
        String props = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.cli.boot.script.properties", null);
        if (props != null) {
            File propsFile = new File(props);
            if (!propsFile.exists()) {
                // TODO i18n
                throw new RuntimeException("Could not find file " + propsFile.getAbsolutePath());
            }
            handleProperties(propsFile);
        }
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext();
            ctx.bindClient(client);
        } catch (CliInitializationException ex) {
            throw new RuntimeException(ex);
        }
        processFile(file, ctx);
    }

    private static void processFile(File file, final CommandContext cmdCtx) {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (cmdCtx.getExitCode() == 0 && !cmdCtx.isTerminated() && line != null) {
                cmdCtx.handleSafe(line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to process file '" + file.getAbsolutePath() + "'", e);
        } finally {
            StreamUtils.safeClose(reader);
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
