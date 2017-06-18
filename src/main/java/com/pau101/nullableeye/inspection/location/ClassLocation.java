package com.pau101.nullableeye.inspection.location;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

// TODO: nested classes?
public final class ClassLocation implements Location<ClassLocation> {
	private final ClassLocation outer;

	private final String name;

	public ClassLocation(String name) {
		this(null, name);
	}

	private ClassLocation(ClassLocation outer, String name) {
		this.outer = outer;
		this.name = name;
	}

	@Override
	public ClassLocation getOwner() {
		return outer;
	}

	@Override
	public ClassLocation withOwner(ClassLocation cls) {
		return new ClassLocation(cls, name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof ClassLocation && Objects.equals(outer, ((ClassLocation) o).outer) && name.equals(((ClassLocation) o).name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outer, name);
	}

	@Override
	public int compareTo(ClassLocation o) {
		int c = ObjectUtils.compare(outer, o.outer, true);
		if (c != 0) {
			return c;
		}
		return name.compareTo(o.name);
	}
}
