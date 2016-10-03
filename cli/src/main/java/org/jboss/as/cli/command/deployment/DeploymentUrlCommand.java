/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli.command.deployment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.as.cli.command.deployment.DeploymentActivators.UrlActivator;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deploy-url", description = "")
public class DeploymentUrlCommand extends DeploymentContentSubCommand implements DMRCommand {

    public static class UrlConverter implements Converter<URL, CliConverterInvocation> {

        @Override
        public URL convert(CliConverterInvocation c) throws OptionValidatorException {
            try {
                return new URL(c.getInput());
            } catch (MalformedURLException e) {
                throw new OptionValidatorException(e.getLocalizedMessage());
            }
        }

    }

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',', activator = UrlActivator.class,
            converter = UrlConverter.class)
    protected List<URL> url;

    DeploymentUrlCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    protected void checkArgument() throws CommandException {
        if (url == null || url.isEmpty()) {
            throw new CommandException("No deployment url");
        }
    }

    @Override
    protected String getName() {
        URL deploymentUrl = url.get(0);
        String name = deploymentUrl.getPath();
        // strip trailing slash if present
        if (name.charAt(name.length() - 1) == '/') {
            name = name.substring(0, name.length() - 1);
        }
        // take only last element of the path
        if (name.lastIndexOf('/') > -1) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        return name;
    }

    @Override
    protected void addContent(ModelNode content) throws OperationFormatException {
        content.get(Util.URL).set(url.get(0).toExternalForm());
    }

    @Override
    protected String getCommandName() {
        return "deploy-url";
    }

    @Override
    protected ModelNode execute(CommandContext ctx, ModelNode request)
            throws IOException {
        return ctx.getModelControllerClient().execute(request);
    }
}
