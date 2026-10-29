package com.myjavaworld.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class EncryptedPropertiesStoreTest {

	@Test
	void roundTripsPropertiesWithAuthenticatedEncryption() throws Exception {
		Path dataHome = Files.createTempDirectory("jftp-settings");
		Path file = dataHome.resolve("settings.dat");
		SecretKeySpec key = EncryptedPropertiesStore.loadOrCreateKey(
				dataHome.toFile());
		Properties expected = new Properties();
			expected.setProperty("host", "example.test");
			expected.setProperty("password", "p@ssword");

		EncryptedPropertiesStore.save(file.toFile(), expected, key);
		Properties actual = EncryptedPropertiesStore.load(file.toFile(), key);

		assertEquals(expected, actual);
	}
}
