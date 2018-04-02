package com.kuckian.jorgy;

import java.util.HashSet;
import java.util.Set;

public class JavaClass implements GraphNode {

	private final String name;
	private final Set<JavaClass> imports = new HashSet<>();
	private final JavaPackage pkg;

	public JavaClass(String name, JavaPackage pkg) {
		this.name = name;
		this.pkg = pkg;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<JavaClass> getImports() {
		return imports;
	}

	public JavaPackage getPackage() {
		return pkg;
	}

}
