package com.pau101.nullableeye.mappings;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Mappings {
	private final Pattern CLASS_REFERENCE = Pattern.compile("(?<=L).+?(?=;)");

	private final Map<String, ClassDescriptor> classes = new HashMap<>();

	private final Map<String, FieldDescriptor> fields = new HashMap<>();

	private final Map<String, MethodDescriptor> methods = new ConcurrentHashMap<>();

	private final Map<String, PackageDescriptor> packages = new HashMap<>();

	private final Map<String, Multimap<String, MethodDescriptor>> classMethodNames = new HashMap<>();

	private Mappings() {}

	public ClassDescriptor getClass(String descriptor) {
		return get(classes, descriptor, "class");
	}

	public FieldDescriptor getField(String descriptor) {
		return get(fields, descriptor, "field");
	}

	public MethodDescriptor getMethod(String descriptor) {
		if (!methods.containsKey(descriptor)) {
			int ms = descriptor.indexOf('(');
			String keyName = descriptor.substring(0, ms);
			Matcher m = CLASS_REFERENCE.matcher(descriptor);
			StringBuffer valueSig = new StringBuffer();
			while (m.find()) {
				String cls = m.group();
				if (classes.containsKey(cls)) {
					cls = classes.get(cls).getValue();
				}
				m.appendReplacement(valueSig, Matcher.quoteReplacement(cls));
			}
			String valueRef = m.appendTail(valueSig).toString();
			int ns = valueRef.indexOf('(');
			String valueName = valueRef.substring(0, ns);
			String keyDesc = descriptor.substring(ms);
			String valueDesc = valueRef.substring(ns);
			methods.put(descriptor, new MethodDescriptor(keyName, valueName, keyDesc, valueDesc));
		}
		return methods.get(descriptor);
	}

	public Collection<MethodDescriptor> getMethods(String classDescriptor, String methodName) {
		Multimap<String, MethodDescriptor> methods = classMethodNames.get(classDescriptor);
		if (methods == null) {
			return Collections.emptySet();
		}
		return methods.get(methodName);
	}

	public PackageDescriptor getPackage(String descriptor) {
		return get(packages, descriptor, "package");
	}

	private static final Logger LOGGER = LogManager.getLogger("Mappings");

	public static Mappings load(String resourceName) throws IOException {
		try (InputStream mappingsStream = Mappings.class.getResourceAsStream(resourceName)) {
			if (mappingsStream == null) {
				throw new FileNotFoundException(resourceName);
			}
			Scanner in = new Scanner(mappingsStream);
			Mappings mappings = new Mappings();
			while (in.hasNextLine()) {
				String type = in.next();
				String token1 = in.next();
				String token2 = in.next();
				switch (type) {
					case "CL:":
						mappings.classes.put(token1, new ClassDescriptor(token1, token2));
						break;
					case "FD:":
						mappings.fields.put(token1, new FieldDescriptor(token1, token2));
						break;
					case "MD:": {
						String valueKey = in.next();
						String valueDesc = in.next();
						MethodDescriptor descriptor = new MethodDescriptor(token1, valueKey, token2, valueDesc);
						mappings.methods.put(token1 + token2, descriptor);
						mappings.classMethodNames.computeIfAbsent(descriptor.getClassDescriptor().getKey(), k -> HashMultimap.create()).put(descriptor.getKey(), descriptor);
						break;
					}
					case "PK:":
						mappings.packages.put(token1, new PackageDescriptor(token1, token2));
				}
				in.nextLine();
			}
			return mappings;
		}
	}

	private static <T extends Descriptor> T get(Map<String, T> map, String descriptor, String type) {
		T value = map.get(descriptor);
		if (value == null) {
			throw new RuntimeException("Failed to find " + type + ": " + descriptor);
		}
		return value;
	}
}
