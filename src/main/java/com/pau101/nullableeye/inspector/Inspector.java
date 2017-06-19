package com.pau101.nullableeye.inspector;

import com.pau101.nullableeye.config.InspectorConfig;
import com.pau101.nullableeye.inspection.Inspection;
import com.pau101.nullableeye.inspection.location.ClassLocation;
import com.pau101.nullableeye.inspection.location.FieldLocation;
import com.pau101.nullableeye.inspection.location.Location;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

public final class Inspector {
	private static final int CLASSES_INITIAL_CAPACITY = 512;

	private static final String CLASS_GLOB_PATTERN = "glob:**.class";

	private final Remapper remapper = FMLDeobfuscatingRemapper.INSTANCE;

	private final Logger logger;

	private final InspectorConfig config;

	private final ClassBytesProvider classBytesProvider;

	private final NullityProvider nullityProvider;

	private final Map<ClassLocation, ClassInspection> inspections = Collections.synchronizedMap(new TreeMap<>());

	private final InspectorInterpreter.InspectorAnalyzer analyzer = InspectorInterpreter.analyzer(this);

	public Inspector(Logger logger, InspectorConfig config, ClassBytesProvider classBytesProvider, NullityProvider nullityProvider) {
		this.logger = logger;
		this.config = config;
		this.classBytesProvider = classBytesProvider;
		this.nullityProvider = nullityProvider;
	}

	public void recordMethod(Inspection<MethodLocation> inspection) {
		getOrCreateClassInspection(inspection).addMethod(inspection);
	}

	public void recordField(Inspection<FieldLocation> inspection) {
		getOrCreateClassInspection(inspection).addField(inspection);
	}

	public void recordParameter(Inspection<ParameterLocation> inspection) {
		getOrCreateClassInspection(inspection).addMethodParameter(inspection);
	}

	private <T extends Location<T>> ClassInspection getOrCreateClassInspection(Inspection<T> inspection) {
		return inspections.computeIfAbsent(inspection.getLocation().getOwner(), ClassInspection::new);
	}

	public Collection<ClassInspection> getInspections() {
		return inspections.values();
	}

	public boolean isConsumerInScope(String className) {
		return config.isConsumerInScope(className);
	}

	public boolean isConsumerRootInScope(String className) {
		return config.isConsumerRootInScope(className);
	}

	public boolean isSupplierInScope(String className) {
		return config.isSupplierInScope(className);
	}

	public boolean isSupplierInScope(MethodInsnNode invocation) {
		return isInsnOwnerInScope(invocation.owner);
	}

	public boolean isSupplierInScope(FieldInsnNode access) {
		return isInsnOwnerInScope(access.owner);
	}

	private boolean isInsnOwnerInScope(String owner) {
		return isSupplierInScope(remapper.map(owner).replace('/', '.'));
	}

	public MethodLocation getMappedMethod(MethodInsnNode methodInsn) {
		return getMappedMethod(methodInsn.owner, methodInsn.name, methodInsn.desc);
	}

	public MethodLocation getMappedMethod(String className, String methodName, String methodDesc) {
		return new MethodLocation(remapper.map(className), remapper.mapMethodName(className, methodName, methodDesc), remapper.mapMethodDesc(methodDesc));
	}

	public FieldLocation getMappedField(FieldInsnNode methodInsn) {
		return getMappedField(methodInsn.owner, methodInsn.name, methodInsn.desc);
	}

	public FieldLocation getMappedField(String className, String fieldName, String fieldDesc) {
		return new FieldLocation(remapper.map(className), remapper.mapFieldName(className, fieldName, fieldDesc));
	}

	public Nullity getNullity(Location<?> location) {
		return nullityProvider.getNullity(location);
	}

	public void inspect(URL[] urls) {
		logger.info("Starting inspection");
		Set<String> classNames = getClasses(urls);
		logger.info("Found {} classes, loading all...", classNames.size());
		Map<String, byte[]> classBytes = new HashMap<>(classNames.size());
		for (String className : classNames) {
			byte[] bytes = classBytesProvider.getBytes(className);
			if (bytes != null) {
				classBytes.put(className, bytes);
			}
		}
		logger.info("Beginning inspection...");
		for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
			String className = entry.getKey();
			byte[] bytes = entry.getValue();
			logger.info("Starting inspection of {}", className);
			inspect(className, bytes);
		}
	}

	private void inspect(String className, byte[] bytes) {
		ClassNode cls = readClass(bytes);
		logger.info("Analyzing {}...", className);
		for (MethodNode method : cls.methods) {
			try {
				analyzer.analyze(cls.name, method);
			} catch (Exception e) {
				logger.warn("Failed to analyze method {}", method.name, e);
			}
		}
	}

	private Set<String> getClasses(URL[] urls) {
		URI minecraftSource = getClassSource(World.class);
		Set<String> classes = new HashSet<>(CLASSES_INITIAL_CAPACITY);
		logger.info("Searching for classes...");
		for (URL url : urls) {
			URI uri = URI.create(url.toString());
			UnaryOperator<String> classNameTransformer;
			if (uri.equals(minecraftSource)) {
				classNameTransformer = FMLDeobfuscatingRemapper.INSTANCE::map;
			} else {
				classNameTransformer = UnaryOperator.identity();
			}
			Path path;
			try {
				path = Paths.get(uri);
			} catch (FileSystemNotFoundException e) {
				logger.info("Unable to search \"{}\": {}", uri, e.getMessage());
				continue;
			}
			getClasses(path, classNameTransformer, classes);
		}
		return classes;
	}

	private URI getClassSource(Class<?> context) {
		String rawName = context.getName();
		String uri = context.getResource(rawName.substring(rawName.lastIndexOf('.') + 1) + ".class").toString();
		return URI.create(uri.substring(0, uri.indexOf('!')));
	}

	private void getClasses(Path path, UnaryOperator<String> classNameTransformer, Set<String> classes) {
		logger.info("Searching for classes in {}", path);
		FileSystem sys;
		try {
			Iterable<Path> roots;
			UnaryOperator<Path> pathResolver;
			try {
				sys = FileSystems.newFileSystem(path, null);
				roots = sys.getRootDirectories();
				pathResolver = UnaryOperator.identity();
			} catch (ProviderNotFoundException e) {
				sys = path.getFileSystem();
				roots = Collections.singleton(path);
				pathResolver = p -> p.relativize(path);
			}
			PathMatcher matcher = sys.getPathMatcher(CLASS_GLOB_PATTERN);
			ClassSearcher searcher = new ClassSearcher(classNameTransformer, pathResolver, matcher, classes);
			for (Path root : roots) {
				Files.walkFileTree(root, searcher);
			}
		} catch (IOException e) {
			logger.error("Error occurred while searching \"{}\" for classes", path, e);
		}
	}

	public static ClassNode readClass(byte[] bytes) {
		ClassNode cls = new ClassNode();
		ClassReader reader = new ClassReader(bytes);
		reader.accept(cls, 0);
		return cls;
	}

	public static byte[] writeClass(ClassNode cls) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cls.accept(writer);
		return writer.toByteArray();
	}

	private final class ClassSearcher extends SimpleFileVisitor<Path> {
		private final UnaryOperator<String> classNameTransformer;

		private final UnaryOperator<Path> pathResolver;

		private final PathMatcher classMatcher;

		private final Set<String> classes;

		public ClassSearcher(UnaryOperator<String> classNameTransformer, UnaryOperator<Path> pathResolver, PathMatcher classMatcher, Set<String> classes) {
			this.classNameTransformer = classNameTransformer;
			this.pathResolver = pathResolver;
			this.classMatcher = classMatcher;
			this.classes = classes;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			boolean cont;
			Path resolved = pathResolver.apply(dir);
			if (resolved.getParent() == null) {
				cont = true;
			} else {
				cont = isConsumerRootInScope(classNameTransformer.apply(getClassName(resolved)));
			}
			return cont ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (classMatcher.matches(file)) {
				String clazz = classNameTransformer.apply(FilenameUtils.removeExtension(getClassName(pathResolver.apply(file))));
				if (isConsumerInScope(clazz)) {
					classes.add(clazz);
				}
			}
			return FileVisitResult.CONTINUE;
		}

		private String getClassName(Path path) {
			String str = path.toString();
			String sep = path.getFileSystem().getSeparator();
			return str.substring(str.indexOf(sep) + sep.length()).replace(sep, ".");
		}
	}
}
