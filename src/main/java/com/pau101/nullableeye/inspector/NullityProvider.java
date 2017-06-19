package com.pau101.nullableeye.inspector;

import com.pau101.nullableeye.inspection.location.Location;

public interface NullityProvider {
	Nullity getNullity(Location<?> location);
}
