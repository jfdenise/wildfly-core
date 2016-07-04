/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.ConnectionInfo;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.util.FingerprintGenerator;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 * A command to print connection-info.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "connection-info", description = "")
public class ConnectionInfoCommand implements Command<CliCommandInvocation> {

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        printConnectionInfo(commandInvocation);
        return null;
    }

    private void printConnectionInfo(CliCommandInvocation commandInvocation) {
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        final ModelControllerClient client = ctx.getModelControllerClient();
        if (client == null) {
            commandInvocation.getShell().out().println("<connect to the controller and re-run the connection-info command to see the connection information>\n");
        } else {

            ConnectionInfo connInfo = ctx.getConnectionInfo();
            String username = null;

            final ModelNode req = new ModelNode();
            req.get(Util.OPERATION).set("whoami");
            req.get(Util.ADDRESS).setEmptyList();
            req.get("verbose").set(true);

            try {
                final ModelNode response = client.execute(req);
                if (Util.isSuccess(response)) {
                    if (response.hasDefined(Util.RESULT)) {
                        final ModelNode result = response.get(Util.RESULT);
                        if (result.hasDefined("identity")) {
                            username = result.get("identity").get("username").asString();
                        }
                        if (result.hasDefined("mapped-roles")) {
                            String strRoles = result.get("mapped-roles").asString();
                            String grantedStr = "granted role";
                            // a comma is contained in the string if there is more than one role
                            if (strRoles.indexOf(',') > 0) {
                                grantedStr = "granted roles";
                            }
                            username = username + ", " + grantedStr + " " + strRoles;
                        } else {
                            username = username + " has no role associated.";
                        }
                    } else {
                        username = "result was not available.";
                    }
                } else {
                    commandInvocation.getShell().out().println(Util.getFailureDescription(response));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to get the AS release info: " + e.getLocalizedMessage());
            }

            SimpleTable st = new SimpleTable(2);
            st.addLine(new String[]{"Username", username});
            st.addLine(new String[]{"Logged since", connInfo.getLoggedSince().toString()});
            X509Certificate[] lastChain = connInfo.getServerCertificates();
            boolean sslConn = lastChain != null;
            if (sslConn) {
                try {
                    for (Certificate current : lastChain) {
                        if (current instanceof X509Certificate) {
                            X509Certificate x509Current = (X509Certificate) current;
                            Map<String, String> fingerprints = FingerprintGenerator.generateFingerprints(x509Current);
                            st.addLine(new String[]{"Subject", x509Current.getSubjectX500Principal().getName()});
                            st.addLine(new String[]{"Issuer", x509Current.getIssuerDN().getName()});
                            st.addLine(new String[]{"Valid from", x509Current.getNotBefore().toString()});
                            st.addLine(new String[]{"Valid to", x509Current.getNotAfter().toString()});
                            for (String alg : fingerprints.keySet()) {
                                st.addLine(new String[]{alg, fingerprints.get(alg)});
                            }
                        }
                    }
                } catch (CommandLineException cle) {
                    throw new RuntimeException("Error trying to generate server certificate fingerprint.", cle);
                }
            } else {
                st.addLine(new String[]{"Not an SSL connection.", ""});
            }
            commandInvocation.getShell().out().println(st.toString());
        }
    }
}
