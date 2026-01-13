/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.server.moduleservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.modules.ClassCache;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service that loads and re-links a module once all the modules dependencies are available.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ModuleLoadService implements Service<Module> {

    private final InjectedValue<ServiceModuleLoader> serviceModuleLoader = new InjectedValue<>();
    private final InjectedValue<ModuleDefinition> moduleDefinitionInjectedValue = new InjectedValue<>();
    private final List<ModuleDependency> allDependencies;
    private final Collection<ModuleDependency> systemDependencies;
    private final Collection<ModuleDependency> userDependencies;
    private final Collection<ModuleDependency> localDependencies;

    private volatile Module module;
    public static Module FROM_BUILD;
    private ModuleLoadService(final Collection<ModuleDependency> systemDependencies, final Collection<ModuleDependency> localDependencies, final Collection<ModuleDependency> userDependencies) {
        this.systemDependencies = systemDependencies;
        this.localDependencies = localDependencies;
        this.userDependencies = userDependencies;

        this.allDependencies = new ArrayList<>();
        this.allDependencies.addAll(systemDependencies);
        this.allDependencies.addAll(localDependencies);
        this.allDependencies.addAll(userDependencies);
    }

    private ModuleLoadService(final List<ModuleDependency> aliasDependencies) {
        this.systemDependencies = Collections.emptyList();
        this.localDependencies = Collections.emptyList();
        this.userDependencies = Collections.emptyList();

        this.allDependencies = new ArrayList<>(aliasDependencies);
    }

    private ModuleLoadService() {
        this.systemDependencies = Collections.emptyList();
        this.localDependencies = Collections.emptyList();
        this.userDependencies = Collections.emptyList();

        this.allDependencies = Collections.emptyList();
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            final ServiceModuleLoader moduleLoader = serviceModuleLoader.getValue();
            Module module;
            if (Boolean.getBoolean("org.wildfly.graal")) {
                ServerLogger.AS_ROOT_LOGGER.info("Re-using deployment module from build phase.");
                module = FROM_BUILD;
                try {
                    module.restorePermissions();
                } catch (Exception ex) {
                    throw new StartException(ex);
                }
            } else {
                module = moduleLoader.loadModule(moduleDefinitionInjectedValue.getValue().getModuleName());
                moduleLoader.relinkModule(module);
                if (Boolean.getBoolean("org.wildfly.graal.build.time")) {
                    FROM_BUILD = module;
//                    for(DependencySpec d : module.getDependencies()) {
//                        System.out.println(d);
//                    }
                    try {
                        //Install cache
                        String cacheClass = System.getProperty("org.wildfly.graal.cache.class");
                        ClassCache cache = (ClassCache) Class.forName(cacheClass).newInstance();
                        FROM_BUILD.setClassCache(cache);
                        System.out.println("Discovering services for deployment module.");
                        for (String serviceClass : FROM_BUILD.getServices()) {
                            if (!serviceClass.startsWith("java.lang.")) {
                                //System.out.println(serviceClass);
                                Set<String> impl = FROM_BUILD.getCache().addServiceToCache(serviceClass);
//                                for(String s : impl) {
//                                    System.out.println("   " + s);
//                                }
                            }
                        }
                        System.out.println("Services discovery done");
                        String classes = System.getProperty("org.wildfly.graal.deployment.classes");
                        if (classes != null) {
                            String[] carray = classes.split(",");
                            for (String clazz : carray) {
                                FROM_BUILD.getCache().addClassToCache(clazz);
                            }
                        }
                    } catch (Exception ex) {
                        throw new StartException(ex);
                    }
                }
            }
            for (ModuleDependency dependency : allDependencies) {
                if (dependency.isUserSpecified()) {
                    final String id = dependency.getDependencyModule();
                    try {
                        String val = moduleLoader.loadModule(id).getProperty("jboss.api");
                        if (val != null) {
                            if (val.equals("private")) {
                                ServerLogger.PRIVATE_DEP_LOGGER.privateApiUsed(moduleDefinitionInjectedValue.getValue().getModuleName(), id);
                            } else if (val.equals("unsupported")) {
                                ServerLogger.UNSUPPORTED_DEP_LOGGER.unsupportedApiUsed(moduleDefinitionInjectedValue.getValue().getModuleName(), id);
                            } else if (val.equals("deprecated")) {
                                ServerLogger.DEPRECATED_DEP_LOGGER.deprecatedApiUsed(moduleDefinitionInjectedValue.getValue().getModuleName(), id);
                            }
                        }
                    } catch (ModuleNotFoundException ignore) {
                        //can happen with optional dependencies
                    }
                }
            }
            this.module = module;
        } catch (ModuleLoadException e) {
            throw ServerLogger.ROOT_LOGGER.failedToLoadModule(moduleDefinitionInjectedValue.getValue().getModuleName(), e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        try {
            // we don't actually unload the module, that is taken care of by the service module loader
            // Clean the permissions so they can be reconstructed at startup.
            module.cleanupPermissions();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        module = null;
    }

    @Override
    public Module getValue() throws IllegalStateException, IllegalArgumentException {
        return module;
    }

    private static ServiceName install(final ServiceTarget target, final String identifier, ModuleLoadService service) {
        final ServiceName serviceName = ServiceModuleLoader.moduleServiceName(identifier);
        final ServiceBuilder<Module> builder = target.addService(serviceName, service);

        builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ServiceModuleLoader.class, service.getServiceModuleLoader());
        builder.addDependency(ServiceModuleLoader.moduleSpecServiceName(identifier), ModuleDefinition.class, service.getModuleDefinitionInjectedValue());
        builder.requires(ServiceModuleLoader.moduleResolvedServiceName(identifier)); //don't attempt to load until all dependent module specs are up, even transitive ones
        builder.setInitialMode(Mode.ON_DEMAND);

        builder.install();
        return serviceName;
    }

    /**
     * Installs a ModuleLoadService that will load the module with the given identifier.
     *
     * @param target     the service target
     * @param identifier the module identifier in its canonical form
     * @return the service name
     */
    public static ServiceName install(final ServiceTarget target, final String identifier) {
        final ModuleLoadService service = new ModuleLoadService();
        return install(target, identifier, service);
    }

    /**
     * Installs a ModuleLoadService that will load the module with the given identifier and its dependencies.
     *
     * @param target the service target
     * @param identifier the module identifier in its canonical form
     * @param systemDependencies the system dependencies
     * @param localDependencies the local dependencies
     * @param userDependencies the user dependencies
     *
     * @return the service name
     */
    public static ServiceName install(final ServiceTarget target, final String identifier, final Collection<ModuleDependency> systemDependencies, final Collection<ModuleDependency> localDependencies, final Collection<ModuleDependency> userDependencies) {
        final ModuleLoadService service = new ModuleLoadService(systemDependencies, localDependencies, userDependencies);
        return install(target, identifier, service);
    }

    /**
     * Installs a ModuleLoadService that will load the module with the given identifier and its aliases.
     *
     * @param target  the service target
     * @param identifier the module identifier in its canonical form
     * @param aliases the list of aliases
     * @return the service name
     */
    public static ServiceName installAliases(final ServiceTarget target, final String identifier, final List<String> aliases) {
        final ArrayList<ModuleDependency> dependencies = new ArrayList<>(aliases.size());
        for (final String i : aliases) {
            dependencies.add(ModuleDependency.Builder.of(null, i).build());
        }
        final ModuleLoadService service = new ModuleLoadService(dependencies);
        return install(target, identifier, service);
    }

    public InjectedValue<ServiceModuleLoader> getServiceModuleLoader() {
        return serviceModuleLoader;
    }

    public InjectedValue<ModuleDefinition> getModuleDefinitionInjectedValue() {
        return moduleDefinitionInjectedValue;
    }

    public List<ModuleDependency> getSystemDependencies() {
        return new ArrayList<>(systemDependencies);
    }

    public List<ModuleDependency> getUserDependencies() {
        return new ArrayList<>(userDependencies);
    }

    public List<ModuleDependency> getLocalDependencies() {
        return new ArrayList<>(localDependencies);
    }

    @Override
    public void passivate() {
        try {
            // we don't actually unload the module, that is taken care of by the service module loader
            // Clean the permissions so they can be reconstructed at startup.
            module.cleanupPermissions();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void resume() {
        try {
            module.restorePermissions();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}