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
package org.jboss.as.cli.impl.ssh;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author jdenise@redhat.com
 */
public class SSHConfiguration {

    public static class Builder {

        private String userName;
        private String password;
        private Path keysPath;
        private Integer port;
        private String address;

        public Builder setAuthUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setAuthUserPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setAutorizedKeysPath(Path keysPath) {
            this.keysPath = keysPath;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public SSHConfiguration build() {
            if (userName != null || password != null) {
                if (userName == null || password == null) {
                    throw new RuntimeException("userName and password can't be null");
                }
            }
            if (keysPath != null) {
                if (!Files.exists(keysPath)) {
                    throw new RuntimeException(keysPath + " doesn't exist");
                }
            }
            return new SSHConfiguration(userName, password, keysPath, address, port);
        }
    }

    private final String userName;
    private final String password;
    private final Path keysPath;
    private final Integer port;
    private final String address;

    private SSHConfiguration(String userName, String password, Path keysPath, String address, Integer port) {
        this.userName = userName;
        this.password = password;
        this.keysPath = keysPath;
        this.port = port == null ? 5000 : port;
        this.address = address == null ? "localhost" : address;
    }

    public String getAuthUserName() {
        return userName;
    }

    public String getAuthPassword() {
        return password;
    }

    public Path getAuthorizedKeys() {
        return keysPath;
    }

    public int getPort() {
        return port;
    }

    public String getAdress() {
        return address;
    }
}
