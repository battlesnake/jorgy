package com.kuckian.jorgy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaPackage implements GraphNode {

	private final String name;
	private final HashSet<JavaType> classes = new HashSet<>();

	public JavaPackage(String name) {
		this.name = name;
	}

	public HashSet<JavaType> getClasses() {
		return classes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<JavaType> getImports() {
		return classes.stream().flatMap(cls -> cls.getImports().stream()).collect(Collectors.toSet());
	}

	public Map<JavaPackage, Set<JavaType>> getPackageDependencies() {
		Map<JavaPackage, Set<JavaType>> res = new HashMap<>();
		for (JavaType cls : classes) {
			for (JavaType dep : cls.getImports()) {
				res.computeIfAbsent(dep.getPackage(), x -> new HashSet<>()).add(cls);
			}
		}
		return res;
	}

}
