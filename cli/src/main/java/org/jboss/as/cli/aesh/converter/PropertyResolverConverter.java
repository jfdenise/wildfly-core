package org.jboss.as.cli.aesh.converter;

import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.as.cli.impl.ArgumentWithValue;

/**
 * Resolve properties that could be embedded in value.
 *
 * @author jdenise@redhat.com
 */
public class PropertyResolverConverter implements Converter<String, CliConverterInvocation> {

    @Override
    public String convert(CliConverterInvocation converterInvocation) throws OptionValidatorException {
        try {
            return ArgumentWithValue.resolveValue(converterInvocation.getInput());
        } catch (CommandFormatException ex) {
            throw new OptionValidatorException(Util.getMessagesFromThrowable(ex));
        }
    }

}
