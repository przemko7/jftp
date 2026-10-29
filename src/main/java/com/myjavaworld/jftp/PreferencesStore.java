/*
 * Copyright 2012 jMethods, Inc.
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.myjavaworld.jftp;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.myjavaworld.util.EncryptedPropertiesStore;

/** Versioned encrypted storage for application preferences. */
final class PreferencesStore {

	private PreferencesStore() {
	}

	static JFTPPreferences load(File dataHome) throws IOException {
		File file = new File(dataHome, "preferences.dat");
		if (!file.exists() || file.length() == 0) {
			return new JFTPPreferences();
		}
		Properties p = EncryptedPropertiesStore.load(file,
				EncryptedPropertiesStore.loadOrCreateKey(dataHome));
		JFTPPreferences prefs = new JFTPPreferences();
		prefs.setLocale(Locale.forLanguageTag(p.getProperty("locale",
				prefs.getLocale().toLanguageTag())));
		prefs.setLookAndFeelClassName(p.getProperty("laf",
				prefs.getLookAndFeelClassName()));
		prefs.setTheme(p.getProperty("theme", prefs.getTheme()));
		prefs.setLocalDirectory(p.getProperty("localDirectory",
				prefs.getLocalDirectory()));
		prefs.setEmail(p.getProperty("email", prefs.getEmail()));
		prefs.setClient(p.getProperty("client", prefs.getClient()));
		prefs.setListParser(p.getProperty("listParser", prefs.getListParser()));
		prefs.setTimeout(integer(p, "timeout", prefs.getTimeout()));
		prefs.setBufferSize(integer(p, "bufferSize", prefs.getBufferSize()));
		prefs.setDateFormat(integer(p, "dateFormat", prefs.getDateFormat()));
		prefs.setTimeFormat(integer(p, "timeFormat", prefs.getTimeFormat()));
		prefs.setDefaultTransferType(integer(p, "defaultTransferType",
				prefs.getDefaultTransferType()));
		prefs.setPassive(bool(p, "passive", prefs.isPassive()));
		prefs.setUseJavaWindows(bool(p, "useJavaWindows", prefs.getUseJavaWindows()));
		prefs.setToolBarType(integer(p, "toolBarType", prefs.getToolBarType()));
		prefs.setServerCertificateStore(p.getProperty("serverCertificateStore",
				prefs.getServerCertificateStore()));
		prefs.setClientCertificateStore(p.getProperty("clientCertificateStore",
				prefs.getClientCertificateStore()));
		prefs.setServerCertificateStorePassword(p.getProperty(
				"serverCertificateStorePassword",
				new String(prefs.getServerCertificateStorePassword())).toCharArray());
		prefs.setClientCertificateStorePassword(p.getProperty(
				"clientCertificateStorePassword",
				new String(prefs.getClientCertificateStorePassword())).toCharArray());
		prefs.setUseProxy(bool(p, "useProxy", prefs.isUseProxy()));
		prefs.setProxyHost(p.getProperty("proxyHost", prefs.getProxyHost()));
		prefs.setProxyPort(integer(p, "proxyPort", prefs.getProxyPort()));
		prefs.setProxyUser(p.getProperty("proxyUser", prefs.getProxyUser()));
		prefs.setProxyPassword(p.getProperty("proxyPassword",
				new String(prefs.getProxyPassword())).toCharArray());
		prefs.setSSLUsage(integer(p, "sslUsage", prefs.getSSLUsage()));
		prefs.setImplicitSSLPort(integer(p, "implicitSSLPort",
				prefs.getImplicitSSLPort()));
		prefs.setDataChannelUnencrypted(bool(p, "dataChannelUnencrypted",
				prefs.isDataChannelUnencrypted()));
		prefs.setLicenseAgreed(bool(p, "licenseAgreed", prefs.isLicenseAgreed()));
		prefs.setLicenseAgreedForVersion(p.getProperty("licenseAgreedForVersion",
				prefs.getLicenseAgreedForVersion()));
		prefs.setCheckForUpdates(bool(p, "checkForUpdates", prefs.getCheckForUpdates()));
		if (p.containsKey("window.x")) {
			prefs.setWindowBounds(new Rectangle(integer(p, "window.x", 0),
					integer(p, "window.y", 0), integer(p, "window.width", 0),
					integer(p, "window.height", 0)));
		}
		Map transferTypes = new TreeMap();
		int transferCount = integer(p, "transferType.count", 0);
		for (int i = 0; i < transferCount; i++) {
			String extension = p.getProperty("transferType." + i + ".extension");
			if (extension != null && extension.length() > 0) {
				transferTypes.put(extension, Integer.valueOf(integer(p,
						"transferType." + i + ".value", 0)));
			}
		}
		if (!transferTypes.isEmpty()) {
			prefs.setTransferTypes(transferTypes);
		}
		return prefs;
	}

	static void save(File dataHome, JFTPPreferences prefs) throws IOException {
		Properties p = new Properties();
		p.setProperty("format", "1");
		Locale locale = prefs.getLocale() == null ? Locale.getDefault()
				: prefs.getLocale();
		p.setProperty("locale", locale.toLanguageTag());
		p.setProperty("laf", value(prefs.getLookAndFeelClassName()));
		p.setProperty("theme", value(prefs.getTheme()));
		p.setProperty("localDirectory", value(prefs.getLocalDirectory()));
		p.setProperty("email", value(prefs.getEmail()));
		p.setProperty("client", value(prefs.getClient()));
		p.setProperty("listParser", value(prefs.getListParser()));
		p.setProperty("timeout", Integer.toString(prefs.getTimeout()));
		p.setProperty("bufferSize", Integer.toString(prefs.getBufferSize()));
		p.setProperty("dateFormat", Integer.toString(prefs.getDateFormat()));
		p.setProperty("timeFormat", Integer.toString(prefs.getTimeFormat()));
		p.setProperty("defaultTransferType", Integer.toString(prefs.getDefaultTransferType()));
		p.setProperty("passive", Boolean.toString(prefs.isPassive()));
		p.setProperty("useJavaWindows", Boolean.toString(prefs.getUseJavaWindows()));
		p.setProperty("toolBarType", Integer.toString(prefs.getToolBarType()));
		p.setProperty("serverCertificateStore", value(prefs.getServerCertificateStore()));
		p.setProperty("clientCertificateStore", value(prefs.getClientCertificateStore()));
		p.setProperty("serverCertificateStorePassword",
				new String(prefs.getServerCertificateStorePassword()));
		p.setProperty("clientCertificateStorePassword",
				new String(prefs.getClientCertificateStorePassword()));
		p.setProperty("useProxy", Boolean.toString(prefs.isUseProxy()));
		p.setProperty("proxyHost", value(prefs.getProxyHost()));
		p.setProperty("proxyPort", Integer.toString(prefs.getProxyPort()));
		p.setProperty("proxyUser", value(prefs.getProxyUser()));
		p.setProperty("proxyPassword", new String(prefs.getProxyPassword()));
		p.setProperty("sslUsage", Integer.toString(prefs.getSSLUsage()));
		p.setProperty("implicitSSLPort", Integer.toString(prefs.getImplicitSSLPort()));
		p.setProperty("dataChannelUnencrypted", Boolean.toString(prefs.isDataChannelUnencrypted()));
		p.setProperty("licenseAgreed", Boolean.toString(prefs.isLicenseAgreed()));
		p.setProperty("licenseAgreedForVersion", prefs.getLicenseAgreedForVersion());
		p.setProperty("checkForUpdates", Boolean.toString(prefs.getCheckForUpdates()));
		Rectangle bounds = prefs.getWindowBounds();
		if (bounds != null) {
			p.setProperty("window.x", Integer.toString(bounds.x));
			p.setProperty("window.y", Integer.toString(bounds.y));
			p.setProperty("window.width", Integer.toString(bounds.width));
			p.setProperty("window.height", Integer.toString(bounds.height));
		}
		Map transferTypes = prefs.getTransferTypes();
		int i = 0;
		if (transferTypes != null) {
			for (Object entryObject : transferTypes.entrySet()) {
				Map.Entry entry = (Map.Entry) entryObject;
				p.setProperty("transferType." + i + ".extension", value(String.valueOf(entry.getKey())));
				p.setProperty("transferType." + i + ".value", value(String.valueOf(entry.getValue())));
				i++;
			}
		}
		p.setProperty("transferType.count", Integer.toString(i));
		EncryptedPropertiesStore.save(new File(dataHome, "preferences.dat"), p,
				EncryptedPropertiesStore.loadOrCreateKey(dataHome));
	}

	private static String value(String value) {
		return value == null ? "" : value;
	}

	private static boolean bool(Properties p, String key, boolean fallback) {
		return p.containsKey(key) ? Boolean.parseBoolean(p.getProperty(key)) : fallback;
	}

	private static int integer(Properties p, String key, int fallback) {
		try {
			return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)));
		} catch (NumberFormatException exp) {
			return fallback;
		}
	}
}
