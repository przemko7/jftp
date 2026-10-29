package com.myjavaworld.zip;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class UnzipSecurityTest {

	@Test
	void rejectsEntriesOutsideTargetDirectory() throws Exception {
		Path root = Files.createTempDirectory("jftp-unzip");
		Path archive = createArchive(root, "../escape.txt", "unsafe");
		Path target = Files.createDirectory(root.resolve("target"));

		Unzip unzip = new Unzip(archive.toFile());
		unzip.setTargetDirectory(target.toFile());
		unzip.open();
		try {
			assertThrows(IOException.class, unzip::unzip);
		} finally {
			unzip.close();
		}
	}

	@Test
	void handlesEmptyEntries() throws Exception {
		Path root = Files.createTempDirectory("jftp-unzip");
		Path archive = createArchive(root, "empty.txt", "");
		Path target = Files.createDirectory(root.resolve("target"));

		Unzip unzip = new Unzip(archive.toFile());
		unzip.setTargetDirectory(target.toFile());
		unzip.open();
		try {
			assertDoesNotThrow(unzip::unzip);
		} finally {
			unzip.close();
		}
	}

	private static Path createArchive(Path root, String name, String contents)
			throws IOException {
		Path archive = root.resolve("test.zip");
		try (ZipOutputStream output = new ZipOutputStream(
				Files.newOutputStream(archive))) {
			output.putNextEntry(new ZipEntry(name));
			output.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			output.closeEntry();
		}
		return archive;
	}
}
