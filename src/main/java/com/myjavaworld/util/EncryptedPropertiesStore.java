/*
 * Copyright 2012 jMethods, Inc.
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.myjavaworld.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Small authenticated-encryption envelope for local JFTP properties. */
public final class EncryptedPropertiesStore {

	private static final byte[] MAGIC = { 'J', 'F', 'T', 'P', 1 };
	private static final int KEY_SIZE = 32;
	private static final int NONCE_SIZE = 12;

	private EncryptedPropertiesStore() {
	}

	public static SecretKeySpec loadOrCreateKey(File dataHome)
			throws IOException {
		if (!dataHome.exists() && !dataHome.mkdirs() && !dataHome.isDirectory()) {
			throw new IOException("Unable to create data directory: " + dataHome);
		}
		File keyFile = new File(dataHome, "secrets.key");
		if (keyFile.exists()) {
			byte[] key = Files.readAllBytes(keyFile.toPath());
			if (key.length != KEY_SIZE) {
				throw new IOException("Invalid JFTP secret key length");
			}
			return new SecretKeySpec(key, "AES");
		}
		byte[] key = new byte[KEY_SIZE];
		new SecureRandom().nextBytes(key);
		Path temp = new File(dataHome, "secrets.key.tmp").toPath();
		Files.write(temp, key);
		try {
			Files.move(temp, keyFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch (java.nio.file.AtomicMoveNotSupportedException exp) {
			Files.move(temp, keyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		keyFile.setReadable(false, false);
		keyFile.setWritable(false, false);
		keyFile.setReadable(true, true);
		keyFile.setWritable(true, true);
		return new SecretKeySpec(key, "AES");
	}

	public static Properties load(File file, SecretKeySpec key)
			throws IOException {
		try {
			byte[] encoded = Files.readAllBytes(file.toPath());
			if (encoded.length < MAGIC.length + NONCE_SIZE) {
				throw new IOException("Encrypted properties file is truncated");
			}
			for (int i = 0; i < MAGIC.length; i++) {
				if (encoded[i] != MAGIC[i]) {
					throw new IOException("Unknown encrypted properties format");
				}
			}
			byte[] nonce = new byte[NONCE_SIZE];
			System.arraycopy(encoded, MAGIC.length, nonce, 0, NONCE_SIZE);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
			byte[] plain = cipher.doFinal(encoded, MAGIC.length + NONCE_SIZE,
					encoded.length - MAGIC.length - NONCE_SIZE);
			Properties properties = new Properties();
			properties.load(new ByteArrayInputStream(plain));
			return properties;
		} catch (GeneralSecurityException exp) {
			throw new IOException("Unable to decrypt properties", exp);
		}
	}

	public static void save(File file, Properties properties,
			SecretKeySpec key) throws IOException {
		try {
			ByteArrayOutputStream plain = new ByteArrayOutputStream();
			properties.store(plain, "JFTP settings");
			byte[] nonce = new byte[NONCE_SIZE];
			new SecureRandom().nextBytes(nonce);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key,
					new GCMParameterSpec(128, nonce));
			byte[] encrypted = cipher.doFinal(plain.toByteArray());
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();
			encoded.write(MAGIC);
			encoded.write(nonce);
			encoded.write(encrypted);
			Path temp = new File(file.getParentFile(), file.getName() + ".tmp")
					.toPath();
			Files.write(temp, encoded.toByteArray());
			try {
				Files.move(temp, file.toPath(), StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException exp) {
				Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (GeneralSecurityException exp) {
			throw new IOException("Unable to encrypt properties", exp);
		}
	}
}
