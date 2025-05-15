/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.platform.mbean;

import java.lang.management.ManagementFactory;
import org.jboss.as.controller.OperationFailedException;

/**
 * Implements the access to the com.sun.management.OperatingSystemMXBean
 *
 * @author jdenise
 */
class ExtendedOperatingSystemMBean extends AbstractExtendedMBean {

    static final String COMMITTED_VIRTUAL_MEMORY_SIZE_ATTRIBUTE = "CommittedVirtualMemorySize";
    static final String FREE_PHYSICAL_MEMORY_SIZE_ATTRIBUTE = "FreePhysicalMemorySize";
    static final String FREE_SWAP_SPACE_SIZE_ATTRIBUTE = "FreeSwapSpaceSize";
    static final String PROCESS_CPU_LOAD_ATTRIBUTE = "ProcessCpuLoad";
    static final String PROCESS_CPU_TIME_ATTRIBUTE = "ProcessCpuTime";
    static final String SYSTEM_CPU_LOAD_ATTRIBUTE = "SystemCpuLoad";
    static final String TOTAL_PHYSICAL_MEMORY_SIZE = "TotalPhysicalMemorySize";
    static final String TOTAL_SWAP_SPACE_SIZE = "TotalSwapSpaceSize";

    // Unix specific
    static final String MAX_FILE_DESCRIPTOR_COUNT_ATTRIBUTE = "MaxFileDescriptorCount";
    static final String OPEN_FILE_DESCRIPTOR_COUNT = "OpenFileDescriptorCount";

    ExtendedOperatingSystemMBean() throws OperationFailedException {
        super(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
    }

    long getCommittedVirtualMemorySize() throws OperationFailedException {
        return (long) getAttribute(COMMITTED_VIRTUAL_MEMORY_SIZE_ATTRIBUTE);

    }

    long getFreePhysicalMemorySize() throws OperationFailedException {
        return (long) getAttribute(FREE_PHYSICAL_MEMORY_SIZE_ATTRIBUTE);
    }

    long getFreeSwapSpaceSize() throws OperationFailedException {
        return (long) getAttribute(FREE_SWAP_SPACE_SIZE_ATTRIBUTE);
    }

    double getProcessCpuLoad() throws OperationFailedException {
        return (double) getAttribute(PROCESS_CPU_LOAD_ATTRIBUTE);
    }

    double getSystemCpuLoad() throws OperationFailedException {
        return (double) getAttribute(SYSTEM_CPU_LOAD_ATTRIBUTE);
    }

    long getProcessCpuTime() throws OperationFailedException {
        return (long) getAttribute(PROCESS_CPU_TIME_ATTRIBUTE);
    }

    long getTotalPhysicalMemorySize() throws OperationFailedException {
        return (long) getAttribute(TOTAL_PHYSICAL_MEMORY_SIZE);
    }

    long getTotalSwapSpaceSize() throws OperationFailedException {
        return (long) getAttribute(TOTAL_SWAP_SPACE_SIZE);
    }

    long getMaxFileDescriptorCount() throws OperationFailedException {
        return (long) getAttribute(MAX_FILE_DESCRIPTOR_COUNT_ATTRIBUTE);
    }

    long getOpenFileDescriptorCount() throws OperationFailedException {
        return (long) getAttribute(OPEN_FILE_DESCRIPTOR_COUNT);
    }
}
