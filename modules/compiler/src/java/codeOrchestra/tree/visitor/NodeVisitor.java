package codeOrchestra.tree.visitor;

import codeOrchestra.tree.processor.CollectingProcessor;
import codeOrchestra.tree.processor.INodeProcessor;
import com.sun.istack.internal.NotNull;
import macromedia.asc.parser.Node;
import macromedia.asc.parser.PackageDefinitionNode;
import macromedia.asc.parser.ProgramNode;
import macromedia.asc.semantics.MetaData;
import macromedia.asc.semantics.ObjectValue;
import macromedia.asc.semantics.ReferenceValue;
import macromedia.asc.semantics.TypeInfo;
import macromedia.asc.util.NumberConstant;
import macromedia.asc.util.NumberUsage;

import java.util.*;

/**
 * @author Anton.I.Neverov
 */
public abstract class NodeVisitor<N extends Node> {

    private static Map<Node, Integer> visitedNodes = new HashMap<Node, Integer>();
    private static boolean testMode = true;

    /**
     * This is implemented only for comparing function bodies!
     */
    public boolean compareTrees(N left, N right) {
        if (testMode) {
            checkInfiniteRecursion(left);
        }

        List<Node> leftChildren = getChildren(left);
        List<Node> rightChildren = getChildren(right);
        List<Object> leftLeaves = getLeaves(left);
        List<Object> rightLeaves = getLeaves(right);

        if (leftChildren == null || rightChildren == null || leftLeaves == null || rightLeaves == null) {
            throw new RuntimeException();
        }

        if (leftLeaves.size() != rightLeaves.size()) {
            return false;
        }
        if (leftChildren.size() != rightChildren.size()) {
            return false;
        }
        for (int i = 0; i < leftLeaves.size(); i++) {
            if (!compareObjects(leftLeaves.get(i), rightLeaves.get(i))) {
                return false;
            }
        }
        for (int i = 0; i < leftChildren.size(); i++) {
            Node leftChild = leftChildren.get(i);
            Node rightChild = rightChildren.get(i);
            if (leftChild == null && rightChild == null) {
                continue;
            }
            if (leftChild == null || rightChild == null) {
                return false;
            }
            if (leftChild.getClass() != rightChild.getClass()) {
                return false;
            }
            NodeVisitor childVisitor = NodeVisitorFactory.getVisitor(leftChild.getClass());
            if (!childVisitor.compareTrees(leftChild, rightChild)) {
                return false;
            }
        }

        if (testMode) {
            visitedNodes.clear();
        }

        return true;
    }

    public static void applyToTree(Node treeRoot, INodeProcessor nodeProcessor) {
        Queue<Node> nodesToProcess = new LinkedList<Node>();
        if (treeRoot instanceof ProgramNode) {
            treeRoot = ((ProgramNode) treeRoot).pkgdefs.get(0);
        }
        nodesToProcess.add(treeRoot);

        Node node;
        while ((node = nodesToProcess.poll()) != null) {
            if (testMode) {
                checkInfiniteRecursion(node);
            }
            nodeProcessor.process(node);
            NodeVisitor visitor = NodeVisitorFactory.getVisitor(node.getClass());
            List<Node> children = visitor.getChildren(node);
            if (children == null) {
                throw new RuntimeException();
            }
            for (Node child : children) {
                if (child != null && !(child instanceof PackageDefinitionNode)) { // PackageDefinitionNode contains itself in its statements
                    nodesToProcess.add(child);
                }
            }
        }

        if (testMode) {
            visitedNodes.clear();
        }
    }

    private static void checkInfiniteRecursion(Node node) {
        if (visitedNodes.containsKey(node)) {
            visitedNodes.put(node, visitedNodes.get(node) + 1);
        } else {
            visitedNodes.put(node, 0);
        }
        if (visitedNodes.get(node) > 30) {
            throw new RuntimeException();
        }
    }

    public static List<Node> getDescendants(Node treeRoot) {
        return getDescendants(treeRoot, Collections.<Class>emptySet());
    }

    public static List<Node> getDescendants(Node treeRoot, Class nodeClass) {
        return getDescendants(treeRoot, Collections.singleton(nodeClass));
    }

    public static List<Node> getDescendants(Node treeRoot, Set<Class> nodeClasses) {
        CollectingProcessor collectingProcessor = new CollectingProcessor(nodeClasses);
        applyToTree(treeRoot, collectingProcessor);
        return collectingProcessor.getNodes();
    }

    // TODO: It does not return children that are known to be null right after parse1
    protected abstract List<Node> getChildren(N node);

    // TODO: It does not return leaves for nodes higher in tree than FunctionDefinitionNode
    protected abstract List<Object> getLeaves(N node);

    protected boolean compareObjects(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Node || right instanceof Node) {
            throw new RuntimeException();
        }
        if (left.getClass() != right.getClass()) {
            return false;
        }
        if (left instanceof NumberUsage) {
            return compareNumberUsages(((NumberUsage) left), (NumberUsage) right);
        }
        if (left instanceof NumberConstant) {
            return compareNumberConstants(((NumberConstant) left), (NumberConstant) right);
        }
        if (left instanceof TypeInfo) {
            return compareTypeInfos(((TypeInfo) left), (TypeInfo) right);
        }
        if (left instanceof ObjectValue) {
            return compareObjectValues(((ObjectValue) left), (ObjectValue) right);
        }
        if (left instanceof ReferenceValue) {
            return compareReferenceValues(((ReferenceValue) left), (ReferenceValue) right);
        }
        if (left instanceof MetaData) {
            return compareMetaDatas(((MetaData) left), (MetaData) right);
        }
        return left.equals(right);
    }

    private boolean compareNumberUsages(NumberUsage left, NumberUsage right) {
        if (left.get_usage() != right.get_usage()) {
            return false;
        }
        if (left.get_rounding() != right.get_rounding()) {
            return false;
        }
        if (left.get_precision() != right.get_precision()) {
            return false;
        }
        if (left.get_floating_usage() != right.get_floating_usage()) {
            return false;
        }
        return true;
    }

    private boolean compareTypeInfos(TypeInfo left, TypeInfo right) {
        if (left.isNullable() != right.isNullable()) {
            return false;
        }
        if (!compareObjects(left.getTypeValue(), right.getTypeValue())) {
            return false;
        }
        if (!compareObjects(left.getPrototype(), right.getPrototype())) {
            return false;
        }
        if (!compareObjects(left.getName(), right.getName())) {
            return false;
        }
        return true;
    }

    private boolean compareObjectValues(ObjectValue left, ObjectValue right) {
        return left.compareTo(right) == 0; // TODO: check this
    }

    private boolean compareReferenceValues(ReferenceValue left, ReferenceValue right) {
        if (!compareObjects(left.getBase(), right.getBase())) {
            return false;
        }
        if (!compareObjects(left.getType(), right.getType())) {
            return false;
        }
        if (!compareObjects(left.name, right.name)) {
            return false;
        }
        // TODO: Other fields?
        return true;
    }

    private boolean compareNumberConstants(NumberConstant left, NumberConstant right) {
        return left.doubleValue() == right.doubleValue();
    }

    private boolean compareMetaDatas(MetaData left, MetaData right) {
        if (!compareObjects(left.id, right.id)) {
            return false;
        }
        if (left.values == null && right.values == null) {
            return true;
        }
        if (left.values == null || right.values == null) {
            return false;
        }
        if (left.values.length != right.values.length) {
            return false;
        }
        for (int i = 0; i < left.values.length; i++) {
            if (!compareObjects(left.values[i], right.values[i])) {
                return false;
            }
        }
        return true;
    }
}
