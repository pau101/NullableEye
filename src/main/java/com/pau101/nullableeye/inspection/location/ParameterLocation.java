package com.pau101.nullableeye.inspection.location;

import java.util.Objects;

public final class ParameterLocation implements Location<ParameterLocation> {
	private final MethodLocation method;

	private final int index;

	public ParameterLocation(MethodLocation method, int index) {
		this.method = method;
		this.index = index;
	}

	@Override
	public ClassLocation getOwner() {
		return method.getOwner();
	}

	public MethodLocation getMethod() {
		return method;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public ParameterLocation withOwner(ClassLocation cls) {
		return new ParameterLocation(method.withOwner(cls), index);
	}

	@Override
	public String toString() {
		return method.toString() + " " + index;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ParameterLocation)) {
			return false;
		}
		ParameterLocation other = (ParameterLocation) o;
		return index == other.index && Objects.equals(method, other.method);
	}

	@Override
	public int hashCode() {
		return Objects.hash(method, index);
	}

	@Override
	public int compareTo(ParameterLocation o) {
		int c = method.compareTo(o.method);
		if (c != 0) {
			return c;
		}
		return Integer.compare(index, o.index);
	}
}
