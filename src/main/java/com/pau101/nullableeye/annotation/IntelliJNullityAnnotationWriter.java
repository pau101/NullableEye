package com.pau101.nullableeye.annotation;

import com.google.common.base.Throwables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.pau101.nullableeye.inspection.Inspection;
import com.pau101.nullableeye.inspection.InspectionType;
import com.pau101.nullableeye.inspection.location.FieldLocation;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;
import com.pau101.nullableeye.inspector.ClassInspection;
import com.pau101.nullableeye.inspector.MethodInspection;
import com.pau101.nullableeye.inspector.Nullity;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

public final class IntelliJNullityAnnotationWriter implements NullityAnnotationWriter {
	private final DocumentBuilderFactory docBldrFactory = DocumentBuilderFactory.newInstance();

	private final Logger logger;

	public IntelliJNullityAnnotationWriter(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void write(Collection<ClassInspection> classInspections, Remapper remapper, Path outputDirectory) {
		DocumentBuilder bob;
		try {
			bob = docBldrFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw Throwables.propagate(e);
		}
		logger.info("Beginning annotation generation in {}", outputDirectory.toAbsolutePath());
		Multimap<String, ClassEntry> packages = TreeMultimap.create(Comparator.naturalOrder(), Comparator.naturalOrder());
		for (ClassInspection insp : classInspections) {
			String className = Type.getObjectType(remapper.map(insp.getLocation().getName())).getClassName();
			int pkgEnd = className.lastIndexOf('.');
			packages.put(pkgEnd == -1 ? "" : className.substring(0, pkgEnd), new ClassEntry(className.replace('$', '.'), insp));
		}
		for (Map.Entry<String, Collection<ClassEntry>> pkg : packages.asMap().entrySet()) {
			Path packageDir = outputDirectory.resolve(pkg.getKey().replace('.', '/'));
			Path annotations = packageDir.resolve("annotations.xml");
			Document doc = bob.newDocument();
			Element root = doc.createElement("root");
			for (ClassEntry entry : pkg.getValue()) {
				append(remapper, doc, root, entry.className, entry.inspection);
			}
			doc.appendChild(root);
			logger.info("Writing annotations for {}", pkg.getKey());
			if (!Files.exists(packageDir)) {
				try {
					Files.createDirectories(packageDir);
				} catch (IOException e) {
					logger.error("Error creating directories for annotation file", e);
					continue;
				}
			}
			try (OutputStream out = Files.newOutputStream(annotations)) {
				Transformer tr = TransformerFactory.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.METHOD, "xml");
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
				tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				tr.transform(new DOMSource(doc), new StreamResult(out));
			} catch (IOException | TransformerException e) {
				logger.error("Error writing annotation file", e);
			}
		}
	}

	private void append(Remapper remapper, Document doc, Element root, String className, ClassInspection inspection) {
		logger.info("Starting generation of {}'s annotations", className);
		for (Map.Entry<FieldLocation, Collection<Inspection<FieldLocation>>> field : inspection.getFieldInspections().asMap().entrySet()) {
			FieldLocation loc = field.getKey();
			String name = className + " " + remapper.mapFieldName(loc.getOwner().getName(), loc.getName(), null);
			Element fieldItem = createItemNode(doc, name);
			for (Inspection<FieldLocation> insp : field.getValue()) {
				appendInspection(doc, fieldItem, insp);
			}
			root.appendChild(fieldItem);
		}
		for (MethodInspection method : inspection.getMethodInspections().values()) {
			String name = getMethodName(remapper, className, method.getLocation());
			if (method.getInspections().size() > 0) {
				Element methodItem = createItemNode(doc, name);
				for (Inspection<MethodLocation> insp : method.getInspections()) {
					appendInspection(doc, methodItem, insp);
				}
				root.appendChild(methodItem);
			}
			for (Map.Entry<Integer, Collection<Inspection<ParameterLocation>>> parameter : method.getParameterInspections().asMap().entrySet()) {
				Element paramItem = createItemNode(doc, name + " " + parameter.getKey());
				for (Inspection<ParameterLocation> insp : parameter.getValue()) {
					appendInspection(doc, paramItem, insp);
				}
				root.appendChild(paramItem);
			}
		}
	}

	private Element createItemNode(Document doc, String name) {
		Element item = doc.createElement("item");
		item.setAttribute("name", name);
		return item;
	}

	private Element createAnnotationNode(Document doc, String annotationType) {
		Element annotation = doc.createElement("annotation");
		annotation.setAttribute("name", annotationType);
		return annotation;
	}

	private void appendInspection(Document doc, Element item, Inspection<?> insp) {
		if (insp.getInspectionType() == InspectionType.POSSIBLY_NULLABLE) {
			item.appendChild(createAnnotationNode(doc, Nullity.NULLABLE.getClassName()));
		}
	}

	private String getMethodName(Remapper remapper, String className, MethodLocation loc) {
		StringBuilder nameBldr = new StringBuilder(className).append(' ');
		String desc = remapper.mapMethodDesc(loc.getDesc());
		nameBldr.append(Type.getReturnType(desc).getClassName());
		nameBldr.append(' ');
		String name = loc.getName();
		if ("<init>".equals(name)) {
			nameBldr.append(className.substring(className.lastIndexOf('.') + 1));
		} else {
			nameBldr.append(remapper.mapMethodName(loc.getOwner().getName(), name, loc.getDesc()));
		}
		nameBldr.append('(');
		Type[] args = Type.getArgumentTypes(desc);
		if (args.length > 0) {
			for (int i = 0;;) {
				nameBldr.append(args[i++].getClassName());
				if (i == args.length) {
					break;
				}
				nameBldr.append(", ");
			}
		}
		return nameBldr.append(')').toString();
	}

	private final class ClassEntry implements Comparable<ClassEntry> {
		private final String className;

		private final ClassInspection inspection;

		private ClassEntry(String className, ClassInspection inspection) {
			this.className = className;
			this.inspection = inspection;
		}

		@Override
		public int compareTo(IntelliJNullityAnnotationWriter.ClassEntry o) {
			return className.compareTo(o.className);
		}
	}
}
