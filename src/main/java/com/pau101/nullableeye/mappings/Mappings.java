package com.pau101.nullableeye.mappings;

import org.objectweb.asm.commons.Remapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class Mappings extends Remapper {
	private final Map<String, String> classes = new HashMap<>();

	private final Map<String, String> fields = new HashMap<>();

	private final Map<String, String> methods = new HashMap<>();

	private Mappings() {}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		return get(methods, owner + "/" + name + desc, name);
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		return get(fields, owner + "/" + name, name);
	}

	@Override
	public String map(String typeName) {
		return get(classes, typeName, typeName);
	}

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
						mappings.classes.put(token1, token2);
						break;
					case "FD:":
						mappings.fields.put(token1, stripClass(token2));
						break;
					case "MD:": {
						mappings.methods.put(token1 + token2, stripClass(in.next()));
						break;
					}
					default:
				}
				in.nextLine();
			}
			return mappings;
		}
	}

	private static String stripClass(String member) {
		return member.substring(member.lastIndexOf('/') + 1);
	}

	private static String get(Map<String, String> map, String descriptor, String defaultValue) {
		return map.getOrDefault(descriptor, defaultValue);
	}
}
