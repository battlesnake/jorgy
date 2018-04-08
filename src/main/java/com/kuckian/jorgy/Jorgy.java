package com.kuckian.jorgy;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import com.kuckian.jorgy.Analyser.ParseFailedException;

public class Jorgy {

	private static final String[] styleSheetLines = new String[] {
			"node { fill-color: red; text-color: black; text-offset: 0, 16px; } ", "edge { fill-color: green; } ",
			"node.weak { fill-color: rgb(255,192,192); text-color: rgb(192,192,192); } ",
			"edge.weak { fill-color: rgb(192,255,192); }",
			"node.toggle { fill-color: rgb(128,128,255); text-color: rgb(64,64,192); } ",
			"edge.toggle { fill-color: rgb(192,192,255); }" };
	private static final String styleSheet = String.join("\n", styleSheetLines);
	
	private static final String prefix = "com.kuckian.";

	public static void main(String[] args) throws IOException, ParseFailedException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Arguments: <source-dir>");
		}
		final String sourceDir = args[0];
		Set<JavaPackage> graphModel = new Analyser().setPrefix(prefix).run(Paths.get(sourceDir));
		System.setProperty("sun.java2d.opengl", "True");
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		Graph graph;
		if (true) {
			graph = buildGraphByPackage(graphModel, prefix, new GraphStyle<JavaPackage>() {
				@Override
				public boolean isWeak(JavaPackage node) {
					return node.getImports().isEmpty();
				}

				@Override
				public boolean isVisible(JavaPackage node) {
					return true; //node.getName().startsWith(prefix);
				}

				@Override
				public float getNodeWeight(JavaPackage node) {
					return node.getImports().isEmpty()  ? 0.1f : 1.0f;
				}

				@Override
				public float getEdgeWeight(JavaPackage from, JavaPackage to) {
					return to.getImports().isEmpty() ? 1.35f : 1.0f;
				}
			});
		} else {
			graph = buildGraphByClass(graphModel, prefix, new GraphStyle<JavaClass>() {
				@Override
				public boolean isWeak(JavaClass node) {
					return node.getImports().isEmpty();
				}

				@Override
				public boolean isVisible(JavaClass node) {
					return true || !node.getPackage().getName().endsWith(".util");
				}

				@Override
				public float getNodeWeight(JavaClass node) {
					return node.getImports().isEmpty() ? 0.01f : 0.3f;
				}

				@Override
				public float getEdgeWeight(JavaClass from, JavaClass to) {
					return to.getImports().isEmpty() ? 1f : 8.0f;
				}
			});
		}
		showGraph(graph);
	}

	private static class Wrapper<T> {
		T value;

		public Wrapper(T value) {
			this.value = value;
		}
	}

	public static String longestCommonPrefix(List<String> strings) {
		if (strings.isEmpty()) {
			return "";
		}
		final String first = strings.get(0);
		final int scanLen = first.length();
		final int count = strings.size();
		for (int prefixLen = 0; prefixLen < scanLen; prefixLen++) {
			final char c = first.charAt(prefixLen);
			for (int i = 1; i < count; i++) {
				if (prefixLen >= strings.get(i).length() || strings.get(i).charAt(prefixLen) != c) {
					return strings.get(i).substring(0, prefixLen);
				}
			}
		}
		return first;
	}

	public static int getPrefixLen(Collection<JavaPackage> strings) {
		return longestCommonPrefix(strings.stream().map(x -> x.getName()).collect(Collectors.toList())).length();
	}

	public static interface GraphStyle<NodeType extends GraphNode> {
		boolean isWeak(NodeType node);

		boolean isVisible(NodeType node);

		float getNodeWeight(NodeType node);

		float getEdgeWeight(NodeType from, NodeType to);
	}

	public static void showGraph(Graph graph) {
		Viewer viewer = graph.display();
		ViewerPipe fromViewer = viewer.newViewerPipe();
		Wrapper<Boolean> close = new Wrapper<Boolean>(false);
		fromViewer.addViewerListener(new ViewerListener() {
			@Override
			public void viewClosed(String viewName) {
				close.value = true;
			}

			@Override
			public void buttonReleased(String id) {
			}

			@Override
			public void buttonPushed(String id) {
				Node node = graph.getNode(id);
				ClassList ncl = new ClassList(node.getAttribute("ui.class"));
				if (ncl.has("toggle")) {
					ncl.unset("toggle");
					node.getEachEdge().forEach(edge -> edge.setAttribute("ui.class",
							new ClassList(edge.getAttribute("ui.class")).unset("toggle").toString()));
				} else {
					ncl.set("toggle");
					node.getEachEdge().forEach(edge -> edge.setAttribute("ui.class",
							new ClassList(edge.getAttribute("ui.class")).set("toggle").toString()));
				}
				node.setAttribute("ui.class", ncl.toString());
			}
		});
		fromViewer.addSink(graph);
		while (!close.value) {
			fromViewer.pump();
		}
	}

	public static Graph buildGraphByClass(Set<JavaPackage> graph, String prefix, GraphStyle<JavaClass> style) {
		Graph graphView = new MultiGraph("Project dependencies");
		graphView.addAttribute("ui.quality");
		graphView.addAttribute("ui.antialias");
		graphView.addAttribute("ui.stylesheet", styleSheet);
		graphView.addAttribute("layout.quality", 4);
		graphView.addAttribute("layout.stabilization-limit", 1);
		Map<JavaClass, Node> nodes = new HashMap<>();
		Wrapper<Integer> edgeId = new Wrapper<>(1);
		graph.forEach(pkg -> {
			pkg.getClasses().forEach(cls -> {
				if (!style.isVisible(cls)) {
					return;
				}
				String name = pkg.getName();
				String displayName = name.startsWith(prefix) ? "@" + name.substring(prefix.length()) : name;
				Node node = graphView.addNode(cls.getName());
				nodes.put(cls, node);
				node.addAttribute("ui.label", displayName);
				node.addAttribute("layout.weight", style.getNodeWeight(cls));
				if (style.isWeak(cls)) {
					node.addAttribute("ui.class", "weak");
				}
			});
		});
		graph.forEach(pkg -> {
			System.out.println(pkg.getName());
			pkg.getClasses().forEach(cls -> {
				if (!style.isVisible(cls)) {
					return;
				}
				cls.getImports().forEach(dep -> {
					if (!style.isVisible(dep)) {
						return;
					}
					Edge edge = graphView.addEdge(Integer.toString(edgeId.value), nodes.get(cls), nodes.get(dep), true);
					edgeId.value++;
					edge.addAttribute("layout.weight", style.getEdgeWeight(cls, dep));
					if (style.isWeak(dep) || style.isWeak(cls)) {
						edge.addAttribute("ui.class", "weak");
					}
				});
			});
		});
		return graphView;
	}

	public static Graph buildGraphByPackage(Set<JavaPackage> graph, String prefix, GraphStyle<JavaPackage> style) {
		Graph graphView = new MultiGraph("Project dependencies");
		graphView.addAttribute("ui.quality");
		graphView.addAttribute("ui.antialias");
		graphView.addAttribute("ui.stylesheet", styleSheet);
		graphView.addAttribute("layout.quality", 4);
		Map<JavaPackage, Node> nodes = new HashMap<>();
		Map<Integer, Set<JavaClass>> edges = new HashMap<>();
		Wrapper<Integer> edgeId = new Wrapper<>(1);
		graph.forEach(pkg -> {
			if (!style.isVisible(pkg)) {
				return;
			}
			String name = pkg.getName();
			String displayName = name.startsWith(prefix) ? "@" + name.substring(prefix.length()) : name;
			Node node = graphView.addNode(pkg.getName());
			nodes.put(pkg, node);
			node.addAttribute("ui.label", displayName);
			node.addAttribute("layout.weight", style.getNodeWeight(pkg));
			if (style.isWeak(pkg)) {
				node.addAttribute("ui.class", "weak");
			}
		});
		graph.forEach(pkg -> {
			if (!style.isVisible(pkg)) {
				return;
			}
			pkg.getPackageDependencies().entrySet().forEach(kv -> {
				JavaPackage dependency = kv.getKey();
				if (!style.isVisible(dependency)) {
					return;
				}
				Set<JavaClass> dependants = kv.getValue();
				edges.put(edgeId.value, dependants);
				Edge edge = graphView.addEdge(Integer.toString(edgeId.value), nodes.get(pkg), nodes.get(dependency),
						true);
				edgeId.value++;
				edge.addAttribute("layout.weight", style.getEdgeWeight(pkg, dependency));
				if (style.isWeak(dependency) || style.isWeak(pkg)) {
					edge.addAttribute("ui.class", "weak");
				}
			});
		});
		return graphView;
	}

	/* Package -> member class -> imported class */
	public static void dumpGraphByClass(Set<JavaPackage> graph) {
		graph.forEach(pkg -> {
			System.out.println(pkg.getName());
			pkg.getClasses().forEach(cls -> {
				System.out.println("  " + cls.getName());
				cls.getImports().forEach(dep -> {
					System.out.println("    " + dep.getName());
				});
				System.out.println();
			});
			System.out.println();
		});
	}

	/* Package -> imported class <- member class */
	public static void dumpGraphByPackage(Set<JavaPackage> graph) {
		graph.forEach(pkg -> {
			System.out.println(pkg.getName());
			pkg.getPackageDependencies().entrySet().forEach(kv -> {
				JavaPackage dependency = kv.getKey();
				Set<JavaClass> dependants = kv.getValue();
				System.out.println("  " + dependency.getName());
				dependants.forEach(dependant -> {
					System.out.println("    " + dependant.getName());
				});
				System.out.println();
			});
			System.out.println();
		});
	}

}
