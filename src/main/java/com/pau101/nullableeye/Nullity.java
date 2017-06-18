package com.pau101.nullableeye;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.function.Predicate;

public enum Nullity {
	NULLABLE("Ljavax/annotation/Nullable;", o -> true, (m, c, l) -> {}),
	NONNULL("Ljavax/annotation/Nonnull;", Objects::nonNull, (m, c, l) -> l.info("{} is nonnull yet returned null at {}", m, c));

	private static final ImmutableMap<String, Nullity> map;

	static {
		ImmutableMap.Builder<String, Nullity> bob = ImmutableMap.builder();
		for (Nullity nullity : values()) {
			bob.put(nullity.desc, nullity);
		}
		map = bob.build();
	}

	private final String desc;

	private final Predicate<Object> validator;

	private final TernaryConsumer<String, String, Logger> reporter;

	Nullity(String desc, Predicate<Object> validator, TernaryConsumer<String, String, Logger> reporter) {
		this.desc = desc;
		this.validator = validator;
		this.reporter = reporter;
	}

	public final boolean test(Object value) {
		return validator.test(value);
	}

	public final void report(String method, String context, Logger logger) {
		reporter.accept(method, context, logger);
	}

	public static Nullity get(String desc) {
		return map.get(desc);
	}

	@FunctionalInterface
	private interface TernaryConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}
}
