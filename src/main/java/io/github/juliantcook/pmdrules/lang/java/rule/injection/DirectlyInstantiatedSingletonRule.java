package io.github.juliantcook.pmdrules.lang.java.rule.injection;

import net.sourceforge.pmd.lang.java.ast.ASTAllocationExpression;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.lang.annotation.Annotation;

public class DirectlyInstantiatedSingletonRule extends AbstractJavaRule {

    @Override
    public Object visit(ASTAllocationExpression node, Object data) {
        if (node.getType() == null) return data;
        for (Annotation annotation : node.getType().getAnnotations()) {
            if (annotation.toString().equals("@javax.inject.Singleton()")) {
                addViolation(data, node, new Object[]{node.getType().getSimpleName()});
            }
        }
        super.visit(node, data);
        return data;
    }
}
