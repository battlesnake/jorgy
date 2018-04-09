package com.kuckian.jorgy;

import java.util.HashSet;
import java.util.Set;

public class JavaType implements GraphNode {

	private final String name;
	private final Set<JavaType> imports = new HashSet<>();
	private final JavaPackage pkg;
	private final TypeType type;

	public JavaType(String name, JavaPackage pkg, TypeType type) {
		this.name = name;
		this.pkg = pkg;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<JavaType> getImports() {
		return imports;
	}

	public JavaPackage getPackage() {
		return pkg;
	}

	public TypeType getType() {
		return type;
	}

}
