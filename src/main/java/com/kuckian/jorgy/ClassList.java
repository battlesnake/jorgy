package com.kuckian.jorgy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassList {
	private final List<String> clss;

	public ClassList(String clss) {
		super();
		this.clss = Arrays.stream(Optional.ofNullable(clss).orElse("").split("\\s*,\\s*")).collect(Collectors.toList());
	}

	public ClassList set(String cls) {
		return set(cls, true);
	}

	public ClassList unset(String cls) {
		return set(cls, false);
	}

	public ClassList set(String cls, boolean isSet) {
		if (isSet) {
			if (!clss.contains(cls)) {
				clss.add(0, cls);
			}
		} else {
			while (clss.remove(cls)) {
				/* Remove all */
			}
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