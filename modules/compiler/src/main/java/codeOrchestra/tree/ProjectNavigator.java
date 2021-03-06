package codeOrchestra.tree;

import codeOrchestra.LiveCodingPolicy;
import codeOrchestra.LiveCodingUtil;
import codeOrchestra.util.Pair;
import macromedia.asc.parser.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anton.I.Neverov
 * @author Alexander Eliseyev
 */
public class ProjectNavigator {

    private final Map<String, ProgramNode> loadedSyntaxTrees = new HashMap<>();
    public Map<String, Set<String>> modelDependencies;

    // package -> list of pairs (original class name, live short class name)
    private Map<String, List<Pair<String, String>>> auxLivecodingClasses = new HashMap<>();

    public void add(String fqName, ProgramNode node) {
        loadedSyntaxTrees.put(fqName, node);
    }

    public void addAdditionalLivecodingClass(String packageName, String originalClassName, String liveShortClassName) {
        List<Pair<String, String>> shortClassNames = auxLivecodingClasses.get(packageName);
        if (shortClassNames == null) {
            shortClassNames = new ArrayList<>();
            auxLivecodingClasses.put(packageName, shortClassNames);
        }

        Pair<String, String> pair = new Pair<>(originalClassName, liveShortClassName);
        if (!shortClassNames.contains(pair)) {
            shortClassNames.add(pair);
        }
    }

    public List<Pair<String, String>> getAdditionalLivecodingClasses(String packageName) {
        List<Pair<String, String>> shortClassNames = auxLivecodingClasses.get(packageName);
        if (shortClassNames == null) {
            return Collections.emptyList();
        }
        return shortClassNames;
    }

    public Set<String> getLiveCodingClassNames(String packageName) {
        // TODO: At this step livecoding classes are not created yet, so we collect names of methods assuming that every one of them will be extracted to some class
        HashSet<String> result = new HashSet<>();

        for (ProgramNode syntaxTree : loadedSyntaxTrees.values()) {
            ClassDefinitionNode classDefinition = TreeNavigator.getPackageClassDefinition(syntaxTree);
            if (!classDefinition.pkgdef.name.id.pkg_part.equals(packageName)) {
                continue;
            }
            LiveCodingPolicy liveCodingPolicy = LiveCodingUtil.getLiveCodingPolicy(classDefinition);
            if (!liveCodingPolicy.isEnabled()) {
                continue;
            }
            for (FunctionDefinitionNode functionDefinitionNode : TreeNavigator.getMethodDefinitions(classDefinition)) {
                if (!LiveCodingUtil.canBeUsedForLiveCoding(functionDefinitionNode, liveCodingPolicy)) {
                    continue;
                }
                result.add(LiveCodingUtil.constructLiveCodingClassName(functionDefinitionNode, classDefinition.name.name));
            }
        }

        result.addAll(getAdditionalLivecodingClasses(packageName).stream().map(Pair::getO2).collect(Collectors.toList()));

        return result;
    }

    public Set<String> getLiveCodingClassNames(String packageName, String className) {
        // TODO: At this step livecoding classes are not created yet, so we collect names of methods assuming that every one of them will be extracted to some class
        HashSet<String> result = new HashSet<>();
        for (ProgramNode syntaxTree : loadedSyntaxTrees.values()) {
            ClassDefinitionNode classDefinition = TreeNavigator.getPackageClassDefinition(syntaxTree);
            if (!classDefinition.pkgdef.name.id.pkg_part.equals(packageName)) {
                continue;
            }
            if (!classDefinition.name.name.equals(className)) {
                continue;
            }
            LiveCodingPolicy liveCodingPolicy = LiveCodingUtil.getLiveCodingPolicy(classDefinition);
            if (!liveCodingPolicy.isEnabled()) {
                continue;
            }
            for (FunctionDefinitionNode functionDefinitionNode : TreeNavigator.getMethodDefinitions(classDefinition)) {
                if (!LiveCodingUtil.canBeUsedForLiveCoding(functionDefinitionNode, liveCodingPolicy)) {
                    continue;
                }
                result.add(LiveCodingUtil.constructLiveCodingClassName(functionDefinitionNode, classDefinition.name.name));
            }
        }

        for (Pair<String, String> pair : getAdditionalLivecodingClasses(packageName)) {
            String originalClassName = pair.getO1();
            if (className.equals(originalClassName)) {
                result.add(pair.getO2());
            }
        }

        return result;
    }

    public Set<String> getClassNames(String packageName) {
        HashSet<String> result = new HashSet<>();
        for (ProgramNode syntaxTree : loadedSyntaxTrees.values()) {
            ClassDefinitionNode classDefinition = TreeNavigator.getPackageClassDefinition(syntaxTree);
            if (!classDefinition.pkgdef.name.id.pkg_part.equals(packageName)) {
                continue;
            }
            result.add(classDefinition.name.name);
        }
        return result;
    }

    public Set<String> getModelDependencies(String packageName) {
        if (modelDependencies == null) {
            modelDependencies = new HashMap<>();

            // Fill user packages
            for (ProgramNode programNode : loadedSyntaxTrees.values()) {
                ClassDefinitionNode classDefinition = TreeNavigator.getPackageClassDefinition(programNode);
                String name = classDefinition.pkgdef.name.id.pkg_part;
                modelDependencies.put(name, new HashSet<String>() {
                    @Override
                    public boolean add(String s) {
                        return modelDependencies.containsKey(s) && super.add(s);
                    }
                });
            }

            // Fill filtered dependencies
            for (ProgramNode programNode : loadedSyntaxTrees.values()) {
                ClassDefinitionNode classDefinition = TreeNavigator.getPackageClassDefinition(programNode);
                PackageDefinitionNode pkgdef = classDefinition.pkgdef;
                String name = pkgdef.name.id.pkg_part;

                Set<String> deps = modelDependencies.get(name);

                deps.addAll(TreeNavigator.getImports(pkgdef).stream().map(importDirectiveNode -> importDirectiveNode.name.id.pkg_part).collect(Collectors.toList()));
            }
        }

        return modelDependencies.get(packageName);
    }

    public ProgramNode getSyntaxTree(String packageName, String className) {
        for (ProgramNode programNode : loadedSyntaxTrees.values()) {
            ClassDefinitionNode classDefinition = TreeNavigator.getPackageClassDefinition(programNode);
            if (classDefinition.pkgdef.name.id.pkg_part.equals(packageName) && classDefinition.name.name.equals(className)) {
                return programNode;
            }
        }
        return null;
    }

}
