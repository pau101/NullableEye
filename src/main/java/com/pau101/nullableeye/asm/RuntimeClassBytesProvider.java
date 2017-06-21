package com.pau101.nullableeye.asm;

import com.google.common.base.Throwables;
import com.pau101.nullableeye.NullableEye;
import com.pau101.nullableeye.inspector.ClassBytesProvider;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeClassBytesProvider implements ClassBytesProvider {
	private static final int CLASSES_INITIAL_CAPACITY = 512;

	private static RuntimeClassBytesProvider instance;

	private final Logger logger = NullableEye.getLogger();

	private final Map<String, byte[]> classBytes = new ConcurrentHashMap<>(CLASSES_INITIAL_CAPACITY);

	private RuntimeClassBytesProvider() {}

	@Override
	public byte[] getBytes(String className) {
		byte[] bytes = classBytes.get(className);
		if (bytes == null) {
			try {
				Class.forName(className);
				bytes = classBytes.get(className);
				if (bytes == null) {
					logger.warn("Unable to intercept bytes of {}", className);
				}
			} catch (Throwable e) {
				Throwables.propagateIfInstanceOf(e, OutOfMemoryError.class);
				Throwable err = e;
				while (true) {
					Throwable c = err.getCause();
					if (c == null) {
						break;
					}
					err = c;
				}
				logger.warn("Failed to load class: {}: {}", err.getClass().getName(), err.getMessage());
			}
		}
		return bytes;
	}

	private void putClass(String name, byte[] bytes) {
		classBytes.put(name, bytes);
	}

	public static RuntimeClassBytesProvider instance() {
		if (instance == null) {
			instance = new RuntimeClassBytesProvider();
		}
		return instance;
	}

	public static final class ClassBytesInterceptor implements IClassTransformer {
		private final RuntimeClassBytesProvider provider = instance();

		@Override
		public byte[] transform(String name, String transformedName, byte[] bytes) {
			if (bytes != null) {
				provider.putClass(transformedName, bytes);
			}
			return bytes;
		}
	}
}
