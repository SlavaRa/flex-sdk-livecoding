package codeOrchestra.digest;

import codeOrchestra.tree.TreeNavigator;
import codeOrchestra.util.StringUtils;
import macromedia.asc.parser.*;

import java.util.*;

/**
 * @author Alexander Eliseyev
 */
public class SourceClassDigest implements IClassDigest {

    private String name;
    private String packageName;
    private String superClassShortName;

    private List<String> asterixImports = new ArrayList<String>();
    // Short name -> FQ name
    private Map<String, String> importMap = new HashMap<String, String>();

    private String superClassFQName;

    private Set<String> membersList = new HashSet<String>();

    public SourceClassDigest(ClassDefinitionNode cl) {
        // Name
        packageName = cl.pkgdef.name.id.pkg_part;
        name = cl.name.name;

        // Superclass short name
        // TODO: definition by fq-name?
        if (cl.baseclass != null && cl.baseclass instanceof MemberExpressionNode) {
            MemberExpressionNode memberExpressionNode = (MemberExpressionNode) cl.baseclass;
            SelectorNode selector = memberExpressionNode.selector;

            if (selector.expr != null && selector.expr instanceof IdentifierNode) {
                IdentifierNode identifierNode = (IdentifierNode) selector.expr;
                superClassShortName = identifierNode.name;
            }
        }

        // Imports
        List<ImportDirectiveNode> imports = TreeNavigator.getImports(cl.pkgdef);
        for (ImportDirectiveNode importDirectiveNode : imports) {
            PackageIdentifiersNode packageIdentifiersNode = importDirectiveNode.name.id;
            String importedPackage = packageIdentifiersNode.pkg_part;
            String importedShortName = packageIdentifiersNode.def_part;

            if (StringUtils.isEmpty(importedShortName)) {
                asterixImports.add(importedPackage);
            } else {
                importMap.put(importedShortName, StringUtils.longNameFromNamespaceAndShortName(importedPackage, importedShortName));
            }
        }

        // Members
        // TODO: implement
    }

    public Set<String> getMembersList() {
        return membersList;
    }

    public String getSuperClassFQName() {
        return superClassFQName;
    }

    public void resolve() {
        if (superClassShortName == null) {
            return;
        }

        // Try explicit imports
        String fqNameCandidate = importMap.get(superClassShortName);
        if (fqNameCandidate != null) {
            superClassFQName = fqNameCandidate;
            return;
        }

        // Try asterix imports
        for (String asterixPackage : asterixImports) {
            fqNameCandidate = tryResolve(asterixPackage, superClassShortName);
            if (fqNameCandidate != null) {
                superClassFQName = fqNameCandidate;
                return;
            }
        }

        // Try same package
        fqNameCandidate = tryResolve(packageName, superClassShortName);
        if (fqNameCandidate != null) {
            superClassFQName = fqNameCandidate;
            return;
        }

        // Try default package
        fqNameCandidate = tryResolve("", superClassShortName);
        if (fqNameCandidate != null) {
            superClassFQName = fqNameCandidate;
            return;
        }
    }

    /**
     * @return fq name if resolved, null otherwise
     */
    private String tryResolve(String packageName, String className) {
        String fqName = StringUtils.longNameFromNamespaceAndShortName(packageName, className);
        if (DigestManager.getInstance().isAvailable(fqName)) {
            return fqName;
        }
        return null;
    }

    public String getFqName() {
        return StringUtils.longNameFromNamespaceAndShortName(packageName, name);
    }

}