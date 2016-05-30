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
package org.jboss.as.cli.security;

import org.jboss.as.protocol.GeneralTimeoutHandler;
import org.jboss.as.protocol.StreamUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.security.CliSSLContext.ServerCertificateConsumer;
import org.jboss.as.cli.util.FingerprintGenerator;
import org.jboss.as.cli.impl.Console;

/**
 * A trust manager that by default delegates to a lazily initialised
 * TrustManager, this TrustManager also support both temporarily and permanently
 * accepting unknown server certificate chains.
 *
 * This class also acts as an aggregation of the configuration related to
 * TrustStore handling.
 *
 * It is not intended that Certificate management requests occur if this class
 * is registered to a SSLContext with multiple concurrent clients.
 */
public class LazyDelegatingTrustManager implements X509TrustManager {

    // Configuration based state set on initialisation.
    private final String trustStore;
    private final String trustStorePassword;
    private final boolean modifyTrustStore;

    private final Set<X509Certificate> temporarilyTrusted = new HashSet<>();
    private X509TrustManager delegate;
    private final GeneralTimeoutHandler timeoutHandler;
    private final ServerCertificateConsumer consumer;
    private final Console console;

    LazyDelegatingTrustManager(Console console,
            CliSSLContext.ServerCertificateConsumer consumer,
            GeneralTimeoutHandler timeoutHandler,
            String trustStore, String trustStorePassword, boolean modifyTrustStore) {
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.modifyTrustStore = modifyTrustStore;
        this.timeoutHandler = timeoutHandler;
        this.consumer = consumer;
        this.console = console;
    }

    /*
         * Methods to allow client interaction for certificate verification.
     */
    boolean isModifyTrustStore() {
        return modifyTrustStore;
    }

    synchronized void storeChainTemporarily(final Certificate[] chain) {
        for (Certificate current : chain) {
            if (current instanceof X509Certificate) {
                temporarilyTrusted.add((X509Certificate) current);
            }
        }
        delegate = null; // Triggers a reload on next use.
    }

    synchronized void storeChainPermenantly(final Certificate[] chain) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            KeyStore theTrustStore = KeyStore.getInstance("JKS");
            File trustStoreFile = new File(trustStore);
            if (trustStoreFile.exists()) {
                fis = new FileInputStream(trustStoreFile);
                theTrustStore.load(fis, trustStorePassword.toCharArray());
                StreamUtils.safeClose(fis);
                fis = null;
            } else {
                theTrustStore.load(null);
            }
            for (Certificate current : chain) {
                if (current instanceof X509Certificate) {
                    X509Certificate x509Current = (X509Certificate) current;
                    theTrustStore.setCertificateEntry(x509Current.getSubjectX500Principal().getName(), x509Current);
                }
            }

            fos = new FileOutputStream(trustStoreFile);
            theTrustStore.store(fos, trustStorePassword.toCharArray());

        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to operate on trust store.", e);
        } finally {
            StreamUtils.safeClose(fis);
            StreamUtils.safeClose(fos);
        }

        delegate = null; // Triggers a reload on next use.
    }

    /*
         * Internal Methods
     */
    private synchronized X509TrustManager getDelegate() {
        if (delegate == null) {
            FileInputStream fis = null;
            try {
                KeyStore theTrustStore = KeyStore.getInstance("JKS");
                File trustStoreFile = new File(trustStore);
                if (trustStoreFile.exists()) {
                    fis = new FileInputStream(trustStoreFile);
                    theTrustStore.load(fis, trustStorePassword.toCharArray());
                } else {
                    theTrustStore.load(null);
                }
                for (X509Certificate current : temporarilyTrusted) {
                    theTrustStore.setCertificateEntry(current.getSubjectX500Principal().getName(), current);
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(theTrustStore);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                for (TrustManager current : trustManagers) {
                    if (current instanceof X509TrustManager) {
                        delegate = (X509TrustManager) current;
                        break;
                    }
                }
            } catch (GeneralSecurityException | IOException e) {
                throw new IllegalStateException("Unable to operate on trust store.", e);
            } finally {
                StreamUtils.safeClose(fis);
            }
        }
        if (delegate == null) {
            throw new IllegalStateException("Unable to create delegate trust manager.");
        }

        return delegate;
    }

    /*
         * X509TrustManager Methods
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // The CLI is only verifying servers.
        getDelegate().checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, String authType) throws CertificateException {
        boolean retry;
        do {
            retry = false;
            try {
                getDelegate().checkServerTrusted(chain, authType);
                consumer.setServerCertificates(chain);
            } catch (CertificateException ce) {
                if (retry == false) {
                    timeoutHandler.suspendAndExecute(() -> {
                        try {
                            handleSSLFailure(chain);
                        } catch (InterruptedException ex) {
                            // Never swallow it
                            Thread.currentThread().interrupt();
                        } catch (IOException | CommandLineException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    if (delegate == null) {
                        retry = true;
                    } else {
                        throw ce;
                    }
                } else {
                    throw ce;
                }
            }
        } while (retry);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return getDelegate().getAcceptedIssuers();
    }

    /**
     * Handle the last SSL failure, prompting the user to accept or reject the
     * certificate of the remote server.
     *
     * @return true if the certificate validation should be retried.
     */
    private void handleSSLFailure(Certificate[] lastChain) throws CommandLineException, IOException, InterruptedException {
        console.println("Unable to connect due to unrecognised server certificate");
        for (Certificate current : lastChain) {
            if (current instanceof X509Certificate) {
                X509Certificate x509Current = (X509Certificate) current;
                Map<String, String> fingerprints = FingerprintGenerator.generateFingerprints(x509Current);
                console.println("Subject    - " + x509Current.getSubjectX500Principal().getName());
                console.println("Issuer     - " + x509Current.getIssuerDN().getName());
                console.println("Valid From - " + x509Current.getNotBefore());
                console.println("Valid To   - " + x509Current.getNotAfter());
                for (String alg : fingerprints.keySet()) {
                    console.println(alg + " : " + fingerprints.get(alg));
                }
                console.println("");
            }
        }

        for (;;) {
            String response;
            if (isModifyTrustStore()) {
                response = console.promptForInput("Accept certificate? [N]o, [T]emporarily, [P]ermanently : ");
            } else {
                response = console.promptForInput("Accept certificate? [N]o, [T]emporarily : ");
            }
            if (response == null) {
                break;
            } else if (response.length() == 1) {
                switch (response.toLowerCase(Locale.ENGLISH).charAt(0)) {
                    case 'n':
                        return;
                    case 't':
                        storeChainTemporarily(lastChain);
                        return;
                    case 'p':
                        if (isModifyTrustStore()) {
                            storeChainPermenantly(lastChain);
                            return;
                        }
                }
            }
        }
    }
}
