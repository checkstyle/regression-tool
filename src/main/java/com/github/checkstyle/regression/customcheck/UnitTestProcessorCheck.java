////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle.regression.customcheck;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.checkstyle.regression.data.ImmutableProperty;
import com.github.checkstyle.regression.data.ModuleInfo;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * The custom check which processes the unit test class of a checkstyle module and grab the
 * possible properties that could be used for generating config.
 *
 * <p>The check would walk through the {@code @Test} annotation, find variable definition
 * like {@code final DefaultConfiguration checkConfig = createModuleConfig(FooCheck.class)}
 * and grab the property info from {@link DefaultConfiguration#addAttribute(String, String)}
 * method call.</p>
 *
 * <p>The check also support to detect module config which defined as a class field. This kind
 * of config is detected by the assignment in the unit test method, like {@code checkConfig =
 * createModuleConfig(FooCheck.class)}, where {@code checkConfig} could be a class field.</p>
 * @author LuoLiangchen
 */
public class UnitTestProcessorCheck extends AbstractCheck {
    /** The map of unit test method name to properties. */
    private static final Map<String, Set<ModuleInfo.Property>> UNIT_TEST_TO_PROPERTIES =
            new LinkedHashMap<>();

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.ANNOTATION,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.ANNOTATION,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {
            TokenTypes.ANNOTATION,
        };
    }

    @Override
    public void visitToken(DetailAST ast) {
        if ("Test".equals(ast.findFirstToken(TokenTypes.IDENT).getText())) {
            final DetailAST methodDef = ast.getParent().getParent();
            final DetailAST methodBlock = methodDef.findFirstToken(TokenTypes.SLIST);
            final Optional<String> configVariableName =
                    getModuleConfigVariableName(methodBlock);
            if (configVariableName.isPresent()) {
                final Set<ModuleInfo.Property> properties = new LinkedHashSet<>();

                for (DetailAST expr : getAllChildrenWithToken(methodBlock, TokenTypes.EXPR)) {
                    if (isAddAttributeMethodCall(expr.getFirstChild(), configVariableName.get())) {
                        final DetailAST elist =
                                expr.getFirstChild().findFirstToken(TokenTypes.ELIST);
                        final String key =
                                convertExpressionToText(elist.getFirstChild().getFirstChild());
                        final String value =
                                convertExpressionToText(elist.getLastChild().getFirstChild());
                        properties.add(ImmutableProperty.builder().name(key).value(value).build());
                    }
                }

                if (!UNIT_TEST_TO_PROPERTIES.containsValue(properties)) {
                    final String methodName = methodDef.findFirstToken(TokenTypes.IDENT).getText();
                    UNIT_TEST_TO_PROPERTIES.put(methodName, properties);
                }
            }
        }
    }

    /**
     * Clears the map of unit test method name to properties.
     */
    public static void clearUnitTestToPropertiesMap() {
        UNIT_TEST_TO_PROPERTIES.clear();
    }

    /**
     * Gets the map of unit test method name to properties.
     * @return the map of unit test method name to properties
     */
    public static Map<String, Set<ModuleInfo.Property>> getUnitTestToPropertiesMap() {
        return Collections.unmodifiableMap(UNIT_TEST_TO_PROPERTIES);
    }

    /**
     * Gets the module config variable name, if it exists.
     * @param methodBlock the UT method block ast, which should have a type {@link TokenTypes#SLIST}
     * @return the optional variable name, if it exists
     */
    private static Optional<String> getModuleConfigVariableName(DetailAST methodBlock) {
        Optional<String> returnValue = Optional.empty();

        for (DetailAST ast = methodBlock.getFirstChild(); ast != null; ast = ast.getNextSibling()) {
            if (ast.getType() == TokenTypes.VARIABLE_DEF) {
                final DetailAST type = ast.findFirstToken(TokenTypes.TYPE);
                final DetailAST assign = ast.findFirstToken(TokenTypes.ASSIGN);
                if (isDefaultConfigurationType(type) && isCreateModuleConfigAssign(assign)) {
                    returnValue = Optional.of(type.getNextSibling().getText());
                }
            }
            else if (ast.getType() == TokenTypes.EXPR
                    && ast.getFirstChild().getType() == TokenTypes.ASSIGN) {
                final DetailAST exprChild = ast.getFirstChild();
                if (isCreateModuleConfigAssign(exprChild)) {
                    returnValue = Optional.of(exprChild.getFirstChild().getText());
                }
            }
            if (returnValue.isPresent()) {
                break;
            }
        }

        return returnValue;
    }

    /**
     * Checks whether this {@link TokenTypes#TYPE} ast is {@link DefaultConfiguration}.
     * @param ast the {@link TokenTypes#TYPE} ast
     * @return true if the type is {@link DefaultConfiguration}
     */
    private static boolean isDefaultConfigurationType(DetailAST ast) {
        return "DefaultConfiguration".equals(ast.getFirstChild().getText());
    }

    /**
     * Checks whether this {@link TokenTypes#ASSIGN} ast contains
     * a {@code createModuleConfig} method call.
     * @param ast the {@link TokenTypes#ASSIGN} ast
     * @return true if the assignment contains a {@code createModuleConfig} method call
     */
    private static boolean isCreateModuleConfigAssign(DetailAST ast) {
        final boolean result;

        if (ast == null) {
            result = false;
        }
        else {
            final DetailAST methodCall;
            if (ast.findFirstToken(TokenTypes.METHOD_CALL) == null) {
                methodCall = ast.getFirstChild().getFirstChild();
            }
            else {
                methodCall = ast.findFirstToken(TokenTypes.METHOD_CALL);
            }
            result = methodCall.getType() == TokenTypes.METHOD_CALL
                    && methodCall.getFirstChild().getType() == TokenTypes.IDENT
                    && "createModuleConfig".equals(methodCall.getFirstChild().getText());
        }

        return result;
    }

    /**
     * Gets all children of a ast with the given tokens type.
     * @param parent the parent ast
     * @param type   the given tokens type
     * @return the children with the given tokens type
     */
    private static List<DetailAST> getAllChildrenWithToken(DetailAST parent, int type) {
        final List<DetailAST> returnValue = new LinkedList<>();

        for (DetailAST ast = parent.getFirstChild(); ast != null; ast = ast.getNextSibling()) {
            if (ast.getType() == type) {
                returnValue.add(ast);
            }
        }

        return returnValue;
    }

    /**
     * Checks whether this expression is an {@code addAttribute} method call on an instance with
     * the given variable name.
     * @param ast          the ast to check
     * @param variableName the given variable name of the module config instance
     * @return true if the expression is a valid {@code addAttribute} method call
     */
    private static boolean isAddAttributeMethodCall(DetailAST ast, String variableName) {
        final boolean result;

        if (ast.getType() == TokenTypes.METHOD_CALL
                && ast.getFirstChild().getType() == TokenTypes.DOT) {
            final DetailAST dot = ast.getFirstChild();
            result = variableName.equals(dot.getFirstChild().getText())
                    && "addAttribute".equals(dot.getLastChild().getText());
        }
        else {
            result = false;
        }

        return result;
    }

    /**
     * Converts an expression content to raw text.
     * @param ast the first child of expression ast to convert
     * @return the converted raw text
     */
    private String convertExpressionToText(DetailAST ast) {
        String result = null;
        if (ast.getType() == TokenTypes.STRING_LITERAL) {
            final String original = ast.getText();
            result = original.substring(1, original.length() - 1);
        }
        else if (ast.getType() == TokenTypes.PLUS) {
            result = convertExpressionToText(ast.getFirstChild())
                    + convertExpressionToText(ast.getLastChild());
        }
        else if (ast.getType() == TokenTypes.LITERAL_NULL) {
            result = ast.getText();
        }
        else if (ast.getType() == TokenTypes.METHOD_CALL) {
            final String line = getFileContents().getLine(ast.getLineNo() - 1);
            final Pattern pattern = Pattern.compile(
                    "\\.addAttribute\\(.+, (?:.+\\.)*(.+)\\.toString\\(\\)\\);");
            final Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                result = matcher.group(1);
            }
        }

        if (result == null) {
            throw new IllegalStateException("the processor cannot support this ast: " + ast);
        }

        return result;
    }
}
