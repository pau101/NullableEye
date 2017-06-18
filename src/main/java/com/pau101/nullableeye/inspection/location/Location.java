package com.pau101.nullableeye.inspection.location;

public interface Location<T extends Location<T>> extends Comparable<T> {
	ClassLocation getOwner();

	Location<T> withOwner(ClassLocation cls);
}
