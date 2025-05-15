/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.platform.mbean;

import java.lang.management.ManagementFactory;
import org.jboss.as.controller.OperationFailedException;

/**
 * Implements the access to the com.sun.management.ThreadMXBean
 *
 * @author jdenise
 */
class ExtendedThreadMBean extends AbstractExtendedMBean {

    static final String THREAD_ALLOCATED_MEMORY_ENABLED_ATTRIBUTE = "ThreadAllocatedMemoryEnabled";
    static final String THREAD_ALLOCATED_MEMORY_SUPPORTED_ATTRIBUTE = "ThreadAllocatedMemorySupported";

    static final String GET_THREAD_CPU_TIME = "getThreadCpuTime";
    static final String GET_THREAD_ALLOCATED_BYTES = "getThreadAllocatedBytes";
    static final String GET_THREAD_USER_TIME = "getThreadUserTime";

    ExtendedThreadMBean() throws OperationFailedException {
        super(ManagementFactory.THREAD_MXBEAN_NAME);
    }

    boolean isThreadAllocatedMemoryEnabled() throws OperationFailedException {
        return (boolean) getAttribute(THREAD_ALLOCATED_MEMORY_ENABLED_ATTRIBUTE);

    }

    boolean isThreadAllocatedMemorySupported() throws OperationFailedException {
        return (boolean) getAttribute(THREAD_ALLOCATED_MEMORY_SUPPORTED_ATTRIBUTE);

    }

    void setThreadAllocatedMemoryEnabled(boolean value) throws OperationFailedException {
        setAttribute(THREAD_ALLOCATED_MEMORY_ENABLED_ATTRIBUTE, value);
    }

    long[] getThreadCpuTime(long[] ids) throws OperationFailedException {
        return (long[]) invokeOperation(GET_THREAD_CPU_TIME,
                new Object[]{ids}, new String[]{long[].class.getName()});
    }

    long getThreadAllocatedBytes(long id) throws OperationFailedException {
        return (long) invokeOperation(GET_THREAD_ALLOCATED_BYTES,
                new Object[]{id}, new String[]{long.class.getName()});
    }

    long[] getThreadAllocatedBytes(long[] id) throws OperationFailedException {
        return (long[]) invokeOperation(GET_THREAD_ALLOCATED_BYTES,
                new Object[]{id}, new String[]{long[].class.getName()});
    }

    long[] getThreadUserTime(long[] ids) throws OperationFailedException {
        return (long[]) invokeOperation(GET_THREAD_USER_TIME,
                new Object[]{ids}, new String[]{long[].class.getName()});
    }
}
