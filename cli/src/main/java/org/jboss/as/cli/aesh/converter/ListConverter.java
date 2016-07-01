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
package org.jboss.as.cli.aesh.converter;

import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ListConverter implements Converter<ModelNode, CliConverterInvocation> {

    public static ListConverter INSTANCE = new ListConverter();

    private ListConverter() {
    }

    @Override
    public ModelNode convert(CliConverterInvocation converterInvocation) throws OptionValidatorException {
        String value = converterInvocation.getInput();
        try {
            return ArgumentValueConverter.LIST.fromString(converterInvocation.
                    getCommandContext().getLegacyCommandContext(), value);
        } catch (CommandFormatException ex) {
            throw new OptionValidatorException(ex.toString());
        }
    }
}
