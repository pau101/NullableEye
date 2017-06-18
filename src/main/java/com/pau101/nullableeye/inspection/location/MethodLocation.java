package com.pau101.nullableeye.inspection.location;

import java.util.Objects;

public final class MethodLocation extends MemberLocation<MethodLocation> {
	private final String desc;

	public MethodLocation(String owner, String name, String desc) {
		this(new ClassLocation(owner), name, desc);
	}

	public MethodLocation(ClassLocation owner, String name, String desc) {
		super(owner, name);
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	@Override
	public MethodLocation withOwner(ClassLocation cls) {
		return new MethodLocation(cls, name, desc);
	}

	public ParameterLocation withParameter(int index) {
		return new ParameterLocation(this, index);
	}

	@Override
	public String toString() {
		return super.toString() + desc;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o) && desc.equals(((MethodLocation) o).desc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, name, desc);
	}

	@Override
	public int compareTo(MethodLocation o) {
		int c = super.compareTo(o);
		if (c != 0) {
			return c;
		}
		return desc.compareTo(o.desc);
	}
}
