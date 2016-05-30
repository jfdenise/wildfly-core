package org.jboss.as.cli.security;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.SSLConfig;
import org.jboss.as.protocol.GeneralTimeoutHandler;
import org.jboss.as.protocol.StreamUtils;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.jboss.as.cli.impl.Console;

public class CliSSLContext {

    public interface ServerCertificateConsumer {

        void setServerCertificates(X509Certificate[] chain);
    }

    private SSLContext sslContext;
    private LazyDelegatingTrustManager trustManager;

    public CliSSLContext(SSLConfig sslConfig,
            GeneralTimeoutHandler timeoutHandler, ServerCertificateConsumer consumer,
            Console console) throws CliInitializationException {
        initSSLContext(sslConfig, timeoutHandler, consumer, console);
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public LazyDelegatingTrustManager getTrustManager() {
        return trustManager;
    }

    /**
     * Initialise the SSLContext and associated TrustManager for this
     * CommandContext.
     *
     * If no configuration is specified the default mode of operation will be to
     * use a lazily initialised TrustManager with no KeyManager.
     */
    private void initSSLContext(SSLConfig sslConfig,
            GeneralTimeoutHandler timeoutHandler, ServerCertificateConsumer consumer,
            Console console) throws CliInitializationException {
        // If the standard properties have been set don't enable and CLI specific stores.
        if (WildFlySecurityManager.getPropertyPrivileged("javax.net.ssl.keyStore", null) != null
                || WildFlySecurityManager.getPropertyPrivileged("javax.net.ssl.trustStore", null) != null) {
            return;
        }

        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers;

        String trustStore = null;
        String trustStorePassword = null;
        boolean modifyTrustStore = true;

        if (sslConfig != null) {
            String keyStoreLoc = sslConfig.getKeyStore();
            if (keyStoreLoc != null) {
                char[] keyStorePassword = sslConfig.getKeyStorePassword().toCharArray();
                String tmpKeyPassword = sslConfig.getKeyPassword();
                char[] keyPassword = tmpKeyPassword != null ? tmpKeyPassword.toCharArray() : keyStorePassword;

                File keyStoreFile = new File(keyStoreLoc);

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(keyStoreFile);
                    KeyStore theKeyStore = KeyStore.getInstance("JKS");
                    theKeyStore.load(fis, keyStorePassword);

                    String alias = sslConfig.getAlias();
                    if (alias != null) {
                        KeyStore replacement = KeyStore.getInstance("JKS");
                        replacement.load(null);
                        KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(keyPassword);

                        replacement.setEntry(alias, theKeyStore.getEntry(alias, protection), protection);
                        theKeyStore = replacement;
                    }

                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(theKeyStore, keyPassword);
                    keyManagers = keyManagerFactory.getKeyManagers();
                } catch (IOException | GeneralSecurityException e) {
                    throw new CliInitializationException(e);
                } finally {
                    StreamUtils.safeClose(fis);
                }

            }

            trustStore = sslConfig.getTrustStore();
            trustStorePassword = sslConfig.getTrustStorePassword();
            modifyTrustStore = sslConfig.isModifyTrustStore();
        }

        if (trustStore == null) {
            final String userHome = WildFlySecurityManager.getPropertyPrivileged("user.home", null);
            File trustStoreFile = new File(userHome, ".jboss-cli.truststore");
            trustStore = trustStoreFile.getAbsolutePath();
            trustStorePassword = "cli_truststore"; // Risk of modification but no private keys to be stored in the truststore.
        }

        trustManager = new LazyDelegatingTrustManager(console, consumer,
                timeoutHandler, trustStore, trustStorePassword, modifyTrustStore);
        trustManagers = new TrustManager[]{trustManager};

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagers, trustManagers, null);

            sslContext = ctx;
        } catch (GeneralSecurityException e) {
            throw new CliInitializationException(e);
        }
    }
}
