package com.pau101.nullableeye.config;

public interface InspectorConfig {
	boolean isConsumerInScope(String className);

	boolean isSupplierInScope(String className);

	boolean isConsumerRootInScope(String className);
}
