/*
 * Copyright 2016-2020 Red Hat, Inc. and/or its affiliates
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import static java.lang.System.getProperties;
import static java.lang.System.getSecurityManager;
import static java.lang.System.getenv;
import static java.lang.System.setProperty;
import java.nio.file.Path;
import java.security.AccessController;
import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.DelegatingModelControllerClient;
import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.Main;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerService;
import org.jboss.as.server.SystemExiter;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.Value;
import org.wildfly.core.jar.runtime._private.BootableJarLogger;

class Server {

    interface ShutdownHandler {

        void shutdown(int status);
    }

    private static final String MODULE_ID_VFS = "org.jboss.vfs";
    private final PropertyChangeListener processStateListener;
    private final String[] cmdargs;
    private final Properties systemProps;
    private final Map<String, String> systemEnv;
    private final ModuleLoader moduleLoader;
    private final ClassLoader bootableModuleCL;
    private ServiceContainer serviceContainer;
    private ControlledProcessState.State currentProcessState;
    private ModelControllerClient modelControllerClient;
    private ExecutorService executorService;
    private ProcessStateNotifier processStateNotifier;
    private final ShutdownHandler shutdownHandler;

    private Server(String[] cmdargs, Properties systemProps,
            Map<String, String> systemEnv, ModuleLoader moduleLoader,
            ClassLoader bootableModuleCL,
            ShutdownHandler shutdownHandler) {
        this.cmdargs = cmdargs;
        this.systemProps = systemProps;
        this.systemEnv = systemEnv;
        this.moduleLoader = moduleLoader;
        this.bootableModuleCL = bootableModuleCL;
        this.shutdownHandler = shutdownHandler;

        processStateListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("currentState".equals(evt.getPropertyName())) {
                    ControlledProcessState.State newState = (ControlledProcessState.State) evt.getNewValue();
                    establishModelControllerClient(newState, false);
                }
            }
        };
    }

    public static Server newSever(Path jbossHome, String[] cmdargs, ModuleLoader moduleLoader, ClassLoader bootableModuleCL, ShutdownHandler shutdownHandler) {
        setPropertyPrivileged(ServerEnvironment.HOME_DIR, jbossHome.toString());
        setupVfsModule(moduleLoader);
        Properties sysprops = getSystemPropertiesPrivileged();
        Map<String, String> sysenv = getSystemEnvironmentPrivileged();
        return new Server(cmdargs, sysprops, sysenv, moduleLoader, bootableModuleCL, shutdownHandler);
    }

    static String setPropertyPrivileged(final String name, final String value) {
        final SecurityManager sm = getSecurityManager();
        if (sm == null) {
            return setProperty(name, value);
        } else {
            return doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return setProperty(name, value);
                }
            });
        }
    }

    private static void setupVfsModule(final ModuleLoader moduleLoader) {
        final ModuleIdentifier vfsModuleID = ModuleIdentifier.create(MODULE_ID_VFS);
        final Module vfsModule;
        try {
            vfsModule = moduleLoader.loadModule(vfsModuleID);
        } catch (final ModuleLoadException mle) {
            throw BootableJarLogger.ROOT_LOGGER.moduleLoaderError(mle, MODULE_ID_VFS, moduleLoader);
        }
        Module.registerURLStreamHandlerFactoryModule(vfsModule);
    }

    private static Map<String, String> getSystemEnvironmentPrivileged() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return getenv();
        }
        return doPrivileged((PrivilegedAction<Map<String, String>>) System::getenv);
    }

    private static Properties getSystemPropertiesPrivileged() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return getProperties();
        }
        return doPrivileged((PrivilegedAction<Properties>) System::getProperties);
    }

    public synchronized ModelControllerClient getModelControllerClient() {
        return modelControllerClient == null ? null : new DelegatingModelControllerClient(new DelegatingModelControllerClient.DelegateProvider() {
            @Override
            public ModelControllerClient getDelegate() {
                return getActiveModelControllerClient();
            }
        });
    }

    public void start() throws Exception {
        ClassLoader tccl = getTccl();
        try {
            setTccl(bootableModuleCL);
            Bootstrap bootstrap = null;
            try {
                final long startTime = System.currentTimeMillis();

                // Take control of server use of System.exit
                // In order to control jbossHome cleanup being done after server stop.
                SystemExiter.initialize(new SystemExiter.Exiter() {
                    @Override
                    public void exit(int status) {
                        Server.this.exit();
                        shutdownHandler.shutdown(status);
                    }
                });

                // Determine the ServerEnvironment
                ServerEnvironment serverEnvironment = Main.determineEnvironment(cmdargs, systemProps, systemEnv, ServerEnvironment.LaunchType.STANDALONE, startTime).getServerEnvironment();
                if (serverEnvironment == null) {
                    // Nothing to do
                    return;
                }
                bootstrap = Bootstrap.Factory.newInstance();

                Bootstrap.Configuration configuration = new Bootstrap.Configuration(serverEnvironment);

                /*
                     * This would setup an {@link TransientConfigurationPersister} which does not persist anything
                     *
                    final ExtensionRegistry extensionRegistry = configuration.getExtensionRegistry();
                    final Bootstrap.ConfigurationPersisterFactory configurationPersisterFactory = new Bootstrap.ConfigurationPersisterFactory() {
                        @Override
                        public ExtensibleConfigurationPersister createConfigurationPersister(ServerEnvironment serverEnvironment, ExecutorService executorService) {
                            final QName rootElement = new QName(Namespace.CURRENT.getUriString(), "server");
                            final StandaloneXml parser = new StandaloneXml(Module.getBootModuleLoader(), executorService, extensionRegistry);
                            final File configurationFile = serverEnvironment.getServerConfigurationFile().getBootFile();
                            XmlConfigurationPersister persister = new TransientConfigurationPersister(configurationFile, rootElement, parser, parser);
                            for (Namespace namespace : Namespace.domainValues()) {
                                if (!namespace.equals(Namespace.CURRENT)) {
                                    persister.registerAdditionalRootElement(new QName(namespace.getUriString(), "server"), parser);
                                }
                            }
                            extensionRegistry.setWriterRegistry(persister);
                            return persister;
                        }
                    };
                    configuration.setConfigurationPersisterFactory(configurationPersisterFactory);
                 */
                configuration.setModuleLoader(moduleLoader);

                Future<ServiceContainer> future = bootstrap.startup(configuration, Collections.<ServiceActivator>emptyList());

                serviceContainer = future.get();

                executorService = Executors.newCachedThreadPool();

                @SuppressWarnings({"unchecked", "deprecation"})
                final Value<ProcessStateNotifier> processStateNotifierValue = (Value<ProcessStateNotifier>) serviceContainer.getRequiredService(ControlledProcessStateService.SERVICE_NAME);
                processStateNotifier = processStateNotifierValue.getValue();
                processStateNotifier.addPropertyChangeListener(processStateListener);
                establishModelControllerClient(processStateNotifier.getCurrentState(), true);

            } catch (RuntimeException rte) {
                if (bootstrap != null) {
                    bootstrap.failed();
                }
                throw rte;
            } catch (Exception ex) {
                if (bootstrap != null) {
                    bootstrap.failed();
                }
                throw BootableJarLogger.ROOT_LOGGER.cannotStartServer(ex);
            }
        } finally {
            setTccl(tccl);
        }
    }

    private void exit() {

        if (serviceContainer != null) {
            try {
                serviceContainer.shutdown();

                serviceContainer.awaitTermination();
            } catch (RuntimeException rte) {
                throw rte;
            } catch (InterruptedException ite) {
                BootableJarLogger.ROOT_LOGGER.error(ite.getLocalizedMessage(), ite);
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                BootableJarLogger.ROOT_LOGGER.error(ex.getLocalizedMessage(), ex);
            }
        }
        if (processStateNotifier != null) {
            processStateNotifier.removePropertyChangeListener(processStateListener);
            processStateNotifier = null;
        }
        if (executorService != null) {
            try {
                executorService.shutdown();

                // 10 secs is arbitrary, but if the service container is terminated,
                // no good can happen from waiting for ModelControllerClient requests to complete
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (RuntimeException rte) {
                throw rte;
            } catch (InterruptedException ite) {
                BootableJarLogger.ROOT_LOGGER.error(ite.getLocalizedMessage(), ite);
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                BootableJarLogger.ROOT_LOGGER.error(ex.getLocalizedMessage(), ex);
            }
        }

        SystemExiter.initialize(SystemExiter.Exiter.DEFAULT);
    }

    private synchronized void establishModelControllerClient(ControlledProcessState.State state, boolean storeState) {
        ModelControllerClient newClient = null;
        if (state != ControlledProcessState.State.STOPPING && state != ControlledProcessState.State.STOPPED && serviceContainer != null) {
            ModelControllerClientFactory clientFactory;
            try {
                @SuppressWarnings("unchecked")
                final ServiceController clientFactorySvc
                        = serviceContainer.getService(ServerService.JBOSS_SERVER_CLIENT_FACTORY);
                clientFactory = (ModelControllerClientFactory) clientFactorySvc.getValue();
            } catch (RuntimeException e) {
                // Either NPE because clientFactorySvc was not installed, or ISE from getValue because not UP
                clientFactory = null;
            }
            if (clientFactory != null) {
                newClient = clientFactory.createSuperUserClient(executorService, true);
            }
        }
        modelControllerClient = newClient;
        if (storeState || currentProcessState == null) {
            currentProcessState = state;
        }
    }

    private synchronized ModelControllerClient getActiveModelControllerClient() {
        switch (currentProcessState) {
            case STOPPING: {
                throw BootableJarLogger.ROOT_LOGGER.processIsStopping();
            }
            case STOPPED: {
                throw BootableJarLogger.ROOT_LOGGER.processIsStopped();
            }
            case STARTING:
            case RUNNING: {
                if (modelControllerClient == null) {
                    // Service wasn't available when we got the ControlledProcessState
                    // state change notification; try again
                    establishModelControllerClient(currentProcessState, false);
                    if (modelControllerClient == null) {
                        throw BootableJarLogger.ROOT_LOGGER.processIsReloading();
                    }
                }
                // fall through
            }
            default: {
                return modelControllerClient;
            }
        }
    }

    static ClassLoader getTccl() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    static void setTccl(final ClassLoader cl) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(cl);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Thread.currentThread().setContextClassLoader(cl);
                    return null;
                }
            });
        }
    }
}
