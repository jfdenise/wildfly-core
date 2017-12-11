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

import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.aesh.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_TRUST_STORE_NAME;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public abstract class SSLSecurityBuilder {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private String sslContextName;
    private String keyManagerName;
    private final ModelNode composite = new ModelNode();
    private ServerSSLContext sslContext;
    private File trustedCertificate;
    private String trustStoreName;
    private String trustStoreFileName;
    private String generatedTrustStore;
    private String trustStoreFilePassword;
    private String newTrustStoreName;
    private String newTrustManagerName;

    private boolean validateCertificate;

    public SSLSecurityBuilder() throws CommandException {
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
    }

    public void setNewTrustStoreName(String newTrustStoreName) {
        this.newTrustStoreName = newTrustStoreName;
    }

    public void setNewTrustManagerName(String newTrustManagerName) {
        this.newTrustManagerName = newTrustManagerName;
    }

    public ModelNode getRequest() {
        return composite;
    }

    public File getTrustedCertificatePath() {
        return trustedCertificate;
    }

    public void setTrustedCertificatePath(File trustedCertificate) {
        this.trustedCertificate = trustedCertificate;
    }

    public void setValidateCertificate(boolean validateCertificate) {
        this.validateCertificate = validateCertificate;
    }

    public ModelNode getSteps() {
        return composite.get(Util.STEPS);
    }

    public ServerSSLContext getServerSSLContext() {
        return sslContext;
    }

    public SSLSecurityBuilder setSSLContextName(String sslContextName) {
        this.sslContextName = sslContextName;
        return this;
    }

    public SSLSecurityBuilder setKeyManagerName(String keyManagerName) {
        this.keyManagerName = keyManagerName;
        return this;
    }

    protected abstract KeyStore buildKeyStore(CommandContext ctx, ModelNode step, boolean workaroundComposite) throws Exception;

    public void buildRequest(CommandContext ctx, boolean buildRequest) throws Exception {
        KeyStore keyStore;
        KeyManager km;
        try {
            // First build the keyStore.
            final ModelNode steps = getSteps();
            keyStore = buildKeyStore(ctx, steps, buildRequest);

            // The trust manager
            KeyManager trustManager = buildTrustManager(ctx, buildRequest);

            // Then the key manager
            km = buildKeyManager(ctx, keyManagerName, keyStore, steps);

            // Finally the SSLContext;
            sslContext = buildServerSSLContext(ctx, km, trustManager, steps);
        } catch (Exception ex) {
            exceptionOccured(ctx, ex);
            throw ex;
        }
    }

    protected KeyManager buildTrustManager(CommandContext ctx, boolean buildRequest) throws Exception {
        KeyManager trustManager = null;
        if (trustedCertificate != null || trustStoreName != null) {
            KeyStore trustStore = null;
            String id = UUID.randomUUID().toString();
            // create a new key-store for the trustore and import the certificate.
            if (newTrustStoreName == null) {
                newTrustStoreName = "trust-store-" + id;
            } else if (ElytronUtil.keyStoreExists(ctx, newTrustStoreName)) {
                throw new CommandException("The key-store " + newTrustStoreName + " already exists");
            }
            if (trustStoreName == null) {
                if (trustStoreFileName == null) {
                    trustStoreFileName = "server-" + id + ".trustore";
                } else {
                    List<String> ksNames = ElytronUtil.findMatchingKeyStores(ctx, new File(trustStoreFileName), Util.JBOSS_SERVER_CONFIG_DIR);
                    if (!ksNames.isEmpty()) {
                        throw new CommandException("Error, the file " + trustStoreFileName + " is already referenced from " + ksNames
                                + " resources. Use " + SecurityCommand.formatOption(OPT_TRUST_STORE_NAME) + " option or choose another file name.");
                    }
                }
                generatedTrustStore = newTrustStoreName;
                String password = trustStoreFilePassword == null ? generateRandomPassword() : trustStoreFilePassword;
                ModelNode request = ElytronUtil.addKeyStore(ctx, newTrustStoreName, new File(trustStoreFileName),
                        Util.JBOSS_SERVER_CONFIG_DIR, password, ElytronUtil.JKS, false, null);
                // For now that is a workaround because we can't add and call operation in same composite.
                if (buildRequest) { // echo-dmr
                    getSteps().add(request);
                } else {
                    SecurityCommand.execute(ctx, request);
                }
                trustStore = new KeyStore(newTrustStoreName, password, false);
                // import the certificate
                ModelNode certImport = ElytronUtil.importCertificate(ctx, trustedCertificate, id, validateCertificate, trustStore);
                getSteps().add(certImport);
                ModelNode store = ElytronUtil.storeKeyStore(ctx, trustStore.getName());
                getSteps().add(store);
            } else {
                trustStore = ElytronUtil.getKeyStore(ctx, trustStoreName);
            }
            //Create a trust-store key-manager
            trustManager = buildTrustManager(ctx, newTrustManagerName, trustStore, getSteps());
        }
        return trustManager;
    }

    private static KeyManager buildKeyManager(CommandContext ctx, String ksManagerName, KeyStore keyStore, ModelNode steps) throws Exception {
        boolean lookupExisting = false;
        if (ksManagerName == null) {
            ksManagerName = DefaultResourceNames.buildDefaultKeyManagerName(ctx, keyStore.getName());
            lookupExisting = true;
        } else {
            if (ElytronUtil.keyManagerExists(ctx, ksManagerName)) {
                throw new CommandException("The key-manager " + ksManagerName + " already exists");
            }
        }
        String name = null;
        boolean exists = false;
        // Lookup for a matching key manager only if the keystore already exists
        // the keyManager doesn't exist and no name has been provided
        if (keyStore.exists() && lookupExisting) {
            name = ElytronUtil.findMatchingKeyManager(ctx, keyStore, null, null);
        }
        if (name == null) {
            name = ksManagerName;
            steps.add(ElytronUtil.addKeyManager(ctx, keyStore, ksManagerName, null, null));
        } else {
            exists = true;
        }
        return new KeyManager(name, keyStore, exists);
    }

    private static KeyManager buildTrustManager(CommandContext ctx, String ksManagerName, KeyStore keyStore, ModelNode steps) throws Exception {
        boolean lookupExisting = false;
        if (ksManagerName == null) {
            ksManagerName = DefaultResourceNames.buildDefaultKeyManagerName(ctx, keyStore.getName());
            lookupExisting = true;
        } else if (ElytronUtil.trustManagerExists(ctx, ksManagerName)) {
            throw new CommandException("The key-manager " + ksManagerName + " already exists");
        }
        String name = null;
        boolean exists = false;
        // Lookup for a matching key manager only if the keystore already exists.
        if (keyStore.exists() && lookupExisting) {
            name = ElytronUtil.findMatchingTrustManager(ctx, keyStore, null, null);
        }
        if (name == null) {
            name = ksManagerName;
            steps.add(ElytronUtil.addTrustManager(ctx, keyStore, ksManagerName, null, null));
        } else {
            exists = true;
        }
        return new KeyManager(name, keyStore, exists);
    }

    private ServerSSLContext buildServerSSLContext(CommandContext ctx, KeyManager manager, KeyManager trustManager, ModelNode steps) throws Exception {
        boolean lookupExisting = false;
        if (sslContextName == null) {
            sslContextName = DefaultResourceNames.buildDefaultSSLContextName(ctx, manager.getKeyStore().getName());
            lookupExisting = true;
        } else {
            if (ElytronUtil.serverSSLContextExists(ctx, sslContextName)) {
                throw new CommandException("The ssl-context " + sslContextName + " already exists");
            }
        }
        List<String> lst = DefaultResourceNames.getDefaultProtocols(ctx);
        String name = null;
        boolean exists = false;

        boolean want = trustManager != null;
        boolean need = trustManager != null;
        String tm = trustManager == null ? null : trustManager.getName();
        // Lookup for a matching sslContext only if the keymanager already exists
        // the sslContext doesn't exist and no name has been provided
        if (manager.exists() && lookupExisting) {
            name = ElytronUtil.findMatchingSSLContext(ctx, manager, want, need,
                    tm, lst);
        }

        if (name == null) {
            steps.add(ElytronUtil.addServerSSLContext(ctx, manager, want, need, tm, lst, sslContextName));
            name = sslContextName;
        } else {
            exists = true;
        }
        return new ServerSSLContext(name, manager, trustManager, exists);
    }

    public void exceptionOccured(CommandContext ctx, Exception ex) {
        if (generatedTrustStore != null) {
            try {
                ModelNode req = ElytronUtil.removeKeyStore(ctx, generatedTrustStore);
                SecurityCommand.execute(ctx, req);
            } catch (Exception ex2) {
                ex.addSuppressed(ex2);
            }
        }
        doExceptionOccured(ctx, ex);
    }

    protected abstract void doExceptionOccured(CommandContext ctx, Exception ex);

    /**
     * @return the trustStoreName
     */
    public String getTrustStoreName() {
        return trustStoreName;
    }

    /**
     * @param trustStoreName the trustStoreName to set
     */
    public void setTrustStoreName(String trustStoreName) {
        this.trustStoreName = trustStoreName;
    }

    /**
     * @return the trustStoreFileName
     */
    public String getTrustStoreFileName() {
        return trustStoreFileName;
    }

    /**
     * @param trustStoreFileName the trustStoreFileName to set
     */
    public void setTrustStoreFileName(String trustStoreFileName) {
        this.trustStoreFileName = trustStoreFileName;
    }

    public void setTrustStoreFilePassword(String trustStoreFilePassword) {
        this.trustStoreFilePassword = trustStoreFilePassword;
    }

    static String generateRandomPassword() {
        return generateRandomString(8);
    }

    static String generateRandomString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (RANDOM.nextDouble() * CHARS.length());
            builder.append(CHARS.substring(index, index + 1));
        }
        return builder.toString();
    }

}
