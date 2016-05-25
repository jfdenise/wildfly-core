/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.aesh.completer;

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class PathOptionCompleter implements OptionCompleter<CliCompleterInvocation> {

    @Override
    public void complete(CliCompleterInvocation cliCompleterInvocation) {
        List<String> candidates = new ArrayList<>();
        int pos = 0;
        if(cliCompleterInvocation.getGivenCompleteValue() != null)
            pos = cliCompleterInvocation.getGivenCompleteValue().length();

        int cursor = OperationRequestCompleter.ARG_VALUE_COMPLETER.complete(cliCompleterInvocation.getCommandContext(),
                cliCompleterInvocation.getGivenCompleteValue(), pos, candidates);
        cliCompleterInvocation.addAllCompleterValues(candidates);
        cliCompleterInvocation.setOffset(cliCompleterInvocation.getGivenCompleteValue().length() - cursor);
        cliCompleterInvocation.setAppendSpace(false);
    }
}
