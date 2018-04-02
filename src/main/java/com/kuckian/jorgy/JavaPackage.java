package com.kuckian.jorgy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaPackage implements GraphNode {

	private final String name;
	private final HashSet<JavaClass> classes = new HashSet<>();

	public JavaPackage(String name) {
		this.name = name;
	}

	public HashSet<JavaClass> getClasses() {
		return classes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<JavaClass> getImports() {
		return classes.stream().flatMap(cls -> cls.getImports().stream()).collect(Collectors.toSet());
	}

	public Map<JavaPackage, Set<JavaClass>> getPackageDependencies() {
		Map<JavaPackage, Set<JavaClass>> res = new HashMap<>();
		for (JavaClass cls : classes) {
			for (JavaClass dep : cls.getImports()) {
				res.computeIfAbsent(dep.getPackage(), x -> new HashSet<>()).add(cls);
			}
		}
		return res;
	}

}
