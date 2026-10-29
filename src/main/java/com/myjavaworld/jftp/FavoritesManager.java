/*
 * Copyright 2012 jMethods, Inc.
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.myjavaworld.jftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;

import com.myjavaworld.util.EncryptedPropertiesStore;

/** Stores favorite FTP sites in a versioned, authenticated properties file. */
public class FavoritesManager {

	private static final File FAV_FILE = new File(JFTP.DATA_HOME,
			"favorites.dat");
	private static final File LEGACY_FAV_FILE = new File(JFTP.DATA_HOME,
			"favorites.ser");

	private FavoritesManager() {
	}

	public static List getFavorites() throws IOException,
			ClassNotFoundException, IllegalBlockSizeException,
			BadPaddingException {
		checkDataHome();
		if (FAV_FILE.exists() && FAV_FILE.length() > 0) {
			return readProperties();
		}
		if (LEGACY_FAV_FILE.exists() && LEGACY_FAV_FILE.length() > 0) {
			List favorites = readLegacyFavorites();
			saveFavorites(favorites);
			return favorites;
		}
		return new ArrayList();
	}

	public static void saveFavorites(List favorites) throws IOException,
			IllegalBlockSizeException {
		checkDataHome();
		Properties properties = new Properties();
		properties.setProperty("format", "1");
		int index = 0;
		if (favorites != null) {
			for (Object value : favorites) {
				if (value instanceof RemoteHost) {
					writeFavorite(properties, index++, (RemoteHost) value);
				}
			}
		}
		properties.setProperty("count", Integer.toString(index));
		EncryptedPropertiesStore.save(FAV_FILE, properties,
				EncryptedPropertiesStore.loadOrCreateKey(JFTP.DATA_HOME));
	}

	public static void addFavorite(RemoteHost host) throws IOException,
			ClassNotFoundException, IllegalBlockSizeException,
			BadPaddingException {
		List favorites = getFavorites();
		favorites.add(host);
		saveFavorites(favorites);
	}

	private static List readProperties() throws IOException {
		Properties properties = EncryptedPropertiesStore.load(FAV_FILE,
				EncryptedPropertiesStore.loadOrCreateKey(JFTP.DATA_HOME));
		int count;
		try {
			count = Integer.parseInt(properties.getProperty("count", "0"));
		} catch (NumberFormatException exp) {
			throw new IOException("Invalid favorites count", exp);
		}
		if (count < 0 || count > 10000) {
			throw new IOException("Invalid favorites count: " + count);
		}
		List favorites = new ArrayList(count);
		for (int i = 0; i < count; i++) {
			String prefix = "favorite." + i + ".";
			Favorite favorite = new Favorite();
			favorite.setName(properties.getProperty(prefix + "name", ""));
			favorite.setHostName(properties.getProperty(prefix + "host", ""));
			favorite.setPort(integer(properties, prefix + "port", 21));
			favorite.setUser(properties.getProperty(prefix + "user", ""));
			favorite.setPassword(properties.getProperty(prefix + "password", ""));
			favorite.setAccount(properties.getProperty(prefix + "account", ""));
			favorite.setFTPClientClassName(properties.getProperty(prefix + "client",
					RemoteHost.DEFAULT_FTP_CLIENT_CLASS_NAME));
			favorite.setListParserClassName(properties.getProperty(prefix + "parser",
					RemoteHost.DEFAULT_LIST_PARSER_CLASS_NAME));
			favorite.setPassive(Boolean.parseBoolean(properties.getProperty(
					prefix + "passive", "true")));
			favorite.setCommands(properties.getProperty(prefix + "commands", ""));
			favorite.setInitialLocalDirectory(properties.getProperty(
					prefix + "localDirectory", ""));
			favorite.setInitialRemoteDirectory(properties.getProperty(
					prefix + "remoteDirectory", ""));
			favorite.setSSLUsage(integer(properties, prefix + "sslUsage", 0));
			favorite.setDataChannelUnencrypted(Boolean.parseBoolean(properties
					.getProperty(prefix + "dataChannelUnencrypted", "false")));
			favorite.setImplicitSSLPort(integer(properties,
					prefix + "implicitSSLPort", 990));
			favorites.add(favorite);
		}
		return favorites;
	}

	private static void writeFavorite(Properties properties, int index,
			RemoteHost host) {
		String prefix = "favorite." + index + ".";
		properties.setProperty(prefix + "name", value(host.getName()));
		properties.setProperty(prefix + "host", value(host.getHostName()));
		properties.setProperty(prefix + "port", Integer.toString(host.getPort()));
		properties.setProperty(prefix + "user", value(host.getUser()));
		properties.setProperty(prefix + "password", value(host.getPassword()));
		properties.setProperty(prefix + "account", value(host.getAccount()));
		properties.setProperty(prefix + "client", value(host.getFTPClientClassName()));
		properties.setProperty(prefix + "parser", value(host.getListParserClassName()));
		properties.setProperty(prefix + "passive", Boolean.toString(host.isPassive()));
		properties.setProperty(prefix + "commands", value(host.getCommandsAsString()));
		properties.setProperty(prefix + "localDirectory",
				value(host.getInitialLocalDirectory()));
		properties.setProperty(prefix + "remoteDirectory",
				value(host.getInitialRemoteDirectory()));
		properties.setProperty(prefix + "sslUsage", Integer.toString(host.getSSLUsage()));
		properties.setProperty(prefix + "dataChannelUnencrypted",
				Boolean.toString(host.isDataChannelUnencrypted()));
		properties.setProperty(prefix + "implicitSSLPort",
				Integer.toString(host.getImplicitSSLPort()));
	}

	private static String value(String value) {
		return value == null ? "" : value;
	}

	private static int integer(Properties properties, String key, int fallback)
			throws IOException {
		try {
			return Integer.parseInt(properties.getProperty(key,
					Integer.toString(fallback)));
		} catch (NumberFormatException exp) {
			throw new IOException("Invalid favorite value: " + key, exp);
		}
	}

	private static List readLegacyFavorites() throws IOException,
			ClassNotFoundException, IllegalBlockSizeException,
			BadPaddingException {
		try (FileInputStream fin = new FileInputStream(LEGACY_FAV_FILE);
				ObjectInputStream in = new ObjectInputStream(fin)) {
			in.setObjectInputFilter(SerializationFilters.jftpFilter());
			Object obj = in.readObject();
			if (obj instanceof List) {
				return (List) obj;
			}
			if (obj instanceof SealedObject) {
				Object decoded = ((SealedObject) obj)
						.getObject(legacyCipher(Cipher.DECRYPT_MODE));
				if (decoded instanceof List) {
					return (List) decoded;
				}
			}
			throw new IOException("Invalid legacy favorites format");
		}
	}

	/** Compatibility-only reader for pre-5.1 files; new writes never use it. */
	private static Cipher legacyCipher(int mode) throws IOException {
		final byte[] legacyKey = { 28, -9, -23, 35, -93, -47, -28, 55, -83,
				-82, 101, -79, 36, 59, 77, -121 };
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(mode, new SecretKeySpec(legacyKey, "AES"));
			return cipher;
		} catch (GeneralSecurityException exp) {
			throw new IOException("Unable to read legacy favorites", exp);
		}
	}

	private static void checkDataHome() throws IOException {
		if (!JFTP.DATA_HOME.exists() && !JFTP.DATA_HOME.mkdirs()
				&& !JFTP.DATA_HOME.isDirectory()) {
			throw new IOException("Unable to create JFTP data directory: "
					+ JFTP.DATA_HOME);
		}
	}
}
