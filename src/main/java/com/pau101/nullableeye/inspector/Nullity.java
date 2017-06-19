package com.pau101.nullableeye.inspector;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.Type;

public enum Nullity {
	NULLABLE("Ljavax/annotation/Nullable;"),
	NONNULL("Ljavax/annotation/Nonnull;");

	private static final ImmutableMap<String, Nullity> map;

	static {
		ImmutableMap.Builder<String, Nullity> bob = ImmutableMap.builder();
		for (Nullity nullity : values()) {
			bob.put(nullity.desc, nullity);
		}
		map = bob.build();
	}

	private final String desc;

	private final String className;

	Nullity(String desc) {
		this.desc = desc;
		this.className = Type.getType(desc).getClassName();
	}

	public String getClassName() {
		return className;
	}

	public static Nullity get(String desc) {
		return map.get(desc);
	}
}
