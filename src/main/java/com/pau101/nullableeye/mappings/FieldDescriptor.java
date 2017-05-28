package com.pau101.nullableeye.mappings;

public final class FieldDescriptor extends Descriptor {
	private final ClassDescriptor classRef;

	public FieldDescriptor(String key, String value) {
		super(stripClass(key), stripClass(value));
		classRef = new ClassDescriptor(isolateClass(key), isolateClass(value));
	}

	public ClassDescriptor getClassDescriptor() {
		return classRef;
	}
}
