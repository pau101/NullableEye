package com.pau101.nullableeye.inspection.location;

import java.util.Objects;

public abstract class MemberLocation<T extends MemberLocation<T>> implements Location<T> {
	protected final ClassLocation owner;

	protected final String name;

	public MemberLocation(String owner, String name) {
		this(new ClassLocation(owner), name);
	}

	public MemberLocation(ClassLocation owner, String name) {
		this.owner = owner;
		this.name = name;
	}

	@Override
	public final ClassLocation getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return owner.toString() + "/" + name;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof MemberLocation<?>)) {
			return false;
		}
		MemberLocation<?> that = (MemberLocation<?>) o;
		return Objects.equals(owner, that.owner) && Objects.equals(name, that.name);
	}

	@Override
	public abstract int hashCode();

	@Override
	public int compareTo(T o) {
		int c = owner.compareTo(o.owner);
		if (c != 0) {
			return c;
		}
		return name.compareTo(o.name);
	}
}
