package com.pau101.nullableeye;

import com.google.common.collect.ImmutableMap;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class NullableEyeClassTransformer implements IClassTransformer {
	private static final String MINECRAFT_PACKAGE = "net.minecraft.";

	private static final String MINECRAFT_PACKAGE_DESC = MINECRAFT_PACKAGE.replace('.', '/');

	private static final FMLDeobfuscatingRemapper REMAPPER = FMLDeobfuscatingRemapper.INSTANCE;

	private static final String INSPECT_OWNER = Type.getInternalName(NullableEyeClassTransformer.class);

	private static final String INSPECT_NAME = "inspect";

	private static final String INSPECT_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class), Type.getType(String.class));

	private static final Map<String, Nullability> NULLABLITIES = new ConcurrentHashMap<>(4096);

	private static final Set<String> REPORTED = new HashSet<>(1024);

	private static final Logger LOGGER = LogManager.getLogger(NullableEyeLoadingPlugin.NAME);

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if (bytes == null || !transformedName.startsWith(MINECRAFT_PACKAGE)) {
			return bytes;
		}
		ClassNode cls = new ClassNode();
		ClassReader reader = new ClassReader(bytes);
		reader.accept(cls, 0);
		for (MethodNode method : cls.methods) {
			recordNullability(cls, method);
			InsnList insns = method.instructions;
			for (int i = 0; i < insns.size(); i++) {
				AbstractInsnNode node = insns.get(i);
				if (node.getType() == AbstractInsnNode.METHOD_INSN) {
					MethodInsnNode invocation = (MethodInsnNode) node;
					if (!REMAPPER.map(invocation.owner).startsWith(MINECRAFT_PACKAGE_DESC)) {
						continue;
					}
					Type ret = Type.getReturnType(invocation.desc);
					if (ret.getSort() == Type.OBJECT || ret.getSort() == Type.ARRAY) {
						InsnList hook = new InsnList();
						hook.add(new InsnNode(Opcodes.DUP));
						hook.add(new LdcInsnNode(getMethodId(invocation.owner, invocation.name, invocation.desc)));
						hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, INSPECT_OWNER, INSPECT_NAME, INSPECT_DESC, false));
						insns.insert(invocation, hook);
						i += hook.size();
					}
				}
			}
		}
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cls.accept(writer);
		return writer.toByteArray();
	}

	private static void recordNullability(ClassNode cls, MethodNode method) {
		if (method.visibleAnnotations != null) {
			for (AnnotationNode annotation : method.visibleAnnotations) {
				Nullability n = Nullability.get(annotation.desc);
				if (n != null) {
					NULLABLITIES.put(getMethodId(cls.name, method.name, method.desc), n);
					return;
				}
			}
		}
	}

	private static String getMethodId(String className, String methodName, String methodDesc) {
		return REMAPPER.map(className) + "#" + REMAPPER.mapMethodName(className, methodName, methodDesc) + REMAPPER.mapMethodDesc(methodDesc);
	}

	public static void inspect(Object value, String method) {
		if (REPORTED.contains(method)) {
			return;
		}
		Nullability nullability = NULLABLITIES.getOrDefault(method, Nullability.NONNULL);
		if (!nullability.test(value)) {
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			nullability.report(method, stackTrace.length > 2 ? stackTrace[2].toString() : "unknown");
			REPORTED.add(method);
			NULLABLITIES.remove(method);
		}
	}

	private enum Nullability {
		NULLABLE("Ljavax/annotation/Nullable;", o -> true, (m, c) -> {}),
		NONNULL("Ljavax/annotation/Nonnull;", Objects::nonNull, (m, c) -> LOGGER.info("{} is nonnull yet returned null at {}", m, c));

		private static final ImmutableMap<String, Nullability> map;

		static {
			ImmutableMap.Builder<String, Nullability> bob = ImmutableMap.builder();
			for (Nullability nullability : values()) {
				bob.put(nullability.desc, nullability);
			}
			map = bob.build();
		}

		private final String desc;

		private final Predicate<Object> validator;

		private final BiConsumer<String, String> reporter;

		Nullability(String desc, Predicate<Object> validator, BiConsumer<String, String> reporter) {
			this.desc = desc;
			this.validator = validator;
			this.reporter = reporter;
		}

		public final boolean test(Object value) {
			return validator.test(value);
		}

		public final void report(String method, String context) {
			reporter.accept(method, context);
		}

		public static Nullability get(String desc) {
			return map.get(desc);
		}
	}
}
