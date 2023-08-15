package ru.pilot.impact.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import ru.pilot.impact.dto.ClassDto;
import ru.pilot.impact.dto.LinesDto;
import ru.pilot.impact.dto.MethodDto;

/**
 * Разбор java класса
 */
public class ClassParser {

    /**
     * Разбор java класса
     *
     * @param classText текст класса
     * @return список методов класса с диапазоном строк
     * todo подключить поля
     */
    public List<ClassDto> extractMethods(String classText) {
        // init AST - abstract syntax trees
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(classText.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // class package
        String packageName = cu.getPackage().getName().getFullyQualifiedName();

        List<ClassDto> classDtoList = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            private ClassDto classDto;

            /** Visit new java type */
            public boolean visit(TypeDeclaration node) {
                if (node.isInterface()) {
                    // skip interface
                    return true;
                }

                // create class name 
                String className = node.getName().getFullyQualifiedName();
                classDto = new ClassDto();
                classDto.setClassName(packageName + "." + className);
                classDtoList.add(classDto);

                return true;
            }

            /** Visit new java method */
            public boolean visit(MethodDeclaration node) {
                Block body = node.getBody();
                if (body == null) {
                    // skip abstract method
                    return true;
                }
                // start and end line method 
                Integer startLineNum = getStartLineNumber(cu, body);
                Integer endLineNum = getEndLineNumber(cu, body);

                MethodDto methodDto = new MethodDto();
                methodDto.setMethodName(node.getName() + "");
                methodDto.setClassName(classDto.getClassName());
                methodDto.setParams(getParameters(node));
                methodDto.setLines(new LinesDto(startLineNum, endLineNum));
                classDto.addMethod(methodDto);

                return true;
            }
        });

        return classDtoList.stream()
                .filter(cl -> !cl.getMethods().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Get type method params
     */
    private static List<String> getParameters(MethodDeclaration node) {
        List<String> parameters = new ArrayList<>();
        for (Object parameter : node.parameters()) {
            VariableDeclaration variableDeclaration = (VariableDeclaration) parameter;
            String type = variableDeclaration.getStructuralProperty(SingleVariableDeclaration.TYPE_PROPERTY).toString();
            for (int i = 0; i < variableDeclaration.getExtraDimensions(); i++) {
                // todo
                type += "[]";
            }
            parameters.add(type);
        }
        return parameters;
    }

    private static Integer getStartLineNumber(CompilationUnit cu, ASTNode node) {
        return cu.getLineNumber(node.getStartPosition());
    }

    private static Integer getEndLineNumber(CompilationUnit cu, ASTNode node) {
        return cu.getLineNumber(node.getStartPosition() + node.getLength());
    }
}
