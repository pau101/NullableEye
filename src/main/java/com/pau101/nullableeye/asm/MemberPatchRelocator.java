package com.pau101.nullableeye.asm;

import com.pau101.nullableeye.NullableEye;
import com.pau101.nullableeye.inspection.location.ClassLocation;
import com.pau101.nullableeye.inspection.location.FieldLocation;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;
import com.pau101.nullableeye.inspector.Inspector;
import com.pau101.nullableeye.inspector.Relocator;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MemberPatchRelocator implements Relocator {
	private static final Inspector INSPECTOR = NullableEye.instance().getInspector();

	private static MemberPatchRelocator instance;

	private final Map<FieldLocation, FieldLocation> fields = new HashMap<>();

	private final Map<MethodLocation, MethodLocation> methods = new HashMap<>();

	private MemberPatchRelocator() {}

	private void put(FieldLocation key, FieldLocation value) {
		fields.put(key, value);
	}

	private void put(MethodLocation key, MethodLocation value) {
		methods.put(key, value);
	}

	@Override
	public ClassLocation relocate(ClassLocation location) {
		return location;
	}

	@Override
	public FieldLocation relocate(FieldLocation location) {
		return fields.getOrDefault(location, location);
	}

	@Override
	public MethodLocation relocate(MethodLocation location) {
		return methods.getOrDefault(location, location);
	}

	@Override
	public ParameterLocation relocate(ParameterLocation location) {
		return location.withMethod(relocate(location.getMethod()));
	}

	public static MemberPatchRelocator instance() {
		if (instance == null) {
			instance = new MemberPatchRelocator();
		}
		return instance;
	}

	public static final class Catcher implements IClassTransformer {
		private final MemberPatchRelocator catcher = instance();

		private final String packageLocation;

		private final IClassTransformer transformer;

		public Catcher(String packageLocation, IClassTransformer transformer) {
			this.packageLocation = packageLocation;
			this.transformer = transformer;
		}

		@Override
		public byte[] transform(String name, String transformedName, byte[] bytes) {
			if (INSPECTOR.isSupplierInScope(transformedName)) {
				if (bytes != null) {
					MemberSet members = getMembers(bytes);
					bytes = transformer.transform(name, transformedName, bytes);
					MemberSet transformedMembers = getMembers(bytes);
					ClassLocation owner = new ClassLocation(packageLocation + transformedName);
					transformedMembers.fields.removeAll(members.fields);
					for (FieldLocation field : transformedMembers.fields) {
						catcher.put(field, field.withOwner(owner));
					}
					transformedMembers.methods.removeAll(members.methods);
					for (MethodLocation method : transformedMembers.methods) {
						catcher.put(method, method.withOwner(owner));
					}
				}
				return bytes;
			}
			return transformer.transform(name, transformedName, bytes);
		}
	}

	private static MemberSet getMembers(byte[] bytes) {
		ClassNode cls = Inspector.readClass(bytes);
		MemberSet members = new MemberSet();
		for (FieldNode field : cls.fields) {
			members.fields.add(INSPECTOR.getMappedField(cls.name, field));
		}
		for (MethodNode method : cls.methods) {
			members.methods.add(INSPECTOR.getMappedMethod(cls.name, method));
		}
		return members;
	}

	private static final class MemberSet {
		final Set<FieldLocation> fields = new HashSet<>();

		final Set<MethodLocation> methods = new HashSet<>();
	}
}
