/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.io;

import org.xnio.XnioWorker;

/**
 *
 * @author jdenise
 */
public class XnioWorkerSupplier {

    XnioWorker worker;
    private final XnioWorker.Builder builder;

    public XnioWorkerSupplier(XnioWorker.Builder builder) {
        this.builder = builder;
    }

    public void init() {
        worker = builder.build();
    }
    public void cleanup() {
        worker = null;
    }

    public XnioWorker get() {
        return worker;
    }

}
