package com.apixandru.java.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.types.ResolvedType;

public class DelegatingSymbolResolver implements SymbolResolver {

    private SymbolResolver sr;

    @Override
    public <T> T resolveDeclaration(Node node, Class<T> resultClass) {
        return sr.resolveDeclaration(node, resultClass);
    }

    @Override
    public <T> T toResolvedType(Type javaparserType, Class<T> resultClass) {
        return sr.toResolvedType(javaparserType, resultClass);
    }

    @Override
    public ResolvedType calculateType(Expression expression) {
        return sr.calculateType(expression);
    }

    public void setSr(SymbolResolver sr) {
        this.sr = sr;
    }

}
