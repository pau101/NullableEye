package com.pau101.nullableeye.mappings;

public abstract class Descriptor {
	protected final String key;

	protected final String value;

	public Descriptor(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return 31 * (31 + key.hashCode()) + value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Descriptor) {
			Descriptor other = (Descriptor) obj;
			return key.equals(other.key) && value.equals(other.value);
		}
		return false;
	}

	protected static String stripClass(String ref) {
		return ref.substring(ref.lastIndexOf('/') + 1);
	}

	protected static String isolateClass(String ref) {
		return ref.substring(0, ref.lastIndexOf('/'));
	}

	@Override
	public String toString() {
		return "Descriptor[key=" + key + ", value=" + value + "]";
	}
}
