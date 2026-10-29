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
package com.myjavaworld.jftp;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import com.myjavaworld.gui.GUIUtil;
import com.myjavaworld.util.ResourceLoader;

/**
 * @author Sai Pullabhotla, psai [at] jMethods [dot] com
 * @version 1.0
 */
public class AutoUpdater extends Thread {

	private static final String KEY_LATEST_VERSION = "latestVersion";
	private static final String KEY_INFO_URL = "infoURL";
	private static final String CHECK_URL = "https://www.jMethods.com/software/jftp/update.properties";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
	private Component parent = null;
	private boolean delay = false;
	private boolean showNoUpdatesMessage = false;

	public AutoUpdater(Component parent, boolean delay,
			boolean ignoreIfNoUpdates) {
		super("Auto Updater Thread");
		setDaemon(true);
		this.parent = parent;
		this.delay = delay;
		this.showNoUpdatesMessage = ignoreIfNoUpdates;
	}

	@Override
	public void run() {
		if (delay) {
			try {
				Thread.sleep(15000);
			} catch (InterruptedException exp) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		for (int attempt = 0; attempt < 3 && !isInterrupted(); attempt++) {
			try {
				URI uri = URI.create(CHECK_URL);
				if (!"https".equalsIgnoreCase(uri.getScheme())) {
					throw new IllegalStateException("Update endpoint must use HTTPS");
				}
				HttpClient client = HttpClient.newBuilder()
						.connectTimeout(REQUEST_TIMEOUT).build();
				HttpRequest request = HttpRequest.newBuilder(uri)
						.timeout(REQUEST_TIMEOUT).GET().build();
				HttpResponse<byte[]> response = client.send(request,
						HttpResponse.BodyHandlers.ofByteArray());
				if (response.statusCode() / 100 != 2) {
					throw new IllegalStateException("Update endpoint returned HTTP "
							+ response.statusCode());
				}
				Properties props = new Properties();
				props.load(new ByteArrayInputStream(response.body()));
				String latestVersion = props.getProperty(KEY_LATEST_VERSION);
				String currentVersion = JFTPConstants.PRODUCT_VERSION;
				String infoURL = props.getProperty(KEY_INFO_URL);
				if (latestVersion != null && latestVersion.trim().length() > 0) {
					final ResourceBundle resources = ResourceLoader
							.getBundle("com.myjavaworld.jftp.AutoUpdater");
					String title = resources.getString("title.dialog");
					if (compareVersions(latestVersion, currentVersion) > 0) {
						MessageFormat mf = new MessageFormat(
								resources.getString("message.updatesAvailable"));
						String message = mf.format(new Object[] {
								JFTPConstants.PRODUCT_NAME, currentVersion,
								latestVersion, infoURL });
						showInformation(title, message, true);
					} else {
						if (showNoUpdatesMessage) {
							showInformation(title, resources
									.getString("message.updatesNotAvailable"), false);
						}
					}
				}
				return;
			} catch (Exception exp) {
				if (isInterrupted()) {
					Thread.currentThread().interrupt();
					return;
				}
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException exp1) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private void showInformation(final String title, final String message,
			final boolean allowLink) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				GUIUtil.showInformation(parent, title, message, allowLink);
			}
		});
	}

	private static int compareVersions(String left, String right) {
		String[] a = left.trim().split("\\.");
		String[] b = right.trim().split("\\.");
		int length = Math.max(a.length, b.length);
		for (int i = 0; i < length; i++) {
			int ai = i < a.length ? parseVersionPart(a[i]) : 0;
			int bi = i < b.length ? parseVersionPart(b[i]) : 0;
			if (ai != bi) {
				return ai < bi ? -1 : 1;
			}
		}
		return 0;
	}

	private static int parseVersionPart(String part) {
		try {
			return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
		} catch (NumberFormatException exp) {
			return 0;
		}
	}
}
