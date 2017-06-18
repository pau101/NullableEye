package com.pau101.nullableeye.inspection.location;

import java.util.Objects;

public final class FieldLocation extends MemberLocation<FieldLocation> {
	public FieldLocation(String owner, String name) {
		super(owner, name);
	}

	public FieldLocation(ClassLocation owner, String name) {
		super(owner, name);
	}

	@Override
	public FieldLocation withOwner(ClassLocation cls) {
		return new FieldLocation(cls, name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, name);
	}
}
