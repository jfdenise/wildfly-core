/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.graal;

import java.util.List;
import java.util.Map;

/**
 *
 * @author jdenise
 */
public interface PreMainInitializer {
    public void init(Map<String, List<GraalRecorder.UnresolvedRecord>> records) throws Exception;
    public String getRecordingKey();
}
