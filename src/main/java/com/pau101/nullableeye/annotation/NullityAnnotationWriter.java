package com.pau101.nullableeye.annotation;

import com.pau101.nullableeye.inspector.ClassInspection;
import org.objectweb.asm.commons.Remapper;

import java.nio.file.Path;
import java.util.Collection;

public interface NullityAnnotationWriter {
	void write(Collection<ClassInspection> classInspections, Remapper remapper, Path outputDirectory);
}
