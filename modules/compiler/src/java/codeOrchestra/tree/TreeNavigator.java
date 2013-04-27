package codeOrchestra.tree;

import codeOrchestra.LiveCodingUtil;
import codeOrchestra.digest.Visibility;
import flex2.compiler.CompilationUnit;
import macromedia.asc.parser.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Anton.I.Neverov
 */
public class TreeNavigator {

    public static Visibility getVisibility(DefinitionNode definitionNode) {
        AttributeListNode attrs = definitionNode.attrs;
        if (attrs == null) {
            return Visibility.UNKNOWN;
        }

        if (hasAttribute("public", attrs)) {
            return Visibility.PUBLIC;
        } else if (hasAttribute("protected", attrs)) {
            return Visibility.PROTECTED;
        } else if (hasAttribute("private", attrs)) {
            return Visibility.PRIVATE;
        } else if (hasAttribute("internal", attrs)) {
            return Visibility.INTERNAL;
        }

        return Visibility.UNKNOWN;
    }

    public static ClassDefinitionNode getClassDefinition(CompilationUnit unit) {
        return getPackageClassDefinition(((ProgramNode) unit.getSyntaxTree()));
    }

    public static List<ClassDefinitionNode> getInternalClassDefinitions(ProgramNode programNode) {
        List<ClassDefinitionNode> result = new ArrayList<ClassDefinitionNode>();

        StatementListNode statements = programNode.statements;
        for (Node item : statements.items) {
            if (item instanceof ClassDefinitionNode) {
                ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) item;
                if (classDefinitionNode.pkgdef == null) {
                    result.add(classDefinitionNode);
                }
            }
        }

        return result;
    }

    public static ClassDefinitionNode getPackageClassDefinition(ProgramNode programNode) {
        StatementListNode statements = programNode.statements;
        for (Node item : statements.items) {
            if (item instanceof ClassDefinitionNode) {
                ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) item;
                if (classDefinitionNode.pkgdef != null) {
                    return classDefinitionNode;
                }
            }
        }
        return null;
    }

    public static List<FunctionDefinitionNode> getMethodDefinitions(ClassDefinitionNode classDefinitionNode) {
        List<FunctionDefinitionNode> functionDefinitionNodes = getAllMethodDefinitions(classDefinitionNode);
        Iterator<FunctionDefinitionNode> iterator = functionDefinitionNodes.iterator();
        while (iterator.hasNext()) {
            FunctionDefinitionNode functionDefinitionNode = iterator.next();
            if (isConstructor(functionDefinitionNode, classDefinitionNode)) {
                iterator.remove();
            }
        }
        return functionDefinitionNodes;
    }

    public static FunctionDefinitionNode getConstructorDefinition(ClassDefinitionNode classDefinitionNode) {
        for (FunctionDefinitionNode functionDefinitionNode : getAllMethodDefinitions(classDefinitionNode)) {
            if (isConstructor(functionDefinitionNode, classDefinitionNode)) {
                return functionDefinitionNode;
            }
        }
        return null;
    }

    public static List<VariableDefinitionNode> getFieldDefinitions(ClassDefinitionNode classDefinitionNode) {
        List<VariableDefinitionNode> variableDefinitionNodes = new ArrayList<VariableDefinitionNode>();
        for (Node item : classDefinitionNode.statements.items) {
            if (item instanceof VariableDefinitionNode) {
                variableDefinitionNodes.add((VariableDefinitionNode) item);
            }
        }
        return variableDefinitionNodes;
    }

    public static List<VariableDefinitionNode> getFieldDefinitionsWithAnnotation(ClassDefinitionNode classDefinitionNode, String annotation) {
        List<VariableDefinitionNode> variableDefinitionNodes = new ArrayList<VariableDefinitionNode>();
        for (Node item : classDefinitionNode.statements.items) {
            if (item instanceof VariableDefinitionNode && LiveCodingUtil.hasAnnotation((DefinitionNode) item, annotation)) {
                variableDefinitionNodes.add((VariableDefinitionNode) item);
            }
        }
        return variableDefinitionNodes;
    }

    public static List<FunctionDefinitionNode> getMethodDefinitionsWithAnnotation(ClassDefinitionNode classDefinitionNode, String annotation) {
        List<FunctionDefinitionNode> functionDefinitionNodes = new ArrayList<FunctionDefinitionNode>();
        for (Node item : classDefinitionNode.statements.items) {
            if (item instanceof FunctionDefinitionNode && LiveCodingUtil.hasAnnotation((DefinitionNode) item, annotation)) {
                functionDefinitionNodes.add((FunctionDefinitionNode) item);
            }
        }
        return functionDefinitionNodes;
    }

    static List<FunctionDefinitionNode> getAllMethodDefinitions(ClassDefinitionNode classDefinitionNode) {
        List<FunctionDefinitionNode> functionDefinitionNodes = new ArrayList<FunctionDefinitionNode>();
        for (Node item : classDefinitionNode.statements.items) {
            if (item instanceof FunctionDefinitionNode) {
                functionDefinitionNodes.add((FunctionDefinitionNode) item);
            }
        }
        return functionDefinitionNodes;
    }

    public static boolean isConstructor(FunctionDefinitionNode functionDefinitionNode, ClassDefinitionNode classDefinitionNode) {
        return functionDefinitionNode.name.identifier.name.equals(classDefinitionNode.name.name);
    }

    public static List<ImportDirectiveNode> getImports(PackageDefinitionNode packageDefinitionNode) {
        List<ImportDirectiveNode> importDirectiveNodes = new ArrayList<ImportDirectiveNode>();
        for (Node item : packageDefinitionNode.statements.items) {
            if (item instanceof ImportDirectiveNode) {
                importDirectiveNodes.add((ImportDirectiveNode) item);
            }
        }
        return importDirectiveNodes;
    }

    public static List<ImportDirectiveNode> getImports(ProgramNode programNode) {
        List<ImportDirectiveNode> importDirectiveNodes = new ArrayList<ImportDirectiveNode>();
        for (Node item : programNode.statements.items) {
            if (item instanceof ImportDirectiveNode) {
                importDirectiveNodes.add((ImportDirectiveNode) item);
            }
        }
        return importDirectiveNodes;
    }

    public static boolean isOverridingMethod(FunctionDefinitionNode functionDefinitionNode) {
        return methodHasAttribute(functionDefinitionNode, "override");
    }

    public static boolean isProtectedField(VariableDefinitionNode variableDefinitionNode) {
        return fieldHasAttribute(variableDefinitionNode, "protected");
    }

    public static boolean isProtectedMethod(FunctionDefinitionNode functionDefinitionNode) {
        return methodHasAttribute(functionDefinitionNode, "protected");
    }

    public static boolean isStaticMethod(FunctionDefinitionNode functionDefinitionNode) {
        return methodHasAttribute(functionDefinitionNode, "static");
    }

    public static boolean isStaticField(VariableBindingNode variableBindingNode) {
        return fieldHasAttribute(variableBindingNode, "static");
    }

    private static boolean methodHasAttribute(FunctionDefinitionNode functionDefinitionNode, String attrName) {
        AttributeListNode attributeListNode = functionDefinitionNode.attrs;
        return hasAttribute(attrName, attributeListNode);
    }

    private static boolean fieldHasAttribute(VariableBindingNode variableBindingNode, String attrName) {
        AttributeListNode attributeListNode = variableBindingNode.attrs;
        return hasAttribute(attrName, attributeListNode);
    }

    private static boolean fieldHasAttribute(VariableDefinitionNode variableDefinitionNode, String attrName) {
        AttributeListNode attributeListNode = variableDefinitionNode.attrs;
        return hasAttribute(attrName, attributeListNode);
    }

    private static boolean hasAttribute(String attrName, AttributeListNode attributeListNode) {
        if (attributeListNode == null) {
            return false;
        }
        for (Node item : attributeListNode.items) {
            Node node = null;
            if (item instanceof ListNode) {
                node = ((ListNode) item).items.at(0);
            } else if (item instanceof MemberExpressionNode) {
                node = item;
            } else if (item instanceof IdentifierNode) {
                if (((IdentifierNode) item).name.equals(attrName)) {
                    return true;
                }
            } else {
                continue;
            }
            if (!(node instanceof MemberExpressionNode)) {
                continue;
            }
            SelectorNode selector = ((MemberExpressionNode) node).selector;
            if (!(selector instanceof GetExpressionNode)) {
                continue;
            }
            Node expr = selector.expr;
            if (!(expr instanceof IdentifierNode)) {
                continue;
            }
            if (((IdentifierNode) expr).name.equals(attrName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGetter(FunctionDefinitionNode functionDefinitionNode) {
        return functionDefinitionNode.name.kind == Tokens.GET_TOKEN;
    }

    public static boolean isSetter(FunctionDefinitionNode functionDefinitionNode) {
        return functionDefinitionNode.name.kind == Tokens.SET_TOKEN;
    }

}
