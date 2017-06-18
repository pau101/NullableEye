package com.pau101.nullableeye.inspection;

import org.apache.commons.lang3.text.WordUtils;

public enum InspectionType {
	POSSIBLY_NULLABLE,
	UNSAFE_DEREFERENCE;

	private final String name;

	InspectionType() {
		name = WordUtils.capitalizeFully(name().replace('_', ' '));
	}

	public String getName() {
		return name;
	}
}
