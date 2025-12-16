/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.server.deployment.reflect;

import org.jboss.as.server.logging.ServerLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;

/**
 * The processor to install the reflection index.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InstallReflectionIndexProcessor implements DeploymentUnitProcessor {
    private static DeploymentReflectionIndex FROM_BUILD;
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw ServerLogger.ROOT_LOGGER.nullModuleAttachment(deploymentUnit);
        }

        if(deploymentUnit.getParent() == null) {
            DeploymentReflectionIndex index;
           if(Boolean.getBoolean("org.wildfly.graal")) {
               index = FROM_BUILD;
           } else {
                index = DeploymentReflectionIndex.create();
                if (Boolean.getBoolean("org.wildfly.graal.build.time")) {
                    FROM_BUILD = index;
                }
           }
            deploymentUnit.putAttachment(Attachments.REFLECTION_INDEX, index);
            deploymentUnit.putAttachment(Attachments.PROXY_REFLECTION_INDEX, new ProxyMetadataSource(index));
        } else {
            final DeploymentReflectionIndex index = deploymentUnit.getParent().getAttachment(Attachments.REFLECTION_INDEX);
            deploymentUnit.putAttachment(Attachments.REFLECTION_INDEX, index);
            deploymentUnit.putAttachment(Attachments.PROXY_REFLECTION_INDEX, deploymentUnit.getParent().getAttachment(Attachments.PROXY_REFLECTION_INDEX));
        }
    }

    public void undeploy(final DeploymentUnit context) {
        context.removeAttachment(Attachments.REFLECTION_INDEX);
    }
}
