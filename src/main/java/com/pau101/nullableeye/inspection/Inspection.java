package com.pau101.nullableeye.inspection;

import com.pau101.nullableeye.inspection.location.Location;

import java.util.Objects;

public final class Inspection<L extends Location<L>> implements Comparable<Inspection<L>> {
	private final Discoverer discoverer;

	private final InspectionType inspectionType;

	private final L location;

	public Inspection(Discoverer discoverer, InspectionType inspectionType, L location) {
		this.discoverer = discoverer;
		this.inspectionType = inspectionType;
		this.location = location;
	}

	public Discoverer getDiscoverer() {
		return discoverer;
	}

	public InspectionType getInspectionType() {
		return inspectionType;
	}

	public L getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return inspectionType.getName() + ": " + location.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Inspection)) {
			return false;
		}
		Inspection that = (Inspection) o;
		return discoverer == that.discoverer && inspectionType == that.inspectionType && Objects.equals(location, that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(discoverer, inspectionType, location);
	}

	@Override
	public int compareTo(Inspection<L> o) {
		int c = inspectionType.compareTo(o.getInspectionType());
		if (c != 0) {
			return c;
		}
		return location.compareTo(o.location);
	}
}
