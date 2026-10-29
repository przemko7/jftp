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
package com.myjavaworld.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.event.EventListenerList;

import com.myjavaworld.util.ProgressEvent;
import com.myjavaworld.util.ProgressListener;

/**
 * @author Sai Pullabhotla
 */
public class Unzip {

	/**
	 * Zip file to unzip
	 */
	private File file = null;
	/**
	 * Directory to unzip the contents
	 */
	private File targetDirectory = null;
	/**
	 * ZipFile object to read the file.
	 */
	private ZipFile zipFile = null;
	/**
	 * Event listener list
	 */
	private EventListenerList listenerList = null;

	public Unzip(File file) {
		this.file = file;
		this.targetDirectory = file.getParentFile();
		listenerList = new EventListenerList();
	}

	public File getFile() {
		return file;
	}

	public void setTargetDirectory(File targetDirectory) {
		this.targetDirectory = targetDirectory;
	}

	public File getTargetDirectory() {
		return targetDirectory;
	}

	public void addZipListener(ZipListener l) {
		listenerList.add(ZipListener.class, l);
	}

	public void removeZipListener(ZipListener l) {
		listenerList.remove(ZipListener.class, l);
	}

	public void addProgressListener(ProgressListener l) {
		listenerList.add(ProgressListener.class, l);
	}

	public void removeProgressListener(ProgressListener l) {
		listenerList.remove(ProgressListener.class, l);
	}

	protected void fireBeginFileEvent(File file) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ZipListener.class) {
				((ZipListener) listeners[i + 1]).beginFile(new ZipEvent(this,
						ZipEvent.UNZIP, file.getName()));
			}
		}
	}

	protected void fireEndFileEvent(File file) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ZipListener.class) {
				((ZipListener) listeners[i + 1]).endFile(new ZipEvent(this,
						ZipEvent.UNZIP, file.getName()));
			}
		}
	}

	protected void fireProgressEvent(int progress) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ProgressListener.class) {
				((ProgressListener) listeners[i + 1])
						.progressChanged(new ProgressEvent(this, progress));
			}
		}
	}

	public void open() throws ZipException, IOException {
		if (zipFile != null) {
			throw new IllegalStateException("ZipFile is already open");
		}
		zipFile = new ZipFile(file);
	}

	public void close() throws IOException {
		if (zipFile == null) {
			throw new IllegalStateException(
					"ZipFile is not open or is already closed");
		}
		try {
			zipFile.close();
		} finally {
			zipFile = null;
		}
	}

	public void unzip() throws IOException {
		if (zipFile == null) {
			throw new IllegalStateException("ZipFile is not open");
		}
		Enumeration entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			Path target = secureTarget(entry);
			if (entry.isDirectory()) {
				Files.createDirectories(target);
				if (entry.getTime() >= 0) {
					Files.setLastModifiedTime(target,
							java.nio.file.attribute.FileTime.fromMillis(entry.getTime()));
				}
			} else {
				unzipFile(entry, target);
			}
		}
	}

	private Path secureTarget(ZipEntry entry) throws IOException {
		Path root = targetDirectory.toPath().toRealPath();
		Path target = root.resolve(entry.getName()).normalize();
		if (!target.startsWith(root)) {
			throw new IOException("ZIP entry escapes extraction directory: "
					+ entry.getName());
		}
		Path parent = target.getParent();
		if (parent != null) {
			Path current = root;
			for (Path component : root.relativize(parent)) {
				current = current.resolve(component);
				if (Files.isSymbolicLink(current)) {
					throw new IOException("ZIP entry traverses symbolic link: "
							+ entry.getName());
				}
			}
		}
		if (Files.isSymbolicLink(target)) {
			throw new IOException("ZIP entry targets symbolic link: "
					+ entry.getName());
		}
		return target;
	}

	private void unzipFile(ZipEntry entry, Path target) throws IOException {
		fireBeginFileEvent(target.toFile());
		Path parentDir = target.getParent();
		if (parentDir != null) {
			Files.createDirectories(parentDir);
		}
		final int bufferSize = 16 * 1024;
		byte[] buffer = new byte[bufferSize];
		int bytesRead = 0;
		long totalBytesRead = 0L;

		try (BufferedInputStream bin = new BufferedInputStream(
				zipFile.getInputStream(entry), bufferSize);
				BufferedOutputStream bout = new BufferedOutputStream(
						new FileOutputStream(target.toFile()), bufferSize)) {
			while ((bytesRead = bin.read(buffer)) != -1) {
				bout.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				int progress = entry.getSize() <= 0 ? 100
						: (int) Math.min(100,
								(totalBytesRead * 100) / entry.getSize());
				fireProgressEvent(progress);
			}
		} finally {
			fireEndFileEvent(target.toFile());
		}
	}
}
