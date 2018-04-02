package com.kuckian.jorgy;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassList {
	private final Set<String> clss;

	public ClassList(String clss) {
		super();
		this.clss = Arrays.stream(Optional.ofNullable(clss).orElse("").split("\\s*,\\s*")).collect(Collectors.toSet());
	}

	public ClassList set(String cls) {
		return set(cls, true);
	}

	public ClassList unset(String cls) {
		return set(cls, false);
	}

	public ClassList set(String cls, boolean isSet) {
		if (isSet) {
			clss.add(cls);
		} else {
			clss.remove(cls);
		}
		return this;
	}

	public ClassList toggle(String cls) {
		return set(cls, !has(cls));
	}

	public boolean has(String cls) {
		return clss.contains(cls);
	}

	public String toString() {
		return String.join(", ", clss);
	}

}