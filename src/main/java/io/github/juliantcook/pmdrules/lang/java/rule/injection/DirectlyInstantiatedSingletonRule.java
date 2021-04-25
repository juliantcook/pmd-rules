package io.github.juliantcook.pmdrules.lang.java.rule.injection;

import net.sourceforge.pmd.lang.java.ast.ASTAllocationExpression;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;

public class DirectlyInstantiatedSingletonRule extends AbstractJavaRule {

    @Override
    public Object visit(ASTAllocationExpression node, Object data) {
        for (Annotation annotation : node.getType().getAnnotations()) {
            if (annotation instanceof Singleton) {
                addViolation(data, node, new Object[]{});
            }
        }
        super.visit(node, data);
        return data;
    }
}
