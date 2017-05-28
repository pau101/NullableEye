package com.pau101.nullableeye.mappings;

public final class MethodDescriptor extends Descriptor {
	private final ClassDescriptor classDescriptor;

	private final String keyDesc;

	private final String valueDesc;

	public MethodDescriptor(String key, String value, String keyDesc, String valueDesc) {
		super(stripClass(key), stripClass(value));
		classDescriptor = new ClassDescriptor(isolateClass(key), isolateClass(value));
		this.keyDesc = keyDesc;
		this.valueDesc = valueDesc;
	}

	public ClassDescriptor getClassDescriptor() {
		return classDescriptor;
	}

	public String getKeyDesc() {
		return keyDesc;
	}

	public String getValueDesc() {
		return valueDesc;
	}

	public String getFullKey() {
		return getClassDescriptor().getKey() + "/" + getKey() + getKeyDesc();
	}

	public String getFullValue() {
		return getClassDescriptor().getValue() + "/" + getValue() + getValueDesc();
	}
}
