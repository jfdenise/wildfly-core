/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

/**
 *
 * @author jdenise
 */
interface Constants {

    static final String DEPLOYMENT_ARG = "--deployment";
    static final String INSTALL_DIR_ARG = "--install-dir";
    static final String DISPLAY_GALLEON_CONFIG_ARG = "--display-galleon-config";
    static final String CLI_SCRIPT_ARG = "--cli-script";
    static final String CHECK_BOOT_ARG = "--check-boot";
    static final String DEPLOYMENTS = "deployments";
    static final String STANDALONE_CONFIG = "standalone.xml";

    static final String LOG_MANAGER_PROP = "java.util.logging.manager";
    static final String LOG_MANAGER_CLASS = "org.jboss.logmanager.LogManager";
    static final String LOG_BOOT_FILE_PROP = "org.jboss.boot.log.file";
    static final String LOGGING_PROPERTIES = "logging.properties";

    static final String DEPLOYMENT_FAILED = "FAILED";
    static final String FAILURE_DESCRIPTION = "failure-description";
    static final String ALL_DEPLOYMENTS_ADDRESS = "/deployment=*";
    static final String READ_BOOT_ERRORS_OPERATION = "read-boot-errors";
    static final String CORE_SERVICE_MANAGEMENT_ADDRESS = "/core-service=management";
    static final String NORMAL = "NORMAL";
    static final String RUNNING = "running";
    static final String RUNNING_MODE = "running-mode";
    static final String SERVER_LOG = "server.log";
    static final String SERVER_STATE = "server-state";
    static final String STATUS = "status";
    static final String STOPPED = "stopped";

    static final String SHA1 = "sha1";

    String DEBUG_PROPERTY = "org.wildfly.core.jar.debug";
}
