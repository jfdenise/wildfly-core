/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.shared;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;
import org.junit.Assume;

/**
 *
 * @author jdenise
 */
public class AssumeUtils {

    public static void assumeBootableJarProfileEnabled() {
        assumeCondition("Tests failing in bootablejar profile are disabled", () -> System.getProperty("ts.bootable") == null);
    }

    private static void assumeCondition(final String message, final Supplier<Boolean> assumeTrueCondition) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Assume.assumeTrue(message, assumeTrueCondition.get());
                return null;
            }
        });
    }
}
