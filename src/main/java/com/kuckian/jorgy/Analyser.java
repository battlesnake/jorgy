package com.kuckian.jorgy;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

public class Analyser {

	private static class Source {
		public String pkg;
		public String name;
		public Set<String> deps;
	}

	private final JavaParser parser = new JavaParser();

	private String prefix = "";

	@SuppressWarnings("serial")
	public static class ParseFailedException extends Exception {
		public ParseFailedException(String arg0) {
			super(arg0);
		}
	}

	private Source readSource(Path path) throws IOException, ParseFailedException {
		CompilationUnit unit = parser.parse(COMPILATION_UNIT, provider(path)).getResult().orElse(null);
		if (unit == null) {
			throw new ParseFailedException("Failed to parse " + path.toString());
		}
		PackageDeclaration pkgDec = unit.getPackageDeclaration().orElse(null);
		if (pkgDec == null) {
			throw new ParseFailedException("Failed to find package declaration in " + path.toString());
		}
		if (unit.getTypes().isEmpty()) {
			throw new ParseFailedException("Failed to find any type declarations in " + path.toString());
		}
		TypeDeclaration<?> typeDec = unit.getType(0);
		NodeList<ImportDeclaration> imports = unit.getImports();
		Source source = new Source();
		source.pkg = pkgDec.getNameAsString();
		source.name = typeDec.getNameAsString();
		source.deps = new HashSet<>();
		imports.forEach(importDec -> source.deps.add(importDec.getNameAsString()));
		return source;
	}

	public Analyser() {
	}

	public Set<JavaPackage> run(Path root) throws IOException, ParseFailedException {

		/* Get paths */
		List<Path> paths;
		try (Stream<Path> stream = Files.walk(root)) {
			paths = stream.collect(Collectors.toList());
		}

		/* Parse sources */
		Map<String, Source> sources = new HashMap<>();
		for (Path path : paths) {
			if (Files.isDirectory(path)) {
				continue;
			}
			try {
				Source source = readSource(path);
				sources.put(source.pkg + "." + source.name, source);
			} catch (ParseFailedException e) {
				System.err.println(
						String.format("Failed to parse <%s>: %s", root.relativize(path).toString(), e.getMessage()));
			}
		}

		/* Resolve packages and classes */
		Map<String, JavaPackage> pkgMap = new HashMap<>();
		Map<String, JavaClass> clsMap = new HashMap<>();
		for (Map.Entry<String, Source> kv : sources.entrySet()) {
			String name = kv.getKey();
			Source source = kv.getValue();
			if (source.pkg.startsWith(prefix)) {
				JavaPackage pkg = pkgMap.computeIfAbsent(source.pkg, pkgName -> new JavaPackage(pkgName));
				JavaClass cls = clsMap.computeIfAbsent(name, clsName -> new JavaClass(clsName, pkg));
				pkg.getClasses().add(cls);
			}
		}

		/* Resolve class links (imports) */
		for (Map.Entry<String, Source> kv : sources.entrySet()) {
			String name = kv.getKey();
			Source source = kv.getValue();
			JavaClass cls = clsMap.get(name);
			for (String depName : source.deps) {
				JavaClass dep = clsMap.get(depName);
				if (dep != null) {
					cls.getImports().add(dep);
				}
			}
		}

		/* Copy-construct result set so the tree can be gc'd */
		return new HashSet<>(pkgMap.values());
	}

	public String getPrefix() {
		return prefix;
	}

	public Analyser setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

}
