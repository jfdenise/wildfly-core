/*
Copyright 2017 Red Hat, Inc.

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
package org.jboss.as.cli.impl.aesh.cmd.security.model;

/**
 *
 * @author jdenise@redhat.com
 */
public class ServerSSLContext {

    private final String name;
    private final KeyManager keyManager;
    private final KeyManager trustManager;
    private final boolean exists;

    public ServerSSLContext(String name, KeyManager keyManager, KeyManager trustManager, boolean exists) {
        this.name = name;
        this.keyManager = keyManager;
        this.trustManager = trustManager;
        this.exists = exists;
    }

    public boolean exists() {
        return exists;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the password
     */
    public KeyManager getKeyManager() {
        return keyManager;
    }

    public KeyManager getTrustManager() {
        return trustManager;
    }
}
