/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.server;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.jboss.as.controller.ModelControllerServiceInitialization;
import org.jboss.as.server.deployment.transformation.DeploymentTransformer;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Module;
/**
 *
 * @author jdenise
 */
public class ServiceLoaderInitializer {
        public static List<ModelControllerServiceInitialization> LOADERS = new ArrayList<>();
    static {
        System.out.println("INIT LOADERS");
        for (ModelControllerServiceInitialization init : ServiceLoader.load(ModelControllerServiceInitialization.class)) {
            System.out.println("ADD LOADER " + init);
            LOADERS.add(init);
        }
        ModuleClassLoader mcl = (ModuleClassLoader)ServiceLoaderInitializer.class.getClassLoader();
        Module mod = mcl.getModule();
        mod.registerServices(DeploymentTransformer.class);
    }
    public static void init() {
        // Do nothing
    }
    public static List<ModelControllerServiceInitialization> getLoaders() {
        return LOADERS;
    }
}
