package com.pau101.nullableeye.inspector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.pau101.nullableeye.inspection.Inspection;
import com.pau101.nullableeye.inspection.location.ClassLocation;
import com.pau101.nullableeye.inspection.location.FieldLocation;
import com.pau101.nullableeye.inspection.location.Location;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ClassInspection {
	private final ClassLocation location;

	private final Multimap<FieldLocation, Inspection<FieldLocation>> fieldInspections = Multimaps.synchronizedMultimap(TreeMultimap.create());

	private final Multimap<FieldLocation, Inspection<FieldLocation>> fieldInspectionsView = Multimaps.unmodifiableMultimap(fieldInspections);

	private final Map<MethodLocation, MethodInspection> methodInspections = Collections.synchronizedMap(new TreeMap<>());

	private final Map<MethodLocation, MethodInspection> methodInspectionsView = Collections.unmodifiableMap(methodInspections);

	public ClassInspection(ClassLocation location) {
		this.location = location;
	}

	public void addField(Inspection<FieldLocation> inspection) {
		checkOwner(inspection);
		fieldInspections.put(inspection.getLocation(), inspection);
	}

	public void addMethod(Inspection<MethodLocation> inspection) {
		checkOwner(inspection);
		getOrCreate(inspection.getLocation()).add(inspection);
	}

	public void addMethodParameter(Inspection<ParameterLocation> inspection) {
		checkOwner(inspection);
		getOrCreate(inspection.getLocation().getMethod()).addParameter(inspection);
	}

	public ClassLocation getLocation() {
		return location;
	}

	public Multimap<FieldLocation, Inspection<FieldLocation>> getFieldInspections() {
		return fieldInspectionsView;
	}

	public Map<MethodLocation, MethodInspection> getMethodInspections() {
		return methodInspectionsView;
	}

	private MethodInspection getOrCreate(MethodLocation location) {
		return methodInspections.computeIfAbsent(location, MethodInspection::new);
	}

	private <T extends Location<T>> void checkOwner(Inspection<T> inspection) {
		Preconditions.checkArgument(
			inspection.getLocation().getOwner().equals(location),
			"Owner is (%s), should be (%s)", inspection.getLocation().getOwner(), location
		);
	}
}
