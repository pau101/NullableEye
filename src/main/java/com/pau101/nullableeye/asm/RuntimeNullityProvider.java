package com.pau101.nullableeye.asm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.pau101.nullableeye.NullableEye;
import com.pau101.nullableeye.Nullity;
import com.pau101.nullableeye.inspection.location.ClassLocation;
import com.pau101.nullableeye.inspection.location.Location;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspector.Inspector;
import com.pau101.nullableeye.inspector.NullityProvider;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeNullityProvider implements NullityProvider {
	private static final int INITIAL_NULLITIES = 4096;

	private static RuntimeNullityProvider instance;

	private final Map<Location<?>, Nullity> nullities = new ConcurrentHashMap<>(INITIAL_NULLITIES);

	private final Multimap<ClassLocation, ClassLocation> supertypes = HashMultimap.create();

	private RuntimeNullityProvider() {}

	private void putNullity(Location<?> name, Nullity nullity) {
		nullities.put(name, nullity);
	}

	private void putSupertype(ClassLocation type, ClassLocation supertype) {
		supertypes.put(type, supertype);
	}

	@Override
	public Nullity getNullity(Location<?> location) {
		return nullities.computeIfAbsent(location, this::findNullity);
	}

	private Nullity findNullity(Location<?> location) {
		ClassLocation owner = location.getOwner();
		if (owner != null) {
			Deque<ClassLocation> classes = new ArrayDeque<>();
			classes.push(owner);
			while (classes.size() > 0) {
				ClassLocation loc = classes.poll();
				Location<?> reowned = location.withOwner(loc);
				Nullity nullity = nullities.get(reowned);
				if (nullity != null) {
					return nullity;
				}
				classes.addAll(supertypes.get(loc));
			}
		}
		return Nullity.NONNULL;
	}

	public static RuntimeNullityProvider instance() {
		if (instance == null) {
			instance = new RuntimeNullityProvider();
		}
		return instance;
	}

	public static final class NullityIntercepter implements IClassTransformer {
		private final Inspector inspector = NullableEye.instance().getInspector();

		private final RuntimeNullityProvider provider = instance();

		@Override
		public byte[] transform(String name, String transformedName, byte[] bytes) {
			if (bytes != null && inspector.isSupplierInScope(transformedName)) {
				ClassNode cls = Inspector.readClass(bytes);
				recordSupertypes(cls);
				for (FieldNode field : cls.fields) {
					recordFieldNullity(cls, field);
				}
				for (MethodNode method : cls.methods) {
					recordMethodNullity(cls, method);
				}
			}
			return bytes;
		}

		private void recordSupertypes(ClassNode cls) {
			ClassLocation loc = new ClassLocation(cls.name);
			if (cls.superName != null) {
				provider.putSupertype(loc, new ClassLocation(cls.superName));
			}
			cls.interfaces.forEach(i -> provider.putSupertype(loc, new ClassLocation(i)));
		}

		private void recordFieldNullity(ClassNode cls, FieldNode field) {
			Nullity nullity = getNullity(field.visibleAnnotations);
			if (nullity != null) {
				provider.putNullity(inspector.getMappedField(cls.name, field.name, field.desc), nullity);
			}
		}

		private void recordMethodNullity(ClassNode cls, MethodNode method) {
			Nullity nullity = getNullity(method.visibleAnnotations);
			if (nullity != null) {
				provider.putNullity(inspector.getMappedMethod(cls.name, method.name, method.desc), nullity);
			}
			recordParameterNullity(cls, method);
		}

		private void recordParameterNullity(ClassNode cls, MethodNode method) {
			List<AnnotationNode>[] paramAnnotations = method.visibleParameterAnnotations;
			if (paramAnnotations != null) {
				MethodLocation methodLoc = inspector.getMappedMethod(cls.name, method.name, method.desc);
				for (int i = 0; i < paramAnnotations.length; i++) {
					List<AnnotationNode> annotations = paramAnnotations[i];
					Nullity nullity = getNullity(annotations);
					if (nullity != null) {
						provider.putNullity(methodLoc.withParameter(i), nullity);
					}
				}
			}
		}

		private Nullity getNullity(List<AnnotationNode> annotations) {
			if (annotations != null) {
				for (AnnotationNode annotation : annotations) {
					Nullity nullity = Nullity.get(annotation.desc);
					if (nullity != null) {
						return nullity;
					}
				}
			}
			return null;
		}
	}
}
