/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.deployment;


import static java.lang.Long.getLong;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service in charge with cleaning left over contents from the content repository.
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ContentCleanerService implements Service<Void> {

   /**
     * For testing purpose only.
     *
     * @deprecated DON'T USE IT.
     */
    @Deprecated
    private static final String UNSUPPORTED_PROPERTY = "org.wildfly.unsupported.content.repository.obsolescence";
    /**
     * The conten repository cleaner will test content for clean-up every 5 minutes.
     */
    public static final long DEFAULT_INTERVAL = getSecurityManager() == null ? getLong(UNSUPPORTED_PROPERTY, 300000L) : doPrivileged((PrivilegedAction<Long>) () -> getLong(UNSUPPORTED_PROPERTY, 300000L));
    /**
     * Standard ServiceName under which a service controller for an instance of
     * @code Service<ContentRepository> would be registered.
     */
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("content-repository-cleaner");

    private final InjectedValue<ModelControllerClientFactory> clientFactoryValue = new InjectedValue<>();
    private final InjectedValue<ScheduledExecutorService> scheduledExecutorValue = new InjectedValue<>();
    private final InjectedValue<ControlledProcessStateService> controlledProcessStateServiceValue = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<>();

    private ContentRepositoryCleaner deploymentContentCleaner;
    private final long interval;
    private final boolean server;
    private final TimeUnit unit;

    public static void addService(final ServiceTarget serviceTarget, final ServiceName clientFactoryService, final ServiceName scheduledExecutorServiceName) {
        final ContentCleanerService service = new ContentCleanerService(true);
        ServiceBuilder<Void> builder = serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(clientFactoryService, ModelControllerClientFactory.class, service.clientFactoryValue)
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.controlledProcessStateServiceValue)
                .addDependency(scheduledExecutorServiceName, ScheduledExecutorService.class, service.scheduledExecutorValue);
        Services.addServerExecutorDependency(builder, service.executorServiceValue);
        builder.install();
    }

    public static void addServiceOnHostController(final ServiceTarget serviceTarget, final ServiceName hostControllerServiceName, final ServiceName clientFactoryServiceName,
                                                  final ServiceName hostControllerExecutorServiceName, final ServiceName scheduledExecutorServiceName) {
        final ContentCleanerService service = new ContentCleanerService(false);
        ServiceBuilder<Void> builder = serviceTarget.addService(SERVICE_NAME, service)
                .addDependency(clientFactoryServiceName, ModelControllerClientFactory.class, service.clientFactoryValue)
                .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.controlledProcessStateServiceValue)
                .addDependency(hostControllerExecutorServiceName, ExecutorService.class, service.executorServiceValue)
                .addDependency(scheduledExecutorServiceName, ScheduledExecutorService.class, service.scheduledExecutorValue);
        builder.install();
    }

    ContentCleanerService(final boolean server) {
        this.interval = DEFAULT_INTERVAL;
        this.unit = TimeUnit.MILLISECONDS;
        this.server = server;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        this.deploymentContentCleaner = new ContentRepositoryCleaner(clientFactoryValue.getValue().createSuperUserClient(
                executorServiceValue.getValue()), controlledProcessStateServiceValue.getValue(),
                scheduledExecutorValue.getValue(), unit.toMillis(interval), server);
        deploymentContentCleaner.startScan();
    }

    @Override
    public synchronized void stop(StopContext context) {
        final ContentRepositoryCleaner contentCleaner = this.deploymentContentCleaner;
        this.deploymentContentCleaner = null;
        contentCleaner.stopScan();
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}
