package com.pau101.nullableeye.inspector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.pau101.nullableeye.inspection.Inspection;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class MethodInspection {
	private final MethodLocation location;

	private final Set<Inspection<MethodLocation>> inspections = Collections.synchronizedSet(new TreeSet<>());

	private final Set<Inspection<MethodLocation>> inspectionsView = Collections.unmodifiableSet(inspections);

	private final Multimap<Integer, Inspection<ParameterLocation>> parameterInspections = Multimaps.synchronizedMultimap(TreeMultimap.create());

	private final Multimap<Integer, Inspection<ParameterLocation>> parameterInspectionsView = Multimaps.unmodifiableMultimap(parameterInspections);

	public MethodInspection(MethodLocation location) {
		this.location = location;
	}

	public void add(Inspection<MethodLocation> inspection) {
		checkOwner(inspection.getLocation());
		inspections.add(inspection);
	}

	public void addParameter(Inspection<ParameterLocation> inspection) {
		checkOwner(inspection.getLocation().getMethod());
		parameterInspections.put(inspection.getLocation().getIndex(), inspection);
	}

	public MethodLocation getLocation() {
		return location;
	}

	public Set<Inspection<MethodLocation>> getInspections() {
		return inspectionsView;
	}

	public Multimap<Integer, Inspection<ParameterLocation>> getParameterInspections() {
		return parameterInspectionsView;
	}

	private void checkOwner(MethodLocation location) {
		Preconditions.checkArgument(location.equals(this.location), "Method is (%s), should be (%s)", location, this.location);
	}
}
