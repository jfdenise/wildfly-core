/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.io;

import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Describes capabilities exposed by IO subsystem.
 */
public interface IOServiceDescriptor {
    /** Describes the maximum number of threads allocated to all active workers */
    NullaryServiceDescriptor<Integer> MAX_THREADS = NullaryServiceDescriptor.of("org.wildfly.io.max-threads", Integer.class);

    /** Describes the default worker */
    NullaryServiceDescriptor<XnioWorkerSupplier> DEFAULT_WORKER = NullaryServiceDescriptor.of("org.wildfly.io.default-worker", XnioWorkerSupplier.class);
    /** Describes a named worker */
    UnaryServiceDescriptor<XnioWorkerSupplier> NAMED_WORKER = UnaryServiceDescriptor.of("org.wildfly.io.worker", XnioWorkerSupplier.class);
    /** Resolves to a named or default worker **/
    UnaryServiceDescriptor<XnioWorkerSupplier> WORKER = UnaryServiceDescriptor.of(NAMED_WORKER.getName(), DEFAULT_WORKER);
}
