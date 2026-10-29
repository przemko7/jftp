/*
 * Copyright 2012 jMethods, Inc.
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.myjavaworld.jftp;

import java.io.ObjectInputFilter;

/** Serialization guard used only while legacy profile files are migrated. */
final class SerializationFilters {

	private SerializationFilters() {
	}

	static ObjectInputFilter jftpFilter() {
		return new ObjectInputFilter() {
			public Status checkInput(FilterInfo info) {
				if (info.depth() > 20 || info.references() > 2000
						|| info.arrayLength() > 10000) {
					return Status.REJECTED;
				}
				Class<?> type = info.serialClass();
				if (type == null) {
					return Status.UNDECIDED;
				}
				if (type.isArray()) {
					type = type.getComponentType();
				}
				if (type.isPrimitive()) {
					return Status.ALLOWED;
				}
				String name = type.getName();
				if (name.startsWith("com.myjavaworld.jftp.")
						|| name.startsWith("java.lang.")
						|| name.startsWith("java.util.")
						|| name.startsWith("java.awt.")
						|| name.startsWith("javax.crypto.")) {
					return Status.ALLOWED;
				}
				return Status.REJECTED;
			}
		};
	}
}
