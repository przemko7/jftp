/*
 * Copyright 2012 jMethods, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.myjavaworld.jftp.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.myjavaworld.jftp.JFTP;

/**
 * @author Sai Pullabhotla, psai [at] jMethods [dot] com
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JFTPTrustManager implements X509TrustManager {

	private JFTP jftp = null;
	private String hostName = null;
	private X509TrustManager tm = null;
	private X509Certificate[] chain = null;

	/**
	 * Creates a new instance of <code>JFTPTrustManager</code>.
	 * 
	 * @param jftp
	 *            JFTP instance
	 * @param hostName
	 *            Host name against which the host name verification is to be
	 *            done.
	 * 
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * 
	 */
	public JFTPTrustManager(JFTP jftp, String hostName)
			throws KeyStoreException, NoSuchAlgorithmException {
		super();
		this.jftp = jftp;
		this.hostName = hostName;
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(KeyStoreManager.getServerCertificateStore());
		tm = (X509TrustManager) tmf.getTrustManagers()[0];
	}

	public X509Certificate[] getAcceptedIssuers() {
		return tm.getAcceptedIssuers();
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		tm.checkClientTrusted(chain, authType);
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		if (chain == null || chain.length == 0) {
			throw new CertificateException("The server returned an empty certificate chain");
		}

		if (this.chain != null) {
			if (Arrays.equals(chain, this.chain)) {
				return;
			}
		}

		boolean validDate = isValidDate(chain);
		boolean validHost = isValidHost(chain);
		boolean trusted = isTrusted(chain);
		try {
			tm.checkServerTrusted(chain, authType);
			trusted = true;
		} catch (CertificateException exp) {
			// The certificate manager may allow an explicit user decision below.
		}

		if (!validDate || !validHost || !trusted) {
			int userOption = SecurityWarningDlg.showDialog(jftp, chain,
					validDate, validHost, trusted);
			if (userOption != SecurityWarningDlg.YES_OPTION) {
				throw new CertificateException("No trusted certificate found. ");
			}
			this.chain = chain;
		} else {
			this.chain = chain;
		}
	}

	/**
	 * Checks to see if the certificate date is valid. This method only checks
	 * the first certificate in the chain.
	 * 
	 * @param chain
	 * @return <code>true</code> if the date on the certificate is valid.
	 *         <code>false</code>, otherwise.
	 * 
	 */
	private boolean isValidDate(X509Certificate[] chain) {
		try {
			chain[0].checkValidity();
		} catch (CertificateNotYetValidException exp) {
			return false;
		} catch (CertificateException exp) {
			return false;
		}
		return true;
	}

	/**
	 * Checks the host name on the certificate with the currently connecting
	 * host name.
	 * 
	 * @param chain
	 * @return <code>true</code>, if the host name verification succeeds.
	 *         <code>false</code>, otherwise.
	 * 
	 */
	private boolean isValidHost(X509Certificate[] chain) {
		X509Certificate certificate = chain[0];
		try {
			Collection<List<?>> names = certificate.getSubjectAlternativeNames();
			if (names != null && !names.isEmpty()) {
				for (List<?> name : names) {
					if (name.size() >= 2 && (Integer.valueOf(2).equals(name.get(0))
							|| Integer.valueOf(7).equals(name.get(0)))) {
						if (matchesHost(String.valueOf(name.get(1)), hostName)) {
							return true;
						}
					}
				}
				return false;
			}
		} catch (CertificateException exp) {
			return false;
		}
		String subjectDN = certificate.getSubjectDN().getName();
		DNParser parser = new DNParser(subjectDN);
		String commonName = parser.getParameter("CN");
		if (commonName != null) {
			return matchesHost(commonName, hostName);
		}
		return false;
	}

	private boolean matchesHost(String pattern, String host) {
		if (pattern == null || host == null) {
			return false;
		}
		String expected = host.toLowerCase(Locale.ROOT);
		String candidate = pattern.toLowerCase(Locale.ROOT);
		if (candidate.startsWith("*.") && expected.indexOf('.') > 0) {
			return expected.substring(expected.indexOf('.') + 1)
					.equals(candidate.substring(2));
		}
		return candidate.equals(expected);
	}

	/**
	 * Checks if the given certificate chain can be trusted.
	 * 
	 * @param chain
	 * @return <code>true</code>, if the certificate chain is trusted.
	 *         <code>false</code>, otherwise.
	 * 
	 */
	private boolean isTrusted(X509Certificate[] chain) {
		boolean trusted = false;
		try {
			KeyStore keyStore = KeyStoreManager.getServerCertificateStore();
			for (int i = chain.length - 1; i >= 0; i--) {
				if (keyStore.getCertificateAlias(chain[i]) != null) {
					trusted = true;
					break;
				}
			}
		} catch (Exception e) {
			trusted = false;
		}
		return trusted;
	}
}
