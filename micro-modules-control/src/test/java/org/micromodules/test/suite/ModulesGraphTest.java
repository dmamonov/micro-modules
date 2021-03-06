package org.micromodules.test.suite;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.micromodules.control.graph.GraphDomain.Node;
import org.micromodules.control.graph.GraphDomain.NodeEdge;
import org.micromodules.control.graph.ModulesGraph;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModulesSpecification;
import org.micromodules.test.project.standalone.Standalone1ContractByName;
import org.micromodules.test.project.standalone.Standalone2;
import org.micromodules.test.project.standalone.__module__;

import static org.micromodules.control.graph.GraphDomain.EdgeType.*;
import static org.micromodules.control.graph.GraphDomain.Node.classNode;
import static org.micromodules.control.graph.GraphDomain.NodeType.CodeNode;
import static org.micromodules.control.graph.GraphDomain.NodeType.ModuleNode;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-30 7:30 PM
 */
public class ModulesGraphTest extends Assert {
    private final ModulesGraph modulesGraph;
    private final Node entireApplication = classNode(org.micromodules.test.project.__module__.EntireApplication.class);

    private final Node standaloneLayer = classNode(org.micromodules.test.project.__module__.StandaloneLayer.class);
    private final Node standalone1Module = classNode(__module__.Standalone1Module.class);
    private final Node standalone2Module = classNode(__module__.Standalone2Module.class);

    private final Node commonLayer = classNode(org.micromodules.test.project.__module__.CommonLayer.class);
    private final Node common1Module = classNode(org.micromodules.test.project.common.__module__.Common1Module.class);

    private final Node businessLayer = classNode(org.micromodules.test.project.__module__.BusinessLayer.class);
    private final Node business1Module = classNode(org.micromodules.test.project.business.__module__.Business1Module.class);
    private final Node business2Module = classNode(org.micromodules.test.project.business.__module__.Business2Module.class);

    private final Node uiLayer = classNode(org.micromodules.test.project.__module__.UiLayer.class);
    private final Node ui1Module = classNode(org.micromodules.test.project.ui.__module__.Ui1Module.class);
    private final Node ui2Module = classNode(org.micromodules.test.project.ui.__module__.Ui2Module.class);
    private final Node ui3Module = classNode(org.micromodules.test.project.ui.__module__.Ui3Module.class);

    public ModulesGraphTest() {
        final ClasspathRelations classpathRelations = ClasspathRelations.createFrom(Thread.currentThread().getContextClassLoader(), "org.micromodules.test.project");
        final ModulesSpecification modulesSpecification = ModulesSpecification.createFrom(classpathRelations);
        this.modulesGraph = ModulesGraph.createFrom(classpathRelations, modulesSpecification);
    }

    private void assertEqualsSet(final ImmutableSet<Node> expected, final ImmutableSet<Node> actual) {
        if (!expected.equals(actual)){
            throw new IllegalStateException(String.format("Sets assertion failed:\n" +
                    "expected: %s\n" +
                    "  actual: %s\n" +
                    "missing: %s\n" +
                    "  extra:  %s\n",
                    Node.nodesToString(expected),
                    Node.nodesToString(actual),
                    Node.nodesToString(Sets.difference(expected, actual)),
                    Node.nodesToString(Sets.difference(actual, expected))
            ));
        }
    }

    private void checkDirectRelation(final Node from, final Predicate<NodeEdge> by, final Predicate<Node> to, final ImmutableSet<Node> expectedSet) {
        final ImmutableSet<Node> actualSet = modulesGraph.query()
                .from(from)
                .forward().by(by)
                .to(to)
                .single().useFinish().set();
        assertEqualsSet(expectedSet, actualSet);
    }

    private void checkRecursiveRelation(final Node from, final Predicate<NodeEdge> by, final Predicate<Node> to, final ImmutableSet<Node> expectedSet) {
        final ImmutableSet<Node> actualSet = modulesGraph.query()
                .from(from)
                .forward().by(by)
                .to(to)
                .recursive().usePath().useFinish().set();
        assertEqualsSet(expectedSet, actualSet);
    }

    @Test
    public void testActualDependencyBetweenClasses() throws Exception {
        checkDirectRelation(classNode(Standalone2.class), UsesClass, CodeNode, ImmutableSet.of(classNode(Standalone1ContractByName.class)));
    }

    @Test
    public void testActualDependencyBetweenModulesExists() throws Exception {
        checkDirectRelation(standalone1Module, Dependency, ModuleNode, ImmutableSet.of());


    }

    @Test
    public void testNoDependencyBetweenModules() throws Exception {
        checkDirectRelation(standalone2Module, Dependency, ModuleNode, ImmutableSet.of(standalone1Module));
    }

    @Test
    public void testDirectSubModulesHierarchy() throws Exception {
        checkDirectRelation(standaloneLayer, SubModule, ModuleNode, ImmutableSet.of(
                standalone1Module,
                standalone2Module
        ));
        checkDirectRelation(entireApplication, SubModule, ModuleNode, ImmutableSet.of(
                standaloneLayer,
                commonLayer,
                businessLayer,
                uiLayer
        ));
    }

    @Test
    public void testSubModulesHierarchy() throws Exception {
        checkRecursiveRelation(entireApplication, SubModule, ModuleNode, ImmutableSet.of(
                standaloneLayer, standalone1Module, standalone2Module,
                commonLayer, common1Module,
                businessLayer, business1Module, business2Module,
                uiLayer, ui1Module, ui2Module, ui3Module
        ));
    }

    @Test
    public void testGrantedDependency() throws Exception {
        checkDirectRelation(standaloneLayer, Granted, ModuleNode, ImmutableSet.of());

        checkDirectRelation(commonLayer, Granted, ModuleNode, ImmutableSet.of(standaloneLayer, commonLayer));
        checkDirectRelation(common1Module, Granted, standalone1Module, ImmutableSet.of());
    }

    @Test
    public void testAllowedDependency() throws Exception {
        checkDirectRelation(common1Module, Dependency.and(Allowed), ModuleNode, ImmutableSet.of(
                standalone1Module
        ));
    }

    @Test
    public void testNotAllowedDependency() throws Exception {
        checkDirectRelation(standalone2Module, Dependency.and(NotAllowed), ModuleNode, ImmutableSet.of(standalone1Module));
    }
}
