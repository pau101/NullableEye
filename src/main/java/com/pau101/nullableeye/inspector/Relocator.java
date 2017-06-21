package com.pau101.nullableeye.inspector;

import com.pau101.nullableeye.inspection.location.ClassLocation;
import com.pau101.nullableeye.inspection.location.FieldLocation;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;

public interface Relocator {
	ClassLocation relocate(ClassLocation location);

	FieldLocation relocate(FieldLocation location);

	MethodLocation relocate(MethodLocation location);

	ParameterLocation relocate(ParameterLocation location);
}
