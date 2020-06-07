/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.ballerinalang.compiler.semantics.analyzer;

import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.tree.ActionNode;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.OperatorKind;
import org.ballerinalang.model.tree.expressions.NamedArgNode;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.ballerinalang.model.tree.expressions.XMLNavigationAccess;
import org.ballerinalang.model.types.TupleType;
import org.ballerinalang.model.types.Type;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.util.BLangCompilerConstants;
import org.ballerinalang.util.diagnostic.DiagnosticCode;
import org.wso2.ballerinalang.compiler.parser.BLangAnonymousModelHelper;
import org.wso2.ballerinalang.compiler.parser.BLangMissingNodesHelper;
import org.wso2.ballerinalang.compiler.parser.NodeCloner;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BConstantSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BErrorTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BLetSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BOperatorSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BXMLNSSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BErrorType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFiniteType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFutureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BMapType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStreamType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTupleType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTypedescType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BXMLSubType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BXMLType;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangInvokableNode;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTableKeySpecifier;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangDoClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangFromClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangInputClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangJoinClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangLetClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangLimitClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOnClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangOnConflictClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangSelectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangWhereClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAccessExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrowFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckPanickedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConstRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangElvisExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangErrorVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangGroupExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIntRangeExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLetExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMatchExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMatchExpression.BLangMatchExprPatternClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryAction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKey;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangRecordKeyValueField;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRestArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangServiceConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStringTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTableConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTableMultiKeyExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTernaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTrapExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTupleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeTestExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypedescExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangUnaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangVariableReference;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWaitExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWaitForAllExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerFlushExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerSyncSendExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttributeAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLCommentLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementFilter;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLNavigationAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLProcInsLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQName;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQuotedString;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.types.BLangLetVariable;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.BArrayState;
import org.wso2.ballerinalang.compiler.util.ClosureVarSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.FieldKind;
import org.wso2.ballerinalang.compiler.util.ImmutableTypeCloner;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.NumericLiteralSupport;
import org.wso2.ballerinalang.compiler.util.TypeDefBuilderHelper;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLog;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLogHelper;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;
import org.wso2.ballerinalang.util.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import static org.wso2.ballerinalang.compiler.tree.BLangInvokableNode.DEFAULT_WORKER_NAME;
import static org.wso2.ballerinalang.compiler.util.Constants.WORKER_LAMBDA_VAR_PREFIX;

/**
 * @since 0.94
 */
public class TypeChecker extends BLangNodeVisitor {

    private static final CompilerContext.Key<TypeChecker> TYPE_CHECKER_KEY = new CompilerContext.Key<>();
    private static Set<String> modifierFunctions = new HashSet<>();

    private static final String TABLE_TNAME = "table";
    private static final String FUNCTION_NAME_PUSH = "push";
    private static final String FUNCTION_NAME_POP = "pop";
    private static final String FUNCTION_NAME_SHIFT = "shift";
    private static final String FUNCTION_NAME_UNSHIFT = "unshift";

    private Names names;
    private SymbolTable symTable;
    private SymbolEnter symbolEnter;
    private SymbolResolver symResolver;
    private NodeCloner nodeCloner;
    private Types types;
    private BLangDiagnosticLogHelper dlog;
    private SymbolEnv env;
    private boolean isTypeChecked;
    private TypeNarrower typeNarrower;
    private TypeParamAnalyzer typeParamAnalyzer;
    private BLangAnonymousModelHelper anonymousModelHelper;
    private SemanticAnalyzer semanticAnalyzer;
    private boolean nonErrorLoggingCheck = false;
    private int letCount = 0;
    private SymbolEnv narrowedQueryEnv;
    private BLangSelectClause selectClause;
    private BLangMissingNodesHelper missingNodesHelper;

    /**
     * Expected types or inherited types.
     */
    private BType expType;
    private BType resultType;

    private DiagnosticCode diagCode;

    static {
        modifierFunctions.add(FUNCTION_NAME_PUSH);
        modifierFunctions.add(FUNCTION_NAME_POP);
        modifierFunctions.add(FUNCTION_NAME_SHIFT);
        modifierFunctions.add(FUNCTION_NAME_UNSHIFT);
    }

    public static TypeChecker getInstance(CompilerContext context) {
        TypeChecker typeChecker = context.get(TYPE_CHECKER_KEY);
        if (typeChecker == null) {
            typeChecker = new TypeChecker(context);
        }

        return typeChecker;
    }

    public TypeChecker(CompilerContext context) {
        context.put(TYPE_CHECKER_KEY, this);

        this.names = Names.getInstance(context);
        this.symTable = SymbolTable.getInstance(context);
        this.symbolEnter = SymbolEnter.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.nodeCloner = NodeCloner.getInstance(context);
        this.types = Types.getInstance(context);
        this.dlog = BLangDiagnosticLogHelper.getInstance(context);
        this.typeNarrower = TypeNarrower.getInstance(context);
        this.typeParamAnalyzer = TypeParamAnalyzer.getInstance(context);
        this.anonymousModelHelper = BLangAnonymousModelHelper.getInstance(context);
        this.semanticAnalyzer = SemanticAnalyzer.getInstance(context);
        this.missingNodesHelper = BLangMissingNodesHelper.getInstance(context);
    }

    public BType checkExpr(BLangExpression expr, SymbolEnv env) {
        return checkExpr(expr, env, symTable.noType);
    }

    public BType checkExpr(BLangExpression expr, SymbolEnv env, BType expType) {
        return checkExpr(expr, env, expType, DiagnosticCode.INCOMPATIBLE_TYPES);
    }

    /**
     * Check the given list of expressions against the given expected types.
     *
     * @param exprs   list of expressions to be analyzed
     * @param env     current symbol environment
     * @param expType expected type
     * @return the actual types of the given list of expressions
     */
    public List<BType> checkExprs(List<BLangExpression> exprs, SymbolEnv env, BType expType) {
        List<BType> resTypes = new ArrayList<>(exprs.size());
        for (BLangExpression expr : exprs) {
            resTypes.add(checkExpr(expr, env, expType));
        }
        return resTypes;
    }

    public BType checkExpr(BLangExpression expr, SymbolEnv env, BType expType, DiagnosticCode diagCode) {
        if (expr.typeChecked) {
            return expr.type;
        }

        SymbolEnv prevEnv = this.env;
        BType preExpType = this.expType;
        DiagnosticCode preDiagCode = this.diagCode;
        this.env = env;
        this.diagCode = diagCode;
        this.expType = expType;
        this.isTypeChecked = true;
        expr.expectedType = expType;

        expr.accept(this);

        expr.type = resultType;
        expr.typeChecked = isTypeChecked;
        this.env = prevEnv;
        this.expType = preExpType;
        this.diagCode = preDiagCode;
        if (resultType.tag != TypeTags.SEMANTIC_ERROR) {
            expr.expectedType = resultType;
        }
        return resultType;
    }


    // Expressions

    public void visit(BLangLiteral literalExpr) {

        BType literalType = setLiteralValueAndGetType(literalExpr, expType);
        if (literalType == symTable.semanticError || literalExpr.isFiniteContext) {
            return;
        }
        resultType = types.checkType(literalExpr, literalType, expType);
    }

    @Override
    public void visit(BLangXMLElementAccess xmlElementAccess) {
        // check for undeclared namespaces.
        checkXMLNamespacePrefixes(xmlElementAccess.filters);
        checkExpr(xmlElementAccess.expr, env, symTable.xmlType);
        resultType = new BXMLType(symTable.xmlElementType, null);
    }

    @Override
    public void visit(BLangXMLNavigationAccess xmlNavigation) {
        if (xmlNavigation.lhsVar) {
            dlog.error(xmlNavigation.pos, DiagnosticCode.CANNOT_UPDATE_XML_SEQUENCE);
        }
        checkXMLNamespacePrefixes(xmlNavigation.filters);
        if (xmlNavigation.childIndex != null) {
            checkExpr(xmlNavigation.childIndex, env, symTable.intType);
        }
        BType actualType = checkExpr(xmlNavigation.expr, env, symTable.xmlType);
        types.checkType(xmlNavigation, actualType, expType);

        if (xmlNavigation.navAccessType == XMLNavigationAccess.NavAccessType.CHILDREN) {
            resultType = symTable.xmlType;
        } else {
            resultType = new BXMLType(symTable.xmlElementType, null);
        }
    }

    private void checkXMLNamespacePrefixes(List<BLangXMLElementFilter> filters) {
        for (BLangXMLElementFilter filter : filters) {
            if (!filter.namespace.isEmpty()) {
                Name nsName = names.fromString(filter.namespace);
                BSymbol nsSymbol = symResolver.lookupSymbolInPrefixSpace(env, nsName);
                filter.namespaceSymbol = nsSymbol;
                if (nsSymbol == symTable.notFoundSymbol) {
                    dlog.error(filter.nsPos, DiagnosticCode.CANNOT_FIND_XML_NAMESPACE, nsName);
                }
            }
        }
    }

    private BType setLiteralValueAndGetType(BLangLiteral literalExpr, BType expType) {
        // Get the type matching to the tag from the symbol table.
        BType literalType = symTable.getTypeFromTag(literalExpr.type.tag);
        Object literalValue = literalExpr.value;
        literalExpr.isJSONContext = types.isJSONContext(expType);

        if (literalType.tag == TypeTags.INT) {
            if (expType.tag == TypeTags.FLOAT) {
                literalType = symTable.floatType;
                literalExpr.value = ((Long) literalValue).doubleValue();
            } else if (expType.tag == TypeTags.DECIMAL &&
                    !NumericLiteralSupport.hasHexIndicator(literalExpr.originalValue)) {
                literalType = symTable.decimalType;
                literalExpr.value = String.valueOf(literalValue);
            } else if (TypeTags.isIntegerTypeTag(expType.tag) || expType.tag == TypeTags.BYTE) {
                literalType = getIntLiteralType(literalExpr.pos, expType, literalType, literalValue);
                if (literalType == symTable.semanticError) {
                    return symTable.semanticError;
                }
            } else if (expType.tag == TypeTags.FINITE && types.isAssignableToFiniteType(expType, literalExpr)) {
                BFiniteType finiteType = (BFiniteType) expType;
                if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.intType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.BYTE)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.byteType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.FLOAT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.floatType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.DECIMAL)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.decimalType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.SIGNED32_INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.signed32IntType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.SIGNED16_INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.signed16IntType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.SIGNED8_INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.signed8IntType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.UNSIGNED32_INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.unsigned32IntType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.UNSIGNED16_INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.unsigned16IntType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.UNSIGNED8_INT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.unsigned8IntType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                }
            } else if (expType.tag == TypeTags.UNION) {
                Set<BType> memberTypes = ((BUnionType) expType).getMemberTypes();
                if (memberTypes.stream()
                        .anyMatch(memType -> memType.tag == TypeTags.INT || memType.tag == TypeTags.JSON ||
                                memType.tag == TypeTags.ANYDATA || memType.tag == TypeTags.ANY)) {
                    return setLiteralValueAndGetType(literalExpr, symTable.intType);
                }

                BType finiteType = getFiniteTypeWithValuesOfSingleType((BUnionType) expType, symTable.intType);
                if (finiteType != symTable.semanticError) {
                    BType setType = setLiteralValueAndGetType(literalExpr, finiteType);
                    if (literalExpr.isFiniteContext) {
                        // i.e., a match was found for a finite type
                        return setType;
                    }
                }

                if (memberTypes.stream().anyMatch(memType -> memType.tag == TypeTags.BYTE)) {
                    return setLiteralValueAndGetType(literalExpr, symTable.byteType);
                }

                finiteType = getFiniteTypeWithValuesOfSingleType((BUnionType) expType, symTable.byteType);
                if (finiteType != symTable.semanticError) {
                    BType setType = setLiteralValueAndGetType(literalExpr, finiteType);
                    if (literalExpr.isFiniteContext) {
                        // i.e., a match was found for a finite type
                        return setType;
                    }
                }

                if (memberTypes.stream().anyMatch(memType -> memType.tag == TypeTags.FLOAT)) {
                    return setLiteralValueAndGetType(literalExpr, symTable.floatType);
                }

                finiteType = getFiniteTypeWithValuesOfSingleType((BUnionType) expType, symTable.floatType);
                if (finiteType != symTable.semanticError) {
                    BType setType = setLiteralValueAndGetType(literalExpr, finiteType);
                    if (literalExpr.isFiniteContext) {
                        // i.e., a match was found for a finite type
                        return setType;
                    }
                }

                if (memberTypes.stream().anyMatch(memType -> memType.tag == TypeTags.DECIMAL)) {
                    return setLiteralValueAndGetType(literalExpr, symTable.decimalType);
                }

                finiteType = getFiniteTypeWithValuesOfSingleType((BUnionType) expType, symTable.decimalType);
                if (finiteType != symTable.semanticError) {
                    BType setType = setLiteralValueAndGetType(literalExpr, finiteType);
                    if (literalExpr.isFiniteContext) {
                        // i.e., a match was found for a finite type
                        return setType;
                    }
                }
            }
        } else if (literalType.tag == TypeTags.FLOAT) {
            String literal = String.valueOf(literalValue);
            String numericLiteral = NumericLiteralSupport.stripDiscriminator(literal);
            boolean isDiscriminatedFloat = NumericLiteralSupport.isFloatDiscriminated(literal);

            if (expType.tag == TypeTags.DECIMAL) {
                // It's illegal to assign discriminated float literal or hex literal to a decimal LHS.
                if (isDiscriminatedFloat || NumericLiteralSupport.isHexLiteral(numericLiteral)) {
                    dlog.error(literalExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType,
                               symTable.floatType);
                    resultType = symTable.semanticError;
                    return resultType;
                }
                // LHS expecting decimal value and RHS offer non discriminated float, consider RHS as decimal.
                literalType = symTable.decimalType;
                literalExpr.value = numericLiteral;
            } else if (expType.tag == TypeTags.FLOAT) {
                literalExpr.value = Double.parseDouble(String.valueOf(numericLiteral));
            } else if (expType.tag == TypeTags.FINITE && types.isAssignableToFiniteType(expType, literalExpr)) {
                BFiniteType finiteType = (BFiniteType) expType;
                if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.FLOAT)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.floatType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                } else if (!isDiscriminatedFloat
                        && literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.DECIMAL)) {
                    BType valueType = setLiteralValueAndGetType(literalExpr, symTable.decimalType);
                    setLiteralValueForFiniteType(literalExpr, valueType);
                    return valueType;
                }
            } else if (expType.tag == TypeTags.UNION) {
                BUnionType unionType = (BUnionType) expType;
                BType unionMember = getAndSetAssignableUnionMember(literalExpr, unionType, symTable.floatType);
                if (unionMember != symTable.noType) {
                    return unionMember;
                }
            }
        } else if (literalType.tag == TypeTags.DECIMAL) {
            return decimalLiteral(literalValue, literalExpr, expType);
        } else if (literalType.tag == TypeTags.STRING && this.expType.tag == TypeTags.CHAR_STRING &&
                types.isCharLiteralValue((String) literalValue)) {
            return symTable.charStringType;
        } else {
            if (this.expType.tag == TypeTags.FINITE) {
                boolean foundMember = types.isAssignableToFiniteType(this.expType, literalExpr);
                if (foundMember) {
                    setLiteralValueForFiniteType(literalExpr, literalType);
                    return literalType;
                }
            } else if (this.expType.tag == TypeTags.UNION) {
                BUnionType unionType = (BUnionType) this.expType;
                boolean foundMember = unionType.getMemberTypes()
                        .stream()
                        .anyMatch(memberType -> types.isAssignableToFiniteType(memberType, literalExpr));
                if (foundMember) {
                    setLiteralValueForFiniteType(literalExpr, literalType);
                    return literalType;
                }
            }
        }

        if (literalExpr.type.tag == TypeTags.BYTE_ARRAY) {
            // check whether this is a byte array
            literalType = new BArrayType(symTable.byteType);
        }

        return literalType;
    }

    private BType getAndSetAssignableUnionMember(BLangLiteral literalExpr, BUnionType expType, BType desiredType) {
        Set<BType> memberTypes = expType.getMemberTypes();
        if (memberTypes.stream()
                .anyMatch(memType -> memType.tag == desiredType.tag
                        || memType.tag == TypeTags.JSON
                        || memType.tag == TypeTags.ANYDATA
                        || memType.tag == TypeTags.ANY)) {
            return setLiteralValueAndGetType(literalExpr, desiredType);
        }

        BType finiteType = getFiniteTypeWithValuesOfSingleType(expType, symTable.floatType);
        if (finiteType != symTable.semanticError) {
            BType setType = setLiteralValueAndGetType(literalExpr, finiteType);
            if (literalExpr.isFiniteContext) {
                // i.e., a match was found for a finite type
                return setType;
            }
        }

        if (memberTypes.stream().anyMatch(memType -> memType.tag == TypeTags.DECIMAL)) {
            return setLiteralValueAndGetType(literalExpr, symTable.decimalType);
        }

        finiteType = getFiniteTypeWithValuesOfSingleType(expType, symTable.decimalType);
        if (finiteType != symTable.semanticError) {
            BType setType = setLiteralValueAndGetType(literalExpr, finiteType);
            if (literalExpr.isFiniteContext) {
                // i.e., a match was found for a finite type
                return setType;
            }
        }
        return symTable.noType;
    }

    private boolean literalAssignableToFiniteType(BLangLiteral literalExpr, BFiniteType finiteType,
                                                  int targetMemberTypeTag) {

        for (BLangExpression valueExpr : finiteType.getValueSpace()) {
            if (valueExpr.type.tag == targetMemberTypeTag &&
                    types.checkLiteralAssignabilityBasedOnType((BLangLiteral) valueExpr, literalExpr)) {
                return true;
            }
        }
        return false;
    }

    private BType decimalLiteral(Object literalValue, BLangLiteral literalExpr, BType expType) {
        String literal = String.valueOf(literalValue);
        if (expType.tag == TypeTags.FLOAT && NumericLiteralSupport.isDecimalDiscriminated(literal)) {
            dlog.error(literalExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType,
                       symTable.decimalType);
            resultType = symTable.semanticError;
            return resultType;
        }
        if (expType.tag == TypeTags.FINITE && types.isAssignableToFiniteType(expType, literalExpr)) {
            BFiniteType finiteType = (BFiniteType) expType;
            if (literalAssignableToFiniteType(literalExpr, finiteType, TypeTags.DECIMAL)) {
                BType valueType = setLiteralValueAndGetType(literalExpr, symTable.decimalType);
                setLiteralValueForFiniteType(literalExpr, valueType);
                return valueType;
            }
        } else if (expType.tag == TypeTags.UNION) {
            BUnionType unionType = (BUnionType) expType;
            BType unionMember = getAndSetAssignableUnionMember(literalExpr, unionType, symTable.decimalType);
            if (unionMember != symTable.noType) {
                return unionMember;
            }
        }
        literalExpr.value = NumericLiteralSupport.stripDiscriminator(literal);
        resultType = symTable.decimalType;
        return symTable.decimalType;
    }

    private void setLiteralValueForFiniteType(BLangLiteral literalExpr, BType type) {
        types.setImplicitCastExpr(literalExpr, type, this.expType);
        this.resultType = type;
        literalExpr.isFiniteContext = true;
    }

    private BType getFiniteTypeWithValuesOfSingleType(BUnionType unionType, BType matchType) {
        List<BFiniteType> finiteTypeMembers = unionType.getMemberTypes().stream()
                .filter(memType -> memType.tag == TypeTags.FINITE)
                .map(memFiniteType -> (BFiniteType) memFiniteType)
                .collect(Collectors.toList());

        if (finiteTypeMembers.isEmpty()) {
            return symTable.semanticError;
        }

        int tag = matchType.tag;
        Set<BLangExpression> matchedValueSpace = new LinkedHashSet<>();

        for (BFiniteType finiteType : finiteTypeMembers) {
            Set<BLangExpression> set = new HashSet<>();
            for (BLangExpression expression : finiteType.getValueSpace()) {
                if (expression.type.tag == tag) {
                    set.add(expression);
                }
            }
            matchedValueSpace.addAll(set);
        }

        if (matchedValueSpace.isEmpty()) {
            return symTable.semanticError;
        }

        return new BFiniteType(null, matchedValueSpace);
    }

    private BType getIntLiteralType(DiagnosticPos pos, BType expType, BType literalType, Object literalValue) {

        switch (expType.tag) {
            case TypeTags.INT:
                return symTable.intType;
            case TypeTags.BYTE:
                if (types.isByteLiteralValue((Long) literalValue)) {
                    return symTable.byteType;
                }
                break;
            case TypeTags.SIGNED32_INT:
                if (types.isSigned32LiteralValue((Long) literalValue)) {
                    return symTable.signed32IntType;
                }
                break;
            case TypeTags.SIGNED16_INT:
                if (types.isSigned16LiteralValue((Long) literalValue)) {
                    return symTable.signed16IntType;
                }
                break;
            case TypeTags.SIGNED8_INT:
                if (types.isSigned8LiteralValue((Long) literalValue)) {
                    return symTable.signed8IntType;
                }
                break;
            case TypeTags.UNSIGNED32_INT:
                if (types.isUnsigned32LiteralValue((Long) literalValue)) {
                    return symTable.unsigned32IntType;
                }
                break;
            case TypeTags.UNSIGNED16_INT:
                if (types.isUnsigned16LiteralValue((Long) literalValue)) {
                    return symTable.unsigned16IntType;
                }
                break;
            case TypeTags.UNSIGNED8_INT:
                if (types.isUnsigned8LiteralValue((Long) literalValue)) {
                    return symTable.unsigned8IntType;
                }
                break;
            default:
        }
        dlog.error(pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType, literalType);
        resultType = symTable.semanticError;
        return resultType;
    }

    @Override
    public void visit(BLangListConstructorExpr listConstructor) {
        if (expType.tag == TypeTags.NONE) {
            BType inferredType = getInferredTupleType(listConstructor);
            if (inferredType != symTable.semanticError) {
                resultType = types.checkType(listConstructor, inferredType, expType);
            }
            return;
        }

        resultType = checkListConstructorCompatibility(expType, listConstructor);
    }

    @Override
    public void visit(BLangTableConstructorExpr tableConstructorExpr) {
        if (expType.tag == TypeTags.NONE) {
            List<BType> memTypes = checkExprList(new ArrayList<>(tableConstructorExpr.recordLiteralList), env);
            for (BType memType : memTypes) {
                if (memType == symTable.semanticError) {
                    resultType = symTable.semanticError;
                    return;
                }
            }

            BType inherentMemberType = inferTableMemberType(memTypes, tableConstructorExpr);
            BTableType tableType = new BTableType(TypeTags.TABLE, inherentMemberType, null);
            for (BLangRecordLiteral recordLiteral : tableConstructorExpr.recordLiteralList) {
                recordLiteral.type = inherentMemberType;
            }

            if (!validateTableConstructorExpr(tableConstructorExpr, tableType)) {
                resultType = symTable.semanticError;
                return;
            }

            if (checkKeySpecifier(tableConstructorExpr, tableType)) {
                return;
            }
            resultType = tableType;
        } else if (expType.tag == TypeTags.TABLE) {
            List<BType> memTypes = new ArrayList<>();
            for (BLangRecordLiteral recordLiteral : tableConstructorExpr.recordLiteralList) {
                BType recordType = checkExpr(recordLiteral, env, ((BTableType) expType).constraint);
                if (recordType == symTable.semanticError) {
                    resultType = symTable.semanticError;
                    return;
                }
                memTypes.add(recordType);
            }

            if (((BTableType) expType).constraint.tag == TypeTags.MAP) {
                validateMapConstraintTable(tableConstructorExpr);
                return;
            }

            if (!(validateTableType((BTableType) expType,
                    tableConstructorExpr.recordLiteralList) &&
                    validateTableConstructorExpr(tableConstructorExpr, (BTableType) expType))) {
                resultType = symTable.semanticError;
                return;
            }

            BTableType tableType = new BTableType(TypeTags.TABLE, inferTableMemberType(memTypes), null);
            if (checkKeySpecifier(tableConstructorExpr, tableType)) {
                return;
            }

            BTableType expectedTableType = (BTableType) expType;
            if (expectedTableType.fieldNameList != null && tableType.fieldNameList == null) {
                tableType.fieldNameList = expectedTableType.fieldNameList;
            }
            resultType = tableType;
        } else if (expType.tag == TypeTags.UNION) {
            List<BType> matchingTypes = new ArrayList<>();
            BUnionType expectedType = (BUnionType) expType;
            BType actualType = checkExpr(tableConstructorExpr, env, symTable.noType);
            for (BType memType : expectedType.getMemberTypes()) {
                if (types.isAssignable(actualType, memType)) {
                    matchingTypes.add(actualType);
                }
            }

            if (matchingTypes.size() == 1) {
                resultType = matchingTypes.get(0);
                return;
            } else if (matchingTypes.size() > 1) {
                dlog.error(tableConstructorExpr.pos, DiagnosticCode.AMBIGUOUS_TYPES, expectedType);
            }
            resultType = symTable.semanticError;
        } else {
            resultType = symTable.semanticError;
        }
    }

    private boolean checkKeySpecifier(BLangTableConstructorExpr tableConstructorExpr, BTableType tableType) {
        if (tableConstructorExpr.tableKeySpecifier != null) {
            if (!(validateTableConstructorRecordLiterals(getTableKeyNameList(tableConstructorExpr.
                    tableKeySpecifier), tableConstructorExpr.recordLiteralList))) {
                resultType = symTable.semanticError;
                return true;
            }
            tableType.fieldNameList = getTableKeyNameList(tableConstructorExpr.tableKeySpecifier);
        }
        return false;
    }

    private BType inferTableMemberType(List<BType> memTypes) {

        if (memTypes.isEmpty()) {
            return ((BTableType) expType).constraint;
        }

        LinkedHashSet<BType> result = new LinkedHashSet<>();

        result.add(memTypes.get(0));

        BUnionType unionType = BUnionType.create(null, result);
        for (int i = 1; i < memTypes.size(); i++) {
            BType source = memTypes.get(i);
            if (!types.isAssignable(source, unionType)) {
                result.add(source);
                unionType = BUnionType.create(null, result);
            }
        }

        if (unionType.getMemberTypes().size() == 1) {
            return memTypes.get(0);
        }

        return unionType;
    }

    private BType inferTableMemberType(List<BType> memTypes, BLangTableConstructorExpr tableConstructorExpr) {
        BLangTableKeySpecifier keySpecifier = tableConstructorExpr.tableKeySpecifier;
        List<String> keySpecifierFieldNames = new ArrayList<>();
        Set<BField> allFieldSet = new LinkedHashSet<>();
        for (BType memType : memTypes) {
            allFieldSet.addAll(((BRecordType) memType).fields.values());
        }

        Set<BField> commonFieldSet = new LinkedHashSet<>(allFieldSet);
        for (BType memType : memTypes) {
            commonFieldSet.retainAll(((BRecordType) memType).fields.values());
        }

        List<String> requiredFieldNames = new ArrayList<>();
        if (keySpecifier != null) {
            for (BLangIdentifier identifier : keySpecifier.fieldNameIdentifierList) {
                requiredFieldNames.add(identifier.value);
                keySpecifierFieldNames.add(identifier.value);
            }
        }

        List<String> fieldNames = new ArrayList<>();
        for (BField field : allFieldSet) {
            String fieldName = field.name.value;

            if (fieldNames.contains(fieldName)) {
                dlog.error(tableConstructorExpr.pos, DiagnosticCode.CANNOT_INFER_MEMBER_TYPE_FOR_TABLE,
                        fieldName);
                return symTable.semanticError;
            }
            fieldNames.add(fieldName);

            boolean isOptional = true;
            for (BField commonField : commonFieldSet) {
                if (commonField.name.value.equals(fieldName)) {
                    isOptional = false;
                    requiredFieldNames.add(commonField.name.value);
                }
            }

            if (isOptional) {
                field.symbol.flags = Flags.asMask(EnumSet.of(Flag.OPTIONAL));
            } else if (requiredFieldNames.contains(fieldName) && keySpecifierFieldNames.contains(fieldName)) {
                field.symbol.flags = Flags.asMask(EnumSet.of(Flag.REQUIRED)) + Flags.asMask(EnumSet.of(Flag.READONLY));
            } else if (requiredFieldNames.contains(fieldName)) {
                field.symbol.flags = Flags.asMask(EnumSet.of(Flag.REQUIRED));
            }
        }

        return createTableConstraintRecordType(allFieldSet, tableConstructorExpr.pos);
    }

    private BRecordType createTableConstraintRecordType(Set<BField> allFieldSet, DiagnosticPos pos) {
        PackageID pkgID = env.enclPkg.symbol.pkgID;
        BRecordTypeSymbol recordSymbol =
                Symbols.createRecordSymbol(0, names.fromString(anonymousModelHelper.getNextAnonymousTypeKey(pkgID)),
                        pkgID, null, env.scope.owner);
        BInvokableType bInvokableType = new BInvokableType(new ArrayList<>(), symTable.nilType, null);
        BInvokableSymbol initFuncSymbol = Symbols.createFunctionSymbol(
                Flags.PUBLIC, Names.EMPTY, env.enclPkg.symbol.pkgID, bInvokableType, env.scope.owner, false);
        initFuncSymbol.retType = symTable.nilType;
        recordSymbol.initializerFunc = new BAttachedFunction(Names.INIT_FUNCTION_SUFFIX, initFuncSymbol,
                bInvokableType);

        recordSymbol.scope = new Scope(recordSymbol);
        recordSymbol.scope.define(
                names.fromString(recordSymbol.name.value + "." + recordSymbol.initializerFunc.funcName.value),
                recordSymbol.initializerFunc.symbol);

        for (BField field : allFieldSet) {
            recordSymbol.scope.define(field.name, field.symbol);
        }

        BRecordType recordType = new BRecordType(recordSymbol);
        recordType.fields = allFieldSet.stream().collect(getFieldCollector());

        recordSymbol.type = recordType;
        recordType.tsymbol = recordSymbol;

        BLangRecordTypeNode recordTypeNode = TypeDefBuilderHelper.createRecordTypeNode(recordType, pkgID, symTable,
                pos);
        recordTypeNode.initFunction = TypeDefBuilderHelper.createInitFunctionForRecordType(recordTypeNode, env, names,
                symTable);
        TypeDefBuilderHelper.addTypeDefinition(recordType, recordSymbol, recordTypeNode, env);
        recordType.sealed = true;
        recordType.restFieldType = symTable.noType;
        return recordType;
    }

    private Collector<BField, ?, LinkedHashMap<String, BField>> getFieldCollector() {
        BinaryOperator<BField> mergeFunc = (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
        return Collectors.toMap(field -> field.name.value, Function.identity(), mergeFunc, LinkedHashMap::new);
    }

    private boolean validateTableType(BTableType tableType, List<BLangRecordLiteral> recordLiterals) {

        if (!types.isAssignable(tableType.constraint, symTable.mapAllType)) {
            dlog.error(tableType.constraintPos, DiagnosticCode.TABLE_CONSTRAINT_INVALID_SUBTYPE, tableType.constraint);
            resultType = symTable.semanticError;
            return false;
        }

        List<String> fieldNameList = tableType.fieldNameList;
        if (fieldNameList != null) {
            return validateKeySpecifier(fieldNameList, tableType.constraint, tableType.keyPos) &&
                    validateTableConstructorRecordLiterals(fieldNameList, recordLiterals);
        }

        return true;
    }

    private boolean validateTableConstructorRecordLiterals(List<String> keySpecifierFieldNames,
                                                           List<BLangRecordLiteral> recordLiterals) {
        for (String fieldName : keySpecifierFieldNames) {
            for (BLangRecordLiteral recordLiteral : recordLiterals) {
                BLangRecordKeyValueField recordKeyValueField = getRecordKeyValueField(recordLiteral, fieldName);
                if (recordKeyValueField.getValue().getKind() == NodeKind.LITERAL ||
                        recordKeyValueField.getValue().getKind() == NodeKind.NUMERIC_LITERAL ||
                        recordKeyValueField.getValue().getKind() == NodeKind.RECORD_LITERAL_EXPR ||
                        recordKeyValueField.getValue().getKind() == NodeKind.ARRAY_LITERAL_EXPR ||
                        recordKeyValueField.getValue().getKind() == NodeKind.TUPLE_LITERAL_EXPR ||
                        recordKeyValueField.getValue().getKind() == NodeKind.XML_ELEMENT_LITERAL ||
                        recordKeyValueField.getValue().getKind() == NodeKind.XML_TEXT_LITERAL) {
                    continue;
                }

                dlog.error(recordLiteral.pos,
                        DiagnosticCode.KEY_SPECIFIER_FIELD_VALUE_MUST_BE_CONSTANT, fieldName);
                resultType = symTable.semanticError;
                return false;
            }
        }

        return true;
    }

    private BLangRecordKeyValueField getRecordKeyValueField(BLangRecordLiteral recordLiteral,
                                                            String fieldName) {
        for (RecordLiteralNode.RecordField recordField : recordLiteral.fields) {
            BLangRecordKeyValueField recordKeyValueField = (BLangRecordKeyValueField) recordField;
            if (fieldName.equals(recordKeyValueField.key.toString())) {
                return recordKeyValueField;
            }
        }

        return null;
    }

    private boolean validateKeySpecifier(List<String> fieldNameList, BType constraint,
                                         DiagnosticPos pos) {
        for (String fieldName : fieldNameList) {
            BField field = types.getTableConstraintField(constraint, fieldName);
            if (field == null) {
                dlog.error(pos,
                        DiagnosticCode.INVALID_FIELD_NAMES_IN_KEY_SPECIFIER, fieldName, constraint);
                resultType = symTable.semanticError;
                return false;
            }

            if (!Symbols.isFlagOn(field.symbol.flags, Flags.READONLY)) {
                dlog.error(pos,
                        DiagnosticCode.KEY_SPECIFIER_FIELD_MUST_BE_READONLY, fieldName);
                resultType = symTable.semanticError;
                return false;
            }

            if (!Symbols.isFlagOn(field.symbol.flags, Flags.REQUIRED)) {
                dlog.error(pos,
                        DiagnosticCode.KEY_SPECIFIER_FIELD_MUST_BE_REQUIRED, fieldName);
                resultType = symTable.semanticError;
                return false;
            }

            if (!types.isAssignable(field.type, symTable.anydataType)) {
                dlog.error(pos,
                        DiagnosticCode.KEY_SPECIFIER_FIELD_MUST_BE_ANYDATA, fieldName, constraint);
                resultType = symTable.semanticError;
                return false;
            }
        }
        return true;
    }

    private boolean validateTableConstructorExpr(BLangTableConstructorExpr tableConstructorExpr,
                                                 BTableType tableType) {
        BType constraintType = tableType.constraint;

        if (tableConstructorExpr.tableKeySpecifier != null) {
            List<String> fieldNameList = getTableKeyNameList(tableConstructorExpr.tableKeySpecifier);

            if (tableType.fieldNameList == null && !validateKeySpecifier(fieldNameList, constraintType,
                    tableConstructorExpr.tableKeySpecifier.pos)) {
                return false;
            }

            if (tableType.fieldNameList != null && !tableType.fieldNameList.equals(fieldNameList)) {
                dlog.error(tableConstructorExpr.tableKeySpecifier.pos, DiagnosticCode.TABLE_KEY_SPECIFIER_MISMATCH,
                        tableType.fieldNameList.toString(), fieldNameList.toString());
                resultType = symTable.semanticError;
                return false;
            }
        }

        BType keyTypeConstraint = tableType.keyTypeConstraint;
        if (keyTypeConstraint != null) {
            List<BType> memberTypes = new ArrayList<>();

            if (keyTypeConstraint.tag == TypeTags.TUPLE) {
                for (Type type : ((TupleType) keyTypeConstraint).getTupleTypes()) {
                    memberTypes.add((BType) type);
                }
            } else {
                memberTypes.add(keyTypeConstraint);
            }

            if (tableConstructorExpr.tableKeySpecifier == null && keyTypeConstraint.tag == TypeTags.NEVER) {
                return true;
            }

            if (tableConstructorExpr.tableKeySpecifier == null ||
                    tableConstructorExpr.tableKeySpecifier.fieldNameIdentifierList.size() != memberTypes.size()) {
                dlog.error(tableConstructorExpr.pos, DiagnosticCode.KEY_SPECIFIER_SIZE_MISMATCH_WITH_KEY_CONSTRAINT,
                        memberTypes.size(), tableConstructorExpr.tableKeySpecifier == null ? 0 :
                                tableConstructorExpr.tableKeySpecifier.fieldNameIdentifierList.size());
                resultType = symTable.semanticError;
                return false;
            }

            List<BLangIdentifier> fieldNameIdentifierList = tableConstructorExpr.tableKeySpecifier.
                    fieldNameIdentifierList;

            int index = 0;
            for (BLangIdentifier identifier : fieldNameIdentifierList) {
                BField field = types.getTableConstraintField(constraintType, identifier.value);
                if (!types.isAssignable(field.type, memberTypes.get(index))) {
                    dlog.error(tableConstructorExpr.tableKeySpecifier.pos,
                            DiagnosticCode.KEY_SPECIFIER_MISMATCH_WITH_KEY_CONSTRAINT,
                            fieldNameIdentifierList.toString(), memberTypes.toString());
                    resultType = symTable.semanticError;
                    return false;
                }
                index++;
            }
        }

        return true;
    }

    private void validateMapConstraintTable(BLangTableConstructorExpr tableConstructorExpr) {
        if (((BTableType) expType).fieldNameList != null || ((BTableType) expType).keyTypeConstraint != null) {
            dlog.error(((BTableType) expType).keyPos,
                    DiagnosticCode.KEY_CONSTRAINT_NOT_SUPPORTED_FOR_TABLE_WITH_MAP_CONSTRAINT);
            resultType = symTable.semanticError;
            return;
        }

        if (tableConstructorExpr.tableKeySpecifier != null) {
            dlog.error(tableConstructorExpr.tableKeySpecifier.pos,
                    DiagnosticCode.KEY_CONSTRAINT_NOT_SUPPORTED_FOR_TABLE_WITH_MAP_CONSTRAINT);
            resultType = symTable.semanticError;
            return;
        }

        if (!(validateTableType((BTableType) expType, tableConstructorExpr.recordLiteralList))) {
            resultType = symTable.semanticError;
            return;
        }

        resultType = expType;
    }

    private List<String> getTableKeyNameList(BLangTableKeySpecifier tableKeySpecifier) {
        List<String> fieldNamesList = new ArrayList<>();
        for (BLangIdentifier identifier : tableKeySpecifier.fieldNameIdentifierList) {
            fieldNamesList.add(identifier.value);
        }

        return fieldNamesList;
    }

    private BType createTableKeyConstraint(List<String> fieldNames, BType constraintType) {
        if (fieldNames == null) {
            return symTable.semanticError;
        }

        List<BType> memTypes = new ArrayList<>();
        for (String fieldName : fieldNames) {
            //null is not possible for field
            BField tableConstraintField = types.getTableConstraintField(constraintType, fieldName);

            if (tableConstraintField == null) {
                return symTable.semanticError;
            }

            BType fieldType = tableConstraintField.type;
            memTypes.add(fieldType);
        }

        if (memTypes.size() == 1) {
            return memTypes.get(0);
        }

        return new BTupleType(memTypes);
    }

    private BType checkListConstructorCompatibility(BType bType, BLangListConstructorExpr listConstructor) {
        if (bType.tag == TypeTags.UNION) {
            boolean prevNonErrorLoggingCheck = this.nonErrorLoggingCheck;
            this.nonErrorLoggingCheck = true;

            BLangDiagnosticLog prevDLog = this.dlog.getCurrentLog();
            this.dlog.setNonConsoleDLog();

            List<BType> compatibleTypes = new ArrayList<>();
            for (BType memberType : ((BUnionType) bType).getMemberTypes()) {
                BType listCompatibleMemType = getListConstructorCompatibleNonUnionType(memberType);

                if (listCompatibleMemType == symTable.semanticError) {
                    continue;
                }

                BType memCompatibiltyType = checkListConstructorCompatibility(listCompatibleMemType, listConstructor);

                if (memCompatibiltyType != symTable.semanticError && dlog.getErrorCount() == 0 &&
                        isUniqueType(compatibleTypes, memCompatibiltyType)) {
                    compatibleTypes.add(memCompatibiltyType);
                }
                dlog.resetErrorCount();
            }

            this.dlog.setCurrentLog(prevDLog);
            this.nonErrorLoggingCheck = prevNonErrorLoggingCheck;

            if (compatibleTypes.isEmpty()) {
                BLangListConstructorExpr exprToLog = listConstructor;
                if (this.nonErrorLoggingCheck) {
                    listConstructor.cloneAttempt++;
                    exprToLog = nodeCloner.clone(listConstructor);
                }

                dlog.error(listConstructor.pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType,
                           getInferredTupleType(exprToLog));
                return symTable.semanticError;
            } else if (compatibleTypes.size() != 1) {
                dlog.error(listConstructor.pos, DiagnosticCode.AMBIGUOUS_TYPES,
                           expType);
                return symTable.semanticError;
            }

            return checkListConstructorCompatibility(compatibleTypes.get(0), listConstructor);
        }

        BType possibleType = getListConstructorCompatibleNonUnionType(bType);

        switch (possibleType.tag) {
            case TypeTags.ARRAY:
                return checkArrayType(listConstructor, (BArrayType) possibleType);
            case TypeTags.TUPLE:
                return checkTupleType(listConstructor, (BTupleType) possibleType);
            case TypeTags.TYPEDESC:
                // i.e typedesc t = [int, string]
                List<BType> results = new ArrayList<>();
                listConstructor.isTypedescExpr = true;
                for (int i = 0; i < listConstructor.exprs.size(); i++) {
                    results.add(checkExpr(listConstructor.exprs.get(i), env, symTable.noType));
                }
                List<BType> actualTypes = new ArrayList<>();
                for (int i = 0; i < listConstructor.exprs.size(); i++) {
                    final BLangExpression expr = listConstructor.exprs.get(i);
                    if (expr.getKind() == NodeKind.TYPEDESC_EXPRESSION) {
                        actualTypes.add(((BLangTypedescExpr) expr).resolvedType);
                    } else if (expr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                        actualTypes.add(((BLangSimpleVarRef) expr).symbol.type);
                    } else {
                        actualTypes.add(results.get(i));
                    }
                }
                if (actualTypes.size() == 1) {
                    listConstructor.typedescType = actualTypes.get(0);
                } else {
                    listConstructor.typedescType = new BTupleType(actualTypes);
                }
                return new BTypedescType(listConstructor.typedescType, null);
        }

        BLangListConstructorExpr exprToLog = listConstructor;
        if (this.nonErrorLoggingCheck) {
            listConstructor.cloneAttempt++;
            exprToLog = nodeCloner.clone(listConstructor);
        }
        dlog.error(listConstructor.pos, DiagnosticCode.INCOMPATIBLE_TYPES, bType, getInferredTupleType(exprToLog));
        return symTable.semanticError;
    }

    private BType getListConstructorCompatibleNonUnionType(BType type) {
        switch (type.tag) {
            case TypeTags.ARRAY:
            case TypeTags.TUPLE:
            case TypeTags.TYPEDESC:
                return type;
            case TypeTags.JSON:
                return !Symbols.isFlagOn(type.flags, Flags.READONLY) ? symTable.arrayJsonType :
                        ImmutableTypeCloner.setImmutableType(null, types, symTable.arrayJsonType, env, symTable,
                                                             anonymousModelHelper, names);
            case TypeTags.ANYDATA:
                return !Symbols.isFlagOn(type.flags, Flags.READONLY) ? symTable.arrayAnydataType :
                        ImmutableTypeCloner.setImmutableType(null, types, symTable.arrayAnydataType, env, symTable,
                                                             anonymousModelHelper, names);
            case TypeTags.ANY:
                return !Symbols.isFlagOn(type.flags, Flags.READONLY) ? symTable.arrayType :
                        ImmutableTypeCloner.setImmutableType(null, types, symTable.arrayType, env, symTable,
                                                             anonymousModelHelper, names);
        }
        return symTable.semanticError;
    }

    private BType checkArrayType(BLangListConstructorExpr listConstructor, BArrayType arrayType) {
        BType eType = arrayType.eType;

        if (arrayType.state == BArrayState.OPEN_SEALED) {
            arrayType.size = listConstructor.exprs.size();
            arrayType.state = BArrayState.CLOSED_SEALED;
        } else if ((arrayType.state != BArrayState.UNSEALED) && (arrayType.size != listConstructor.exprs.size())) {
            if (arrayType.size < listConstructor.exprs.size()) {
                dlog.error(listConstructor.pos,
                           DiagnosticCode.MISMATCHING_ARRAY_LITERAL_VALUES, arrayType.size,
                           listConstructor.exprs.size());
                return symTable.semanticError;
            }

            if (!types.hasFillerValue(eType)) {
                dlog.error(listConstructor.pos,
                           DiagnosticCode.INVALID_LIST_CONSTRUCTOR_ELEMENT_TYPE, expType);
                return symTable.semanticError;
            }
        }

        boolean errored = false;
        for (BLangExpression expr : listConstructor.exprs) {
            if (exprIncompatible(eType, expr) && !errored) {
                errored = true;
            }
        }

        return errored ? symTable.semanticError : arrayType;
    }

    private BType checkTupleType(BLangListConstructorExpr listConstructor, BTupleType tupleType) {
        List<BLangExpression> exprs = listConstructor.exprs;
        List<BType> memberTypes = tupleType.tupleTypes;
        BType restType = tupleType.restType;

        int listExprSize = exprs.size();
        int memberTypeSize = memberTypes.size();

        if (listExprSize < memberTypeSize) {
            for (int i = listExprSize; i < memberTypeSize; i++) {
                if (!types.hasFillerValue(memberTypes.get(i))) {
                    dlog.error(listConstructor.pos, DiagnosticCode.SYNTAX_ERROR,
                               "tuple and expression size does not match");
                    return symTable.semanticError;
                }
            }
        } else if (listExprSize > memberTypeSize && restType == null) {
            dlog.error(listConstructor.pos, DiagnosticCode.SYNTAX_ERROR,
                       "tuple and expression size does not match");
            return symTable.semanticError;
        }

        boolean errored = false;

        int nonRestCountToCheck = listExprSize < memberTypeSize ? listExprSize : memberTypeSize;

        for (int i = 0; i < nonRestCountToCheck; i++) {
            if (exprIncompatible(memberTypes.get(i), exprs.get(i)) && !errored) {
                errored = true;
            }
        }

        for (int i = nonRestCountToCheck; i < exprs.size(); i++) {
            if (exprIncompatible(restType, exprs.get(i)) && !errored) {
                errored = true;
            }
        }
        return errored ? symTable.semanticError : tupleType;
    }

    private boolean exprIncompatible(BType eType, BLangExpression expr) {
        if (expr.typeChecked) {
            return expr.type == symTable.semanticError;
        }

        BLangExpression exprToCheck = expr;

        if (this.nonErrorLoggingCheck) {
            expr.cloneAttempt++;
            exprToCheck = nodeCloner.clone(expr);
        }

        return checkExpr(exprToCheck, this.env, eType) == symTable.semanticError;
    }

    private BType[] getExprListUniqueTypes(List<BLangExpression> exprs, SymbolEnv env) {
        LinkedHashSet<BType> typesSet = new LinkedHashSet<>(checkExprList(exprs, env));
        return typesSet.toArray(new BType[0]);
    }

    private List<BType> checkExprList(List<BLangExpression> exprs, SymbolEnv env) {
        List<BType> types = new ArrayList<>();
        SymbolEnv prevEnv = this.env;
        BType preExpType = this.expType;
        this.env = env;
        this.expType = symTable.noType;
        for (BLangExpression e : exprs) {
            checkExpr(e, this.env);
            types.add(resultType);
        }
        this.env = prevEnv;
        this.expType = preExpType;
        return types;
    }

    private BType getInferredTupleType(BLangListConstructorExpr listConstructor) {
        List<BType> memTypes = checkExprList(listConstructor.exprs, env);

        for (BType memType : memTypes) {
            if (memType == symTable.semanticError) {
                return symTable.semanticError;
            }
        }

        return new BTupleType(memTypes);
    }

    public void visit(BLangRecordLiteral recordLiteral) {
        int expTypeTag = expType.tag;

        if (expTypeTag == TypeTags.NONE) {
            expType = defineInferredRecordType(recordLiteral);
        } else if (expTypeTag == TypeTags.OBJECT) {
            dlog.error(recordLiteral.pos,
                       DiagnosticCode.INVALID_RECORD_LITERAL, expType);
            resultType = symTable.semanticError;
            return;
        }

        resultType = checkMappingConstructorCompatibility(expType, recordLiteral);
    }

    private BType checkMappingConstructorCompatibility(BType bType, BLangRecordLiteral mappingConstructor) {
        if (bType.tag == TypeTags.UNION) {
            boolean prevNonErrorLoggingCheck = this.nonErrorLoggingCheck;
            this.nonErrorLoggingCheck = true;

            BLangDiagnosticLog prevDLog = this.dlog.getCurrentLog();
            this.dlog.setNonConsoleDLog();

            List<BType> compatibleTypes = new ArrayList<>();
            for (BType memberType : ((BUnionType) bType).getMemberTypes()) {
                BType listCompatibleMemType = getMappingConstructorCompatibleNonUnionType(memberType);

                if (listCompatibleMemType == symTable.semanticError) {
                    continue;
                }

                BType memCompatibiltyType = checkMappingConstructorCompatibility(listCompatibleMemType,
                                                                                 mappingConstructor);

                if (memCompatibiltyType != symTable.semanticError && dlog.getErrorCount() == 0 &&
                        isUniqueType(compatibleTypes, memCompatibiltyType)) {
                    compatibleTypes.add(memCompatibiltyType);
                }
                dlog.resetErrorCount();
            }

            this.dlog.setCurrentLog(prevDLog);
            this.nonErrorLoggingCheck = prevNonErrorLoggingCheck;

            if (compatibleTypes.isEmpty()) {
                reportIncompatibleMappingConstructorError(mappingConstructor, bType);
                validateSpecifiedFields(mappingConstructor, symTable.semanticError);
                return symTable.semanticError;
            } else if (compatibleTypes.size() != 1) {
                dlog.error(mappingConstructor.pos, DiagnosticCode.AMBIGUOUS_TYPES, bType);
                validateSpecifiedFields(mappingConstructor, symTable.semanticError);
                return symTable.semanticError;
            }

            return checkMappingConstructorCompatibility(compatibleTypes.get(0), mappingConstructor);
        }

        BType possibleType = getMappingConstructorCompatibleNonUnionType(bType);

        switch (possibleType.tag) {
            case TypeTags.MAP:
                return validateSpecifiedFields(mappingConstructor, possibleType) ? possibleType :
                        symTable.semanticError;
            case TypeTags.RECORD:
                boolean isSpecifiedFieldsValid = validateSpecifiedFields(mappingConstructor, possibleType);

                boolean hasAllRequiredFields = validateRequiredAndReadonlyFields((BRecordType) possibleType,
                                                                                 mappingConstructor.fields,
                                                                                 mappingConstructor.pos);

                return isSpecifiedFieldsValid && hasAllRequiredFields ? possibleType : symTable.semanticError;
        }
        reportIncompatibleMappingConstructorError(mappingConstructor, bType);
        validateSpecifiedFields(mappingConstructor, symTable.semanticError);
        return symTable.semanticError;
    }

    private BType getMappingConstructorCompatibleNonUnionType(BType type) {
        switch (type.tag) {
            case TypeTags.MAP:
            case TypeTags.RECORD:
                return type;
            case TypeTags.JSON:
                return !Symbols.isFlagOn(type.flags, Flags.READONLY) ? symTable.mapJsonType :
                        ImmutableTypeCloner.setImmutableType(null, types, symTable.mapJsonType, env, symTable,
                                                             anonymousModelHelper, names);
            case TypeTags.ANYDATA:
                return !Symbols.isFlagOn(type.flags, Flags.READONLY) ? symTable.mapAnydataType :
                        ImmutableTypeCloner.setImmutableType(null, types, symTable.mapAnydataType, env, symTable,
                                                             anonymousModelHelper, names);
            case TypeTags.ANY:
                return !Symbols.isFlagOn(type.flags, Flags.READONLY) ? symTable.mapType :
                        ImmutableTypeCloner.setImmutableType(null, types, symTable.mapType, env, symTable,
                                                             anonymousModelHelper, names);
        }
        return symTable.semanticError;
    }

    private boolean isMappingConstructorCompatibleType(BType type) {
        return type.tag == TypeTags.RECORD || type.tag == TypeTags.MAP;
    }

    private void reportIncompatibleMappingConstructorError(BLangRecordLiteral mappingConstructorExpr, BType expType) {
        if (expType.tag != TypeTags.UNION) {
            dlog.error(mappingConstructorExpr.pos,
                       DiagnosticCode.MAPPING_CONSTRUCTOR_COMPATIBLE_TYPE_NOT_FOUND, expType);
            return;
        }

        BUnionType unionType = (BUnionType) expType;
        BType[] memberTypes = unionType.getMemberTypes().toArray(new BType[0]);

        // Special case handling for `T?` where T is a record type. This is done to give more user friendly error
        // messages for this common scenario.
        if (memberTypes.length == 2) {
            BRecordType recType = null;

            if (memberTypes[0].tag == TypeTags.RECORD && memberTypes[1].tag == TypeTags.NIL) {
                recType = (BRecordType) memberTypes[0];
            } else if (memberTypes[1].tag == TypeTags.RECORD && memberTypes[0].tag == TypeTags.NIL) {
                recType = (BRecordType) memberTypes[1];
            }

            if (recType != null) {
                validateSpecifiedFields(mappingConstructorExpr, recType);
                validateRequiredAndReadonlyFields(recType, mappingConstructorExpr.fields, mappingConstructorExpr.pos);
                return;
            }
        }

        // By this point, we know there aren't any types to which we can assign the mapping constructor. If this is
        // case where there is at least one type with which we can use mapping constructors, but this particular
        // mapping constructor is incompatible, we give an incompatible mapping constructor error.
        for (BType bType : memberTypes) {
            if (isMappingConstructorCompatibleType(bType)) {
                dlog.error(mappingConstructorExpr.pos, DiagnosticCode.INCOMPATIBLE_MAPPING_CONSTRUCTOR,
                           unionType);
                return;
            }
        }

        dlog.error(mappingConstructorExpr.pos,
                   DiagnosticCode.MAPPING_CONSTRUCTOR_COMPATIBLE_TYPE_NOT_FOUND, unionType);
    }

    private boolean validateSpecifiedFields(BLangRecordLiteral mappingConstructor, BType possibleType) {
        boolean isFieldsValid = true;

        for (RecordLiteralNode.RecordField field : mappingConstructor.fields) {
            BType checkedType = checkMappingField(field, possibleType);
            if (isFieldsValid && checkedType == symTable.semanticError) {
                isFieldsValid = false;
            }
        }

        return isFieldsValid;
    }

    private boolean validateRequiredAndReadonlyFields(BRecordType type,
                                                      List<RecordLiteralNode.RecordField> specifiedFields,
                                                      DiagnosticPos pos) {
        HashSet<String> specFieldNames = getFieldNames(specifiedFields);
        boolean hasAllRequiredFields = true;
        boolean hasValidReadonlyFields = true;

        for (BField field : type.fields.values()) {
            String fieldName = field.name.value;

            if (specFieldNames.contains(fieldName)) {
                FieldTypePosPair typePosPair = getRecordFieldByName(specifiedFields, fieldName);

                if (!Symbols.isFlagOn(field.symbol.flags, Flags.READONLY) ||
                        types.isAssignable(typePosPair.type, symTable.readonlyType)) {
                    continue;
                }

                if (hasValidReadonlyFields) {
                    hasValidReadonlyFields = false;
                }
            } else if (Symbols.isFlagOn(field.symbol.flags, Flags.REQUIRED)) {
                // Check if `field` is explicitly assigned a value in the record literal
                // If a required field is missing, it's a compile error
                dlog.error(pos, DiagnosticCode.MISSING_REQUIRED_RECORD_FIELD, field.name);
                if (hasAllRequiredFields) {
                    hasAllRequiredFields = false;
                }
            }
        }
        return hasValidReadonlyFields && hasAllRequiredFields;
    }

    /**
     * Get the specified field or the field type-desc as obtained from a record type descriptor in a spread field,
     * for a field expected in the record type descriptor.
     *
     * @param fields    The fields of the mapping constructor expression
     * @param name      The name of the field to look for
     * @return  The type and pos pair of the relevant field in the mapping constructor
     */
    private FieldTypePosPair getRecordFieldByName(List<RecordLiteralNode.RecordField> fields, String name) {
        for (RecordLiteralNode.RecordField field : fields) {
            if (field.isKeyValueField()) {
                BLangRecordKeyValueField keyValueField = (BLangRecordKeyValueField) field;
                if (name.equals(getKeyValueFieldName(keyValueField))) {
                    return new FieldTypePosPair(keyValueField.valueExpr.type, keyValueField.valueExpr.pos);
                }
            } else if (field.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                BLangRecordLiteral.BLangRecordVarNameField varNameField =
                        (BLangRecordLiteral.BLangRecordVarNameField) field;
                if (name.equals(getVarNameFieldName(varNameField))) {
                    return new FieldTypePosPair(varNameField.type, varNameField.pos);
                }
            } else {
                BLangExpression spreadFieldExpr = ((BLangRecordLiteral.BLangRecordSpreadOperatorField) field).expr;

                BType spreadFieldType = spreadFieldExpr.type;
                if (spreadFieldType.tag != TypeTags.RECORD) {
                    continue;
                }

                BRecordType recordType = (BRecordType) spreadFieldType;
                if (recordType.fields.containsKey(name)) {
                    return new FieldTypePosPair(recordType.fields.get(name).type, spreadFieldExpr.pos);
                }
            }
        }
        return null;
    }

    private HashSet<String> getFieldNames(List<RecordLiteralNode.RecordField> specifiedFields) {
        HashSet<String> fieldNames = new HashSet<>();

        for (RecordLiteralNode.RecordField specifiedField : specifiedFields) {
            if (specifiedField.isKeyValueField()) {
                String name = getKeyValueFieldName((BLangRecordKeyValueField) specifiedField);
                if (name == null) {
                    continue; // computed key
                }

                fieldNames.add(name);
            } else if (specifiedField.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                fieldNames.add(getVarNameFieldName((BLangRecordLiteral.BLangRecordVarNameField) specifiedField));
            } else {
                fieldNames.addAll(getSpreadOpFieldRequiredFieldNames(
                        (BLangRecordLiteral.BLangRecordSpreadOperatorField) specifiedField));
            }
        }

        return fieldNames;
    }

    private String getKeyValueFieldName(BLangRecordKeyValueField field) {
        BLangRecordKey key = field.key;
        if (key.computedKey) {
            return null;
        }

        BLangExpression keyExpr = key.expr;

        if (keyExpr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
            return ((BLangSimpleVarRef) keyExpr).variableName.value;
        } else if (keyExpr.getKind() == NodeKind.LITERAL) {
            return (String) ((BLangLiteral) keyExpr).value;
        }
        return null;
    }

    private String getVarNameFieldName(BLangRecordLiteral.BLangRecordVarNameField field) {
        return field.variableName.value;
    }

    private List<String> getSpreadOpFieldRequiredFieldNames(BLangRecordLiteral.BLangRecordSpreadOperatorField field) {
        BType spreadType = checkExpr(field.expr, env);

        if (spreadType.tag != TypeTags.RECORD) {
            return Collections.emptyList();
        }

        List<String> fieldNames = new ArrayList<>();
        for (BField bField : ((BRecordType) spreadType).getFields().values()) {
            if (!Symbols.isOptional(bField.symbol)) {
                fieldNames.add(bField.name.value);
            }
        }
        return fieldNames;
    }

    @Override
    public void visit(BLangWorkerFlushExpr workerFlushExpr) {
        if (workerFlushExpr.workerIdentifier != null) {
            String workerName = workerFlushExpr.workerIdentifier.getValue();
            if (!this.workerExists(this.env, workerName)) {
                this.dlog.error(workerFlushExpr.pos, DiagnosticCode.UNDEFINED_WORKER, workerName);
            }
        }
        BType actualType = BUnionType.create(null, symTable.errorType, symTable.nilType);
        resultType = types.checkType(workerFlushExpr, actualType, expType);
    }

    @Override
    public void visit(BLangWorkerSyncSendExpr syncSendExpr) {
        BSymbol symbol = symResolver.lookupSymbolInMainSpace(env, names.fromIdNode(syncSendExpr.workerIdentifier));

        if (symTable.notFoundSymbol.equals(symbol)) {
            syncSendExpr.workerType = symTable.semanticError;
        } else {
            syncSendExpr.workerType = symbol.type;
        }

        // TODO Need to remove this cached env
        syncSendExpr.env = this.env;
        checkExpr(syncSendExpr.expr, this.env);

        // Validate if the send expression type is anydata
        if (!syncSendExpr.expr.type.isAnydata()) {
            this.dlog.error(syncSendExpr.pos, DiagnosticCode.INVALID_TYPE_FOR_SEND,
                            syncSendExpr.expr.type);
        }

        String workerName = syncSendExpr.workerIdentifier.getValue();
        if (!this.workerExists(this.env, workerName)) {
            this.dlog.error(syncSendExpr.pos, DiagnosticCode.UNDEFINED_WORKER, workerName);
        }

        syncSendExpr.expectedType = expType;

        // Type checking against the matching receive is done during code analysis.
        // When the expected type is noType, set the result type as nil to avoid variable assignment is required errors.
        resultType = expType == symTable.noType ? symTable.nilType : expType;
    }

    @Override
    public void visit(BLangWorkerReceive workerReceiveExpr) {
        BSymbol symbol = symResolver.lookupSymbolInMainSpace(env, names.fromIdNode(workerReceiveExpr.workerIdentifier));

        if (workerReceiveExpr.isChannel) {
            this.dlog.error(workerReceiveExpr.pos, DiagnosticCode.UNDEFINED_ACTION);
            return;
        }
        // TODO Need to remove this cached env
        workerReceiveExpr.env = this.env;

        if (symTable.notFoundSymbol.equals(symbol)) {
            workerReceiveExpr.workerType = symTable.semanticError;
        } else {
            workerReceiveExpr.workerType = symbol.type;
        }
        // The receive expression cannot be assigned to var, since we cannot infer the type.
        if (symTable.noType == this.expType) {
            this.dlog.error(workerReceiveExpr.pos, DiagnosticCode.INVALID_USAGE_OF_RECEIVE_EXPRESSION);
        }
        // We cannot predict the type of the receive expression as it depends on the type of the data sent by the other
        // worker/channel. Since receive is an expression now we infer the type of it from the lhs of the statement.
        workerReceiveExpr.type = this.expType;
        resultType = this.expType;
    }

    private boolean workerExists(SymbolEnv env, String workerName) {
        //TODO: move this method to CodeAnalyzer
        if (workerName.equals(DEFAULT_WORKER_NAME)) {
           return true;
        }
        BSymbol symbol = this.symResolver.lookupSymbolInMainSpace(env, new Name(workerName));
        return symbol != this.symTable.notFoundSymbol &&
               symbol.type.tag == TypeTags.FUTURE &&
               ((BFutureType) symbol.type).workerDerivative;
    }

    @Override
    public void visit(BLangConstRef constRef) {
        constRef.symbol = symResolver.lookupMainSpaceSymbolInPackage(constRef.pos, env,
                names.fromIdNode(constRef.pkgAlias), names.fromIdNode(constRef.variableName));

        types.setImplicitCastExpr(constRef, constRef.type, expType);
        resultType = constRef.type;
    }

    public void visit(BLangSimpleVarRef varRefExpr) {
        // Set error type as the actual type.
        BType actualType = symTable.semanticError;

        Name varName = names.fromIdNode(varRefExpr.variableName);
        if (varName == Names.IGNORE) {
            if (varRefExpr.lhsVar) {
                varRefExpr.type = this.symTable.anyType;
            } else {
                varRefExpr.type = this.symTable.semanticError;
                dlog.error(varRefExpr.pos, DiagnosticCode.UNDERSCORE_NOT_ALLOWED);
            }
            varRefExpr.symbol = new BVarSymbol(0, varName, env.enclPkg.symbol.pkgID, varRefExpr.type, env.scope.owner);
            resultType = varRefExpr.type;
            return;
        }

        Name compUnitName = getCurrentCompUnit(varRefExpr);
        varRefExpr.pkgSymbol =
                symResolver.resolvePrefixSymbol(env, names.fromIdNode(varRefExpr.pkgAlias), compUnitName);
        if (varRefExpr.pkgSymbol == symTable.notFoundSymbol) {
            dlog.error(varRefExpr.pos, DiagnosticCode.UNDEFINED_MODULE, varRefExpr.pkgAlias);
        }

        if (varRefExpr.pkgSymbol.tag == SymTag.XMLNS) {
            actualType = symTable.stringType;
        } else if (varRefExpr.pkgSymbol != symTable.notFoundSymbol) {
            BSymbol symbol = symResolver.lookupMainSpaceSymbolInPackage(varRefExpr.pos, env,
                    names.fromIdNode(varRefExpr.pkgAlias), varName);
            // if no symbol, check same for object attached function
            if (symbol == symTable.notFoundSymbol && env.enclType != null) {
                Name objFuncName = names.fromString(Symbols
                        .getAttachedFuncSymbolName(env.enclType.type.tsymbol.name.value, varName.value));
                symbol = symResolver.resolveStructField(varRefExpr.pos, env, objFuncName,
                        env.enclType.type.tsymbol);
            }

            // TODO: call to isInLocallyDefinedRecord() is a temporary fix done to disallow local var references in
            //  locally defined record type defs. This check should be removed once local var referencing is supported.
            if (((symbol.tag & SymTag.VARIABLE) == SymTag.VARIABLE)) {
                BVarSymbol varSym = (BVarSymbol) symbol;
                checkSefReferences(varRefExpr.pos, env, varSym);
                varRefExpr.symbol = varSym;
                actualType = varSym.type;
                markAndRegisterClosureVariable(symbol, varRefExpr.pos);
            } else if ((symbol.tag & SymTag.TYPE_DEF) == SymTag.TYPE_DEF) {
                actualType = new BTypedescType(symbol.type, null);
                varRefExpr.symbol = symbol;
            } else if ((symbol.tag & SymTag.CONSTANT) == SymTag.CONSTANT) {
                BConstantSymbol constSymbol = (BConstantSymbol) symbol;
                varRefExpr.symbol = constSymbol;
                BType symbolType = symbol.type;
                if (symbolType != symTable.noType && expType.tag == TypeTags.FINITE ||
                        (expType.tag == TypeTags.UNION && ((BUnionType) expType).getMemberTypes().stream()
                                .anyMatch(memType -> memType.tag == TypeTags.FINITE &&
                                        types.isAssignable(symbolType, memType)))) {
                    actualType = symbolType;
                } else {
                    actualType = constSymbol.literalType;
                }

                // If the constant is on the LHS, modifications are not allowed.
                // E.g. m.k = "10"; // where `m` is a constant.
                if (varRefExpr.lhsVar || varRefExpr.compoundAssignmentLhsVar) {
                    actualType = symTable.semanticError;
                    dlog.error(varRefExpr.pos, DiagnosticCode.CANNOT_UPDATE_CONSTANT_VALUE);
                }
            } else {
                logUndefinedSymbolError(varRefExpr.pos, varName.value);
            }
        }

        // Check type compatibility
        if (expType.tag == TypeTags.ARRAY && isArrayOpenSealedType((BArrayType) expType)) {
            dlog.error(varRefExpr.pos, DiagnosticCode.SEALED_ARRAY_TYPE_CAN_NOT_INFER_SIZE);
            return;

        }
        resultType = types.checkType(varRefExpr, actualType, expType);
    }

    @Override
    public void visit(BLangRecordVarRef varRefExpr) {
        LinkedHashMap<String, BField> fields = new LinkedHashMap<>();

        String recordName = this.anonymousModelHelper.getNextAnonymousTypeKey(env.enclPkg.symbol.pkgID);
        BRecordTypeSymbol recordSymbol = Symbols.createRecordSymbol(0, names.fromString(recordName),
                                                                    env.enclPkg.symbol.pkgID, null, env.scope.owner);
        symbolEnter.defineSymbol(varRefExpr.pos, recordSymbol, env);

        boolean unresolvedReference = false;
        for (BLangRecordVarRef.BLangRecordVarRefKeyValue recordRefField : varRefExpr.recordRefFields) {
            ((BLangVariableReference) recordRefField.variableReference).lhsVar = true;
            checkExpr(recordRefField.variableReference, env);
            if (((BLangVariableReference) recordRefField.variableReference).symbol == null ||
                    !isValidVariableReference(recordRefField.variableReference)) {
                unresolvedReference = true;
                continue;
            }
            BVarSymbol bVarSymbol = (BVarSymbol) ((BLangVariableReference) recordRefField.variableReference).symbol;
            BField field = new BField(names.fromIdNode(recordRefField.variableName), varRefExpr.pos,
                                      new BVarSymbol(0, names.fromIdNode(recordRefField.variableName),
                                                     env.enclPkg.symbol.pkgID, bVarSymbol.type, recordSymbol));
            fields.put(field.name.value, field);
        }

        BLangExpression restParam = (BLangExpression) varRefExpr.restParam;
        if (restParam != null) {
            checkExpr(restParam, env);
            unresolvedReference = !isValidVariableReference(restParam);
        }

        if (unresolvedReference) {
            resultType = symTable.semanticError;
            return;
        }

        BRecordType bRecordType = new BRecordType(recordSymbol);
        bRecordType.fields = fields;
        recordSymbol.type = bRecordType;
        varRefExpr.symbol = new BVarSymbol(0, recordSymbol.name,
                env.enclPkg.symbol.pkgID, bRecordType, env.scope.owner);

        if (restParam == null) {
            bRecordType.sealed = true;
            bRecordType.restFieldType = symTable.noType;
        } else if (restParam.type == symTable.semanticError) {
            bRecordType.restFieldType = symTable.mapType;
        } else {
            // Rest variable type of Record ref (record destructuring assignment) is a map where T is the broad type of
            // all fields that are not specified in the destructuring pattern. Here we set the rest type of record type
            // to T.
            BMapType restParamType = (BMapType) restParam.type;
            bRecordType.restFieldType = restParamType.constraint;
        }

        resultType = bRecordType;
    }

    @Override
    public void visit(BLangErrorVarRef varRefExpr) {
        if (varRefExpr.typeNode != null) {
            BType bType = symResolver.resolveTypeNode(varRefExpr.typeNode, env);
            varRefExpr.type = bType;
            checkIndirectErrorVarRef(varRefExpr);
            resultType = bType;
            return;
        }

        if (varRefExpr.reason != null) {
            varRefExpr.reason.lhsVar = true;
            checkExpr(varRefExpr.reason, env);
        }

        boolean unresolvedReference = false;

        for (BLangNamedArgsExpression detailItem : varRefExpr.detail) {
            BLangVariableReference refItem = (BLangVariableReference) detailItem.expr;
            refItem.lhsVar = true;
            checkExpr(refItem, env);

            if (!isValidVariableReference(refItem)) {
                unresolvedReference = true;
                continue;
            }

            if (refItem.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR
                    || refItem.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR) {
                dlog.error(refItem.pos, DiagnosticCode.INVALID_VARIABLE_REFERENCE_IN_BINDING_PATTERN,
                           refItem);
                unresolvedReference = true;
                continue;
            }

            if (refItem.symbol == null) {
                unresolvedReference = true;
            }
        }

        if (varRefExpr.restVar != null) {
            varRefExpr.restVar.lhsVar = true;
            if (varRefExpr.restVar.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                checkExpr(varRefExpr.restVar, env);
                unresolvedReference = unresolvedReference
                        || varRefExpr.restVar.symbol == null
                        || !isValidVariableReference(varRefExpr.restVar);

            } else if (varRefExpr.restVar.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR
                    || varRefExpr.restVar.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR) {
                unresolvedReference = checkErrorRestParamVarRef(varRefExpr, unresolvedReference);
            }
        }

        if (unresolvedReference) {
            resultType = symTable.semanticError;
            return;
        }

        BType errorRefRestFieldType;
        if (varRefExpr.restVar == null) {
            errorRefRestFieldType = symTable.pureType;
        } else if (varRefExpr.restVar.getKind() == NodeKind.SIMPLE_VARIABLE_REF
                && ((BLangSimpleVarRef) varRefExpr.restVar).variableName.value.equals(Names.IGNORE.value)) {
            errorRefRestFieldType = symTable.pureType;
        } else if (varRefExpr.restVar.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR
            || varRefExpr.restVar.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR) {
            errorRefRestFieldType = varRefExpr.restVar.type;
        } else if (varRefExpr.restVar.type.tag == TypeTags.MAP) {
            errorRefRestFieldType = ((BMapType) varRefExpr.restVar.type).constraint;
        } else {
            dlog.error(varRefExpr.restVar.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                       varRefExpr.restVar.type, symTable.detailType);
            resultType = symTable.semanticError;
            return;
        }

        BType errorDetailType = getCompatibleDetailType(errorRefRestFieldType);
        resultType = new BErrorType(symTable.errorType.tsymbol, varRefExpr.reason.type, errorDetailType);
    }

    private void checkIndirectErrorVarRef(BLangErrorVarRef varRefExpr) {
        for (BLangNamedArgsExpression detailItem : varRefExpr.detail) {
            checkExpr(detailItem.expr, env);
            checkExpr(detailItem, env, detailItem.expr.type);
        }

        if (varRefExpr.restVar != null) {
            checkExpr(varRefExpr.restVar, env);
        }

        // Indirect error binding pattern does not have an error reason binding fragment
        varRefExpr.reason.type = symTable.noType;
    }

    private BRecordType getCompatibleDetailType(BType errorRefRestFieldType) {

        PackageID packageID = env.enclPkg.packageID;
        BRecordTypeSymbol detailSymbol = new BRecordTypeSymbol(SymTag.RECORD, Flags.PUBLIC, Names.EMPTY,
                packageID, null, env.scope.owner);
        detailSymbol.scope = new Scope(env.scope.owner);
        BRecordType detailType = new BRecordType(detailSymbol);

        int flags = Flags.asMask(new HashSet<>(Lists.of(Flag.OPTIONAL, Flag.PUBLIC)));
        BField messageField = new BField(Names.DETAIL_MESSAGE, null,
                new BVarSymbol(flags, Names.DETAIL_MESSAGE, packageID, symTable.stringType, detailSymbol));
        detailType.fields.put(messageField.name.value, messageField);
        detailSymbol.scope.define(Names.DETAIL_MESSAGE, messageField.symbol);

        BField causeField = new BField(Names.DETAIL_CAUSE, null,
                new BVarSymbol(flags, Names.DETAIL_CAUSE, packageID, symTable.errorType, detailSymbol));
        detailType.fields.put(causeField.name.value, causeField);
        detailSymbol.scope.define(Names.DETAIL_CAUSE, causeField.symbol);

        detailType.restFieldType = errorRefRestFieldType;

        // TODO : Remove this. Had to add this due to BIR codegen requires this.
        BInvokableType invokableType = new BInvokableType(new ArrayList<>(), symTable.nilType, null);
        BInvokableSymbol initSymbol = Symbols.createFunctionSymbol(0, Names.INIT_FUNCTION_SUFFIX, packageID,
                invokableType, detailSymbol, false);
        detailSymbol.initializerFunc = new BAttachedFunction(Names.INIT_FUNCTION_SUFFIX, initSymbol, invokableType);
        detailSymbol.scope.define(initSymbol.name, initSymbol);
        return detailType;
    }

    private boolean checkErrorRestParamVarRef(BLangErrorVarRef varRefExpr, boolean unresolvedReference) {
        BLangAccessExpression accessExpression = (BLangAccessExpression) varRefExpr.restVar;
        Name exprName = names.fromIdNode(((BLangSimpleVarRef) accessExpression.expr).variableName);
        BSymbol fSym = symResolver.lookupSymbolInMainSpace(env, exprName);
        if (fSym != null) {
            if (fSym.type.getKind() == TypeKind.MAP) {
                BType constraint = ((BMapType) fSym.type).constraint;
                if (types.isAssignable(constraint, symTable.pureType)) {
                    varRefExpr.restVar.type = constraint;
                } else {
                    varRefExpr.restVar.type = symTable.pureType;
                }
            } else {
                throw new UnsupportedOperationException("rec field base access");
            }
        } else {
            unresolvedReference = true;
        }
        return unresolvedReference;
    }

    @Override
    public void visit(BLangTupleVarRef varRefExpr) {
        List<BType> results = new ArrayList<>();
        for (int i = 0; i < varRefExpr.expressions.size(); i++) {
            ((BLangVariableReference) varRefExpr.expressions.get(i)).lhsVar = true;
            results.add(checkExpr(varRefExpr.expressions.get(i), env, symTable.noType));
        }
        BTupleType actualType = new BTupleType(results);
        if (varRefExpr.restParam != null) {
            BLangExpression restExpr = (BLangExpression) varRefExpr.restParam;
            ((BLangVariableReference) restExpr).lhsVar = true;
            BType checkedType = checkExpr(restExpr, env, symTable.noType);
            if (checkedType.tag != TypeTags.ARRAY) {
                dlog.error(varRefExpr.pos, DiagnosticCode.INVALID_TYPE_FOR_REST_DESCRIPTOR, checkedType);
                resultType = symTable.semanticError;
                return;
            }
            actualType.restType = ((BArrayType) checkedType).eType;
        }
        resultType = types.checkType(varRefExpr, actualType, expType);
    }

    /**
     * This method will recursively check if a multidimensional array has at least one open sealed dimension.
     *
     * @param arrayType array to check if open sealed
     * @return true if at least one dimension is open sealed
     */
    public boolean isArrayOpenSealedType(BArrayType arrayType) {
        if (arrayType.state == BArrayState.OPEN_SEALED) {
            return true;
        }
        if (arrayType.eType.tag == TypeTags.ARRAY) {
            return isArrayOpenSealedType((BArrayType) arrayType.eType);
        }
        return false;
    }

    /**
     * This method will recursively traverse and find the symbol environment of a lambda node (which is given as the
     * enclosing invokable node) which is needed to lookup closure variables. The variable lookup will start from the
     * enclosing invokable node's environment, which are outside of the scope of a lambda function.
     */
    private SymbolEnv findEnclosingInvokableEnv(SymbolEnv env, BLangInvokableNode encInvokable) {
        if (env.enclEnv.node != null && env.enclEnv.node.getKind() == NodeKind.ARROW_EXPR) {
            // if enclosing env's node is arrow expression
            return env.enclEnv;
        }

        if (env.enclEnv.node != null && env.enclEnv.node.getKind() == NodeKind.TRANSACTION) {
            // if enclosing env's node is a transaction
            return env.enclEnv;
        }

        if (env.enclInvokable != null && env.enclInvokable == encInvokable) {
            return findEnclosingInvokableEnv(env.enclEnv, encInvokable);
        }
        return env;
    }

    private SymbolEnv findEnclosingInvokableEnv(SymbolEnv env, BLangRecordTypeNode recordTypeNode) {
        if (env.enclEnv.node != null && env.enclEnv.node.getKind() == NodeKind.ARROW_EXPR) {
            // if enclosing env's node is arrow expression
            return env.enclEnv;
        }

        if (env.enclEnv.node != null && env.enclEnv.node.getKind() == NodeKind.TRANSACTION) {
            // if enclosing env's node is a transaction
            return env.enclEnv;
        }

        if (env.enclType != null && env.enclType == recordTypeNode) {
            return findEnclosingInvokableEnv(env.enclEnv, recordTypeNode);
        }
        return env;
    }

    private boolean isFunctionArgument(BSymbol symbol, List<BLangSimpleVariable> params) {
        return params.stream().anyMatch(param -> (param.symbol.name.equals(symbol.name) &&
                param.type.tag == symbol.type.tag));
    }

    public void visit(BLangFieldBasedAccess fieldAccessExpr) {
        // First analyze the variable reference expression.
        ((BLangVariableReference) fieldAccessExpr.expr).lhsVar = fieldAccessExpr.lhsVar;
        ((BLangVariableReference) fieldAccessExpr.expr).compoundAssignmentLhsVar =
                fieldAccessExpr.compoundAssignmentLhsVar;
        BType varRefType = getTypeOfExprInFieldAccess(fieldAccessExpr.expr);

        // Disallow `expr.ns:attrname` syntax on non xml expressions.
        if (fieldAccessExpr instanceof BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess
                && !isXmlAccess(fieldAccessExpr)) {
            dlog.error(fieldAccessExpr.pos, DiagnosticCode.INVALID_FIELD_ACCESS_EXPRESSION);
            resultType = symTable.semanticError;
            return;
        }

        BType actualType;
        // Accessing all fields using * is only supported for XML.
        // todo: remove this, this is no longer supported, this is moved to xml.<*>
        if (fieldAccessExpr.fieldKind == FieldKind.ALL && varRefType.tag != TypeTags.XML) {
            dlog.error(fieldAccessExpr.pos, DiagnosticCode.CANNOT_GET_ALL_FIELDS, varRefType);
            actualType = symTable.semanticError;
        } else {
            if (fieldAccessExpr.optionalFieldAccess) {
                if (fieldAccessExpr.lhsVar || fieldAccessExpr.compoundAssignmentLhsVar) {
                    dlog.error(fieldAccessExpr.pos,
                               DiagnosticCode.OPTIONAL_FIELD_ACCESS_NOT_REQUIRED_ON_LHS);
                    resultType = symTable.semanticError;
                    return;
                }
                actualType = checkOptionalFieldAccessExpr(fieldAccessExpr, varRefType,
                                                          names.fromIdNode(fieldAccessExpr.field));
            } else {
                actualType = checkFieldAccessExpr(fieldAccessExpr, varRefType, names.fromIdNode(fieldAccessExpr.field));

                if (actualType != symTable.semanticError &&
                        (fieldAccessExpr.lhsVar || fieldAccessExpr.compoundAssignmentLhsVar)) {
                    if (isAllReadonlyTypes(varRefType)) {
                        dlog.error(fieldAccessExpr.pos, DiagnosticCode.CANNOT_UPDATE_READONLY_VALUE_OF_TYPE,
                                   varRefType);
                        resultType = symTable.semanticError;
                        return;
                    } else if (types.isSubTypeOfBaseType(varRefType, TypeTags.RECORD) &&
                            isInvalidReadonlyFieldUpdate(varRefType, fieldAccessExpr.field.value)) {
                        dlog.error(fieldAccessExpr.pos, DiagnosticCode.CANNOT_UPDATE_READONLY_RECORD_FIELD,
                                   fieldAccessExpr.field.value, varRefType);
                        resultType = symTable.semanticError;
                        return;
                    }
                }
            }
        }

        resultType = types.checkType(fieldAccessExpr, actualType, this.expType);
    }

    private boolean isAllReadonlyTypes(BType type) {
        if (type.tag != TypeTags.UNION) {
            return Symbols.isFlagOn(type.flags, Flags.READONLY);
        }

        for (BType memberType : ((BUnionType) type).getMemberTypes()) {
            if (!isAllReadonlyTypes(memberType)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInvalidReadonlyFieldUpdate(BType type, String fieldName) {
        if (type.tag == TypeTags.RECORD) {
            BRecordType recordType = (BRecordType) type;
            for (BField field : recordType.fields.values()) {
                if (!field.name.value.equals(fieldName)) {
                    continue;
                }

                if (Symbols.isFlagOn(type.flags, Flags.READONLY)) {
                    return true;
                }

                return Symbols.isFlagOn(field.symbol.flags, Flags.READONLY) &&
                        // allow updates at compile-time for optional fields since it could be the first update.
                        Symbols.isFlagOn(field.symbol.flags, Flags.REQUIRED);
            }
            return recordType.sealed;
        }

        // For unions, we consider this an invalid update only if it is invalid for all member types. If for at least
        // one member this is valid, we allow this at compile time with the potential to fail at runtime.
        boolean allInvalidUpdates = true;
        for (BType memberType : ((BUnionType) type).getMemberTypes()) {
            if (!isInvalidReadonlyFieldUpdate(memberType, fieldName)) {
                allInvalidUpdates = false;
            }
        }
        return allInvalidUpdates;
    }

    private boolean isXmlAccess(BLangFieldBasedAccess fieldAccessExpr) {
        BLangExpression expr = fieldAccessExpr.expr;
        BType exprType = expr.type;

        if (exprType.tag == TypeTags.XML || exprType.tag == TypeTags.XML_ELEMENT) {
            return true;
        }

        if (expr.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR  && hasLaxOriginalType((BLangFieldBasedAccess) expr)
                && exprType.tag == TypeTags.UNION) {
            Set<BType> memberTypes = ((BUnionType) exprType).getMemberTypes();
            return memberTypes.contains(symTable.xmlType) || memberTypes.contains(symTable.xmlElementType);
          }

        return false;
    }

    public void visit(BLangIndexBasedAccess indexBasedAccessExpr) {
        // First analyze the variable reference expression.
        ((BLangVariableReference) indexBasedAccessExpr.expr).lhsVar = indexBasedAccessExpr.lhsVar;
        ((BLangVariableReference) indexBasedAccessExpr.expr).compoundAssignmentLhsVar =
                indexBasedAccessExpr.compoundAssignmentLhsVar;
        checkExpr(indexBasedAccessExpr.expr, this.env, symTable.noType);

        if (indexBasedAccessExpr.indexExpr.getKind() == NodeKind.TABLE_MULTI_KEY &&
                indexBasedAccessExpr.expr.type.tag != TypeTags.TABLE) {
            dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.MULTI_KEY_MEMBER_ACCESS_NOT_SUPPORTED,
                    indexBasedAccessExpr.expr.type);
            resultType = symTable.semanticError;
            return;
        }

        BType actualType = checkIndexAccessExpr(indexBasedAccessExpr);

        BType exprType = indexBasedAccessExpr.expr.type;
        BLangExpression indexExpr = indexBasedAccessExpr.indexExpr;

        if (actualType != symTable.semanticError &&
                (indexBasedAccessExpr.lhsVar || indexBasedAccessExpr.compoundAssignmentLhsVar)) {
            if (isAllReadonlyTypes(exprType)) {
                dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.CANNOT_UPDATE_READONLY_VALUE_OF_TYPE,
                           exprType);
                resultType = symTable.semanticError;
                return;
            } else if (types.isSubTypeOfBaseType(exprType, TypeTags.RECORD) &&
                    (indexExpr.getKind() == NodeKind.LITERAL || isConst(indexExpr)) &&
                    isInvalidReadonlyFieldUpdate(exprType, getConstFieldName(indexExpr))) {
                dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.CANNOT_UPDATE_READONLY_RECORD_FIELD,
                           getConstFieldName(indexExpr), exprType);
                resultType = symTable.semanticError;
                return;
            }
        }

        // If this is on lhs, no need to do type checking further. And null/error
        // will not propagate from parent expressions
        if (indexBasedAccessExpr.lhsVar) {
            indexBasedAccessExpr.originalType = actualType;
            indexBasedAccessExpr.type = actualType;
            resultType = actualType;
            return;
        }

        this.resultType = this.types.checkType(indexBasedAccessExpr, actualType, this.expType);
    }

    public void visit(BLangInvocation iExpr) {
        // Variable ref expression null means this is the leaf node of the variable ref expression tree
        // e.g. foo();, foo(), foo().k;
        if (iExpr.expr == null) {
            // This is a function invocation expression. e.g. foo()
            checkFunctionInvocationExpr(iExpr);
            return;
        }

        // Module aliases cannot be used with methods
        if (invalidModuleAliasUsage(iExpr)) {
            return;
        }

        // Find the variable reference expression type
        checkExpr(iExpr.expr, this.env, symTable.noType);

        BType varRefType = iExpr.expr.type;

        switch (varRefType.tag) {
            case TypeTags.OBJECT:
                // Invoking a function bound to an object
                // First check whether there exist a function with this name
                // Then perform arg and param matching
                checkObjectFunctionInvocationExpr(iExpr, (BObjectType) varRefType);
                break;
            case TypeTags.RECORD:
                checkFieldFunctionPointer(iExpr, this.env);
                break;
            case TypeTags.NONE:
                dlog.error(iExpr.pos, DiagnosticCode.UNDEFINED_FUNCTION, iExpr.name);
                break;
            case TypeTags.SEMANTIC_ERROR:
                break;
            default:
                checkInLangLib(iExpr, varRefType);
        }
    }

    public void visit(BLangInvocation.BLangActionInvocation aInv) {
        // For an action invocation, this will only be satisfied when it's an async call of a function.
        // e.g., start foo();
        if (aInv.expr == null) {
            checkFunctionInvocationExpr(aInv);
            return;
        }

        // Module aliases cannot be used with remote method call actions
        if (invalidModuleAliasUsage(aInv)) {
            return;
        }

        // Find the variable reference expression type
        checkExpr(aInv.expr, this.env, symTable.noType);
        BLangVariableReference varRef = (BLangVariableReference) aInv.expr;

        switch (varRef.type.tag) {
            case TypeTags.OBJECT:
                checkActionInvocation(aInv, (BObjectType) varRef.type);
                break;
            case TypeTags.RECORD:
                checkFieldFunctionPointer(aInv, this.env);
                break;
            case TypeTags.NONE:
                dlog.error(aInv.pos, DiagnosticCode.UNDEFINED_FUNCTION, aInv.name);
                resultType = symTable.semanticError;
                break;
            case TypeTags.SEMANTIC_ERROR:
            default:
                dlog.error(aInv.pos, DiagnosticCode.INVALID_ACTION_INVOCATION, varRef.type);
                resultType = symTable.semanticError;
                break;
        }
    }

    private boolean invalidModuleAliasUsage(BLangInvocation invocation) {
        Name pkgAlias = names.fromIdNode(invocation.pkgAlias);
        if (pkgAlias != Names.EMPTY) {
            dlog.error(invocation.pos, DiagnosticCode.PKG_ALIAS_NOT_ALLOWED_HERE);
            return true;
        }
        return false;
    }

    public void visit(BLangLetExpression letExpression) {
        BLetSymbol letSymbol = new BLetSymbol(SymTag.LET, Flags.asMask(new HashSet<>(Lists.of())),
                                              new Name(String.format("$let_symbol_%d$", letCount++)),
                                              env.enclPkg.symbol.pkgID, letExpression.type, env.scope.owner);
        letExpression.env = SymbolEnv.createExprEnv(letExpression, env, letSymbol);
        for (BLangLetVariable letVariable : letExpression.letVarDeclarations) {
            semanticAnalyzer.analyzeDef((BLangNode) letVariable.definitionNode, letExpression.env);
        }
        BType exprType = checkExpr(letExpression.expr, letExpression.env);
        types.checkType(letExpression, exprType, this.expType);
    }

    private void checkInLangLib(BLangInvocation iExpr, BType varRefType) {
        boolean langLibMethodExists = checkLangLibMethodInvocationExpr(iExpr, varRefType);
        if (!langLibMethodExists) {
            dlog.error(iExpr.name.pos, DiagnosticCode.UNDEFINED_FUNCTION_IN_TYPE, iExpr.name.value, iExpr.expr.type);
            resultType = symTable.semanticError;
            return;
        }

        checkIllegalStorageSizeChangeMethodCall(iExpr, varRefType);
    }

    private boolean isFixedLengthList(BType type) {
        switch(type.tag) {
            case TypeTags.ARRAY:
                return (((BArrayType) type).state != BArrayState.UNSEALED);
            case TypeTags.TUPLE:
                return (((BTupleType) type).restType == null);
            case TypeTags.UNION:
                BUnionType unionType = (BUnionType) type;
                for (BType member : unionType.getMemberTypes()) {
                    if (!isFixedLengthList(member)) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private void checkIllegalStorageSizeChangeMethodCall(BLangInvocation iExpr, BType varRefType) {
        String invocationName = iExpr.name.getValue();
        if (!modifierFunctions.contains(invocationName)) {
            return;
        }

        if (isFixedLengthList(varRefType)) {
            dlog.error(iExpr.name.pos, DiagnosticCode.ILLEGAL_FUNCTION_CHANGE_LIST_SIZE, invocationName, varRefType);
            resultType = symTable.semanticError;
            return;
        }

        if (isShiftOnIncompatibleTuples(varRefType, invocationName)) {
            dlog.error(iExpr.name.pos, DiagnosticCode.ILLEGAL_FUNCTION_CHANGE_TUPLE_SHAPE, invocationName,
                       varRefType);
            resultType = symTable.semanticError;
            return;
        }
    }

    private boolean isShiftOnIncompatibleTuples(BType varRefType, String invocationName) {
        if ((varRefType.tag == TypeTags.TUPLE) && (invocationName.compareTo(FUNCTION_NAME_SHIFT) == 0) &&
                hasDifferentTypeThanRest((BTupleType) varRefType)) {
            return true;
        }

        if ((varRefType.tag == TypeTags.UNION) && (invocationName.compareTo(FUNCTION_NAME_SHIFT) == 0)) {
            BUnionType unionVarRef = (BUnionType) varRefType;
            boolean allMemberAreFixedShapeTuples = true;
            for (BType member : unionVarRef.getMemberTypes()) {
                if (member.tag != TypeTags.TUPLE) {
                    allMemberAreFixedShapeTuples = false;
                    break;
                }
                if (!hasDifferentTypeThanRest((BTupleType) member)) {
                    allMemberAreFixedShapeTuples = false;
                    break;
                }
            }
            return allMemberAreFixedShapeTuples;
        }
        return false;
    }

    private boolean hasDifferentTypeThanRest(BTupleType tupleType) {
        if (tupleType.restType == null) {
            return false;
        }

        for (BType member : tupleType.getTupleTypes()) {
            if (!types.isSameType(tupleType.restType, member)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkFieldFunctionPointer(BLangInvocation iExpr, SymbolEnv env) {
        BType type = checkExpr(iExpr.expr, env);

        BLangIdentifier invocationIdentifier = iExpr.name;

        if (type == symTable.semanticError) {
            return false;
        }
        BSymbol funcSymbol = symResolver.resolveStructField(iExpr.pos, env, names.fromIdNode(invocationIdentifier),
                type.tsymbol);
        if (funcSymbol == symTable.notFoundSymbol) {
            if (!checkLangLibMethodInvocationExpr(iExpr, type)) {
                dlog.error(iExpr.name.pos, DiagnosticCode.UNDEFINED_FIELD_IN_RECORD, invocationIdentifier, type);
                resultType = symTable.semanticError;
            }
            return false;
        }
        iExpr.symbol = funcSymbol;
        iExpr.type = ((BInvokableSymbol) funcSymbol).retType;
        checkInvocationParamAndReturnType(iExpr);
        iExpr.functionPointerInvocation = true;
        return true;
    }

    public void visit(BLangTypeInit cIExpr) {
        if ((expType.tag == TypeTags.ANY && cIExpr.userDefinedType == null) || expType.tag == TypeTags.RECORD) {
            dlog.error(cIExpr.pos, DiagnosticCode.INVALID_TYPE_NEW_LITERAL, expType);
            resultType = symTable.semanticError;
            return;
        }

        BType actualType;
        if (cIExpr.userDefinedType != null) {
            actualType = symResolver.resolveTypeNode(cIExpr.userDefinedType, env);
        } else {
            actualType = expType;
        }

        if (actualType == symTable.semanticError) {
            //TODO dlog error?
            resultType = symTable.semanticError;
            return;
        }

        switch (actualType.tag) {
            case TypeTags.OBJECT:
                if ((actualType.tsymbol.flags & Flags.ABSTRACT) == Flags.ABSTRACT) {
                    dlog.error(cIExpr.pos, DiagnosticCode.CANNOT_INITIALIZE_ABSTRACT_OBJECT,
                               actualType.tsymbol);
                    cIExpr.initInvocation.argExprs.forEach(expr -> checkExpr(expr, env, symTable.noType));
                    resultType = symTable.semanticError;
                    return;
                }

                if (((BObjectTypeSymbol) actualType.tsymbol).initializerFunc != null) {
                    cIExpr.initInvocation.symbol = ((BObjectTypeSymbol) actualType.tsymbol).initializerFunc.symbol;
                    checkInvocationParam(cIExpr.initInvocation);
                    cIExpr.initInvocation.type = ((BInvokableSymbol) cIExpr.initInvocation.symbol).retType;
                } else {
                    // If the initializerFunc is null then this is a default constructor invocation. Hence should not
                    // pass any arguments.
                    if (!isValidInitInvocation(cIExpr, (BObjectType) actualType)) {
                        return;
                    }
                }
                break;
            case TypeTags.STREAM:
                if (cIExpr.initInvocation.argExprs.size() != 1) {
                    dlog.error(cIExpr.pos, DiagnosticCode.INVALID_STREAM_CONSTRUCTOR, cIExpr.initInvocation.name);
                    resultType = symTable.semanticError;
                    return;
                }

                BStreamType actualStreamType = (BStreamType) actualType;
                if (actualStreamType.error != null) {
                    BType error = actualStreamType.error;
                    if (!types.containsErrorType(error)) {
                        dlog.error(cIExpr.pos, DiagnosticCode.ERROR_TYPE_EXPECTED, error.toString());
                        resultType = symTable.semanticError;
                        return;
                    }
                }

                BLangExpression iteratorExpr = cIExpr.initInvocation.argExprs.get(0);
                BType constructType = checkExpr(iteratorExpr, env, symTable.noType);
                BUnionType nextReturnType = types.getVarTypeFromIteratorFuncReturnType(constructType);
                BUnionType expectedReturnType = createNextReturnType(cIExpr.pos, (BStreamType) actualType);
                if (nextReturnType == null) {
                    dlog.error(iteratorExpr.pos, DiagnosticCode.MISSING_REQUIRED_METHOD_NEXT,
                            constructType, expectedReturnType);
                    resultType = symTable.semanticError;
                    return;
                }

                types.checkType(iteratorExpr.pos, nextReturnType, expectedReturnType,
                        DiagnosticCode.INCOMPATIBLE_TYPES);
                resultType = actualType;
                return;
            case TypeTags.UNION:
                List<BType> matchingMembers = findMembersWithMatchingInitFunc(cIExpr, (BUnionType) actualType);
                BType matchedType = getMatchingType(matchingMembers, cIExpr, actualType);
                cIExpr.initInvocation.type = symTable.nilType;

                if (matchedType.tag == TypeTags.OBJECT) {
                    if (((BObjectTypeSymbol) matchedType.tsymbol).initializerFunc != null) {
                        cIExpr.initInvocation.symbol = ((BObjectTypeSymbol) matchedType.tsymbol).initializerFunc.symbol;
                        checkInvocationParam(cIExpr.initInvocation);
                        cIExpr.initInvocation.type = ((BInvokableSymbol) cIExpr.initInvocation.symbol).retType;
                        actualType = matchedType;
                        break;
                    } else {
                        if (!isValidInitInvocation(cIExpr, (BObjectType) matchedType)) {
                            return;
                        }
                    }
                }
                types.checkType(cIExpr, matchedType, expType);
                cIExpr.type = matchedType;
                resultType = matchedType;
                return;
            default:
                dlog.error(cIExpr.pos, DiagnosticCode.CANNOT_INFER_OBJECT_TYPE_FROM_LHS, actualType);
                resultType = symTable.semanticError;
                return;
        }

        if (cIExpr.initInvocation.type == null) {
            cIExpr.initInvocation.type = symTable.nilType;
        }
        BType actualTypeInitType = getObjectConstructorReturnType(actualType, cIExpr.initInvocation.type);
        resultType = types.checkType(cIExpr, actualTypeInitType, expType);
    }

    private BUnionType createNextReturnType(DiagnosticPos pos, BStreamType streamType) {
        BRecordType recordType = new BRecordType(null);
        recordType.restFieldType = symTable.noType;
        recordType.sealed = true;

        Name fieldName = Names.VALUE;
        BField field = new BField(fieldName, pos, new BVarSymbol(Flags.PUBLIC,
                fieldName, env.enclPkg.packageID, streamType.constraint, env.scope.owner));
        field.type = streamType.constraint;
        recordType.fields.put(field.name.value, field);

        recordType.tsymbol = Symbols.createRecordSymbol(0, Names.EMPTY, env.enclPkg.packageID,
                recordType, env.scope.owner);
        recordType.tsymbol.scope = new Scope(env.scope.owner);
        recordType.tsymbol.scope.define(fieldName, field.symbol);

        LinkedHashSet<BType> retTypeMembers = new LinkedHashSet<>();
        retTypeMembers.add(recordType);
        if (streamType.error != null) {
            retTypeMembers.add(streamType.error);
        }
        retTypeMembers.add(symTable.nilType);

        BUnionType unionType = BUnionType.create(null, retTypeMembers);
        unionType.tsymbol = Symbols.createTypeSymbol(SymTag.UNION_TYPE, 0, Names.EMPTY,
                env.enclPkg.symbol.pkgID, unionType, env.scope.owner);

        return unionType;
    }

    private boolean isValidInitInvocation(BLangTypeInit cIExpr, BObjectType objType) {

        if (!cIExpr.initInvocation.argExprs.isEmpty()
                && ((BObjectTypeSymbol) objType.tsymbol).initializerFunc == null) {
            dlog.error(cIExpr.pos, DiagnosticCode.TOO_MANY_ARGS_FUNC_CALL,
                       cIExpr.initInvocation.exprSymbol);
            cIExpr.initInvocation.argExprs.forEach(expr -> checkExpr(expr, env, symTable.noType));
            resultType = symTable.semanticError;
            return false;
        }
        return true;
    }

    private BType getObjectConstructorReturnType(BType objType, BType initRetType) {
        if (initRetType.tag == TypeTags.UNION) {
            LinkedHashSet<BType> retTypeMembers = new LinkedHashSet<>();
            retTypeMembers.add(objType);

            retTypeMembers.addAll(((BUnionType) initRetType).getMemberTypes());
            retTypeMembers.remove(symTable.nilType);

            BUnionType unionType = BUnionType.create(null, retTypeMembers);
            unionType.tsymbol = Symbols.createTypeSymbol(SymTag.UNION_TYPE, 0,
                                                         Names.EMPTY, env.enclPkg.symbol.pkgID, unionType,
                                                         env.scope.owner);
            return unionType;
        } else if (initRetType.tag == TypeTags.NIL) {
            return objType;
        }
        return symTable.semanticError;
    }

    private List<BType> findMembersWithMatchingInitFunc(BLangTypeInit cIExpr, BUnionType lhsUnionType) {
        boolean containsSingleObject = lhsUnionType.getMemberTypes().stream()
                .filter(x -> x.tag == TypeTags.OBJECT)
                .count() == 1;

        List<BType> matchingLhsMemberTypes = new ArrayList<>();
        for (BType memberType : lhsUnionType.getMemberTypes()) {
            if (memberType.tag != TypeTags.OBJECT) {
                // member is not an object.
                continue;
            }
            if ((memberType.tsymbol.flags & Flags.ABSTRACT) == Flags.ABSTRACT) {
                dlog.error(cIExpr.pos, DiagnosticCode.CANNOT_INITIALIZE_ABSTRACT_OBJECT,
                           lhsUnionType.tsymbol);
            }

            if (containsSingleObject) {
                return Collections.singletonList(memberType);
            }

            BAttachedFunction initializerFunc = ((BObjectTypeSymbol) memberType.tsymbol).initializerFunc;
            if (isArgsMatchesFunction(cIExpr.argsExpr, initializerFunc)) {
                matchingLhsMemberTypes.add(memberType);
            }
        }
        return matchingLhsMemberTypes;
    }

    private BType getMatchingType(List<BType> matchingLhsMembers, BLangTypeInit cIExpr, BType lhsUnion) {
        if (matchingLhsMembers.isEmpty()) {
            // No union type member found which matches with initializer expression.
            dlog.error(cIExpr.pos, DiagnosticCode.CANNOT_INFER_OBJECT_TYPE_FROM_LHS, lhsUnion);
            resultType = symTable.semanticError;
            return symTable.semanticError;
        } else if (matchingLhsMembers.size() == 1) {
            // We have a correct match.
            return matchingLhsMembers.get(0).tsymbol.type;
        } else {
            // Multiple matches found.
            dlog.error(cIExpr.pos, DiagnosticCode.AMBIGUOUS_TYPES, lhsUnion);
            resultType = symTable.semanticError;
            return symTable.semanticError;
        }
    }

    private boolean isArgsMatchesFunction(List<BLangExpression> invocationArguments, BAttachedFunction function) {
        invocationArguments.forEach(expr -> checkExpr(expr, env, symTable.noType));

        if (function == null) {
            return invocationArguments.isEmpty();
        }

        if (function.symbol.params.isEmpty() && invocationArguments.isEmpty()) {
            return true;
        }

        List<BLangNamedArgsExpression> namedArgs = new ArrayList<>();
        List<BLangExpression> positionalArgs = new ArrayList<>();
        for (BLangExpression argument : invocationArguments) {
            if (argument.getKind() == NodeKind.NAMED_ARGS_EXPR) {
                namedArgs.add((BLangNamedArgsExpression) argument);
            } else {
                positionalArgs.add(argument);
            }
        }

        List<BVarSymbol> requiredParams = function.symbol.params.stream()
                .filter(param -> !param.defaultableParam)
                .collect(Collectors.toList());
        // Given named and positional arguments are less than required parameters.
        if (requiredParams.size() > invocationArguments.size()) {
            return false;
        }

        List<BVarSymbol> defaultableParams = function.symbol.params.stream()
                .filter(param -> param.defaultableParam)
                .collect(Collectors.toList());

        int givenRequiredParamCount = 0;
        for (int i = 0; i < positionalArgs.size(); i++) {
            if (function.symbol.params.size() > i) {
                givenRequiredParamCount++;
                BVarSymbol functionParam = function.symbol.params.get(i);
                // check the type compatibility of positional args against function params.
                if (!types.isAssignable(positionalArgs.get(i).type, functionParam.type)) {
                    return false;
                }
                requiredParams.remove(functionParam);
                defaultableParams.remove(functionParam);
                continue;
            }

            if (function.symbol.restParam != null) {
                BType restParamType = ((BArrayType) function.symbol.restParam.type).eType;
                if (!types.isAssignable(positionalArgs.get(i).type, restParamType)) {
                    return false;
                }
                continue;
            }

            // additional positional args given for function with no rest param
            return false;
        }

        for (BLangNamedArgsExpression namedArg : namedArgs) {
            boolean foundNamedArg = false;
            // check the type compatibility of named args against function params.
            List<BVarSymbol> params = function.symbol.params;
            for (int i = givenRequiredParamCount; i < params.size(); i++) {
                BVarSymbol functionParam = params.get(i);
                if (!namedArg.name.value.equals(functionParam.name.value)) {
                    continue;
                }
                foundNamedArg = true;
                BType namedArgExprType = checkExpr(namedArg.expr, env);
                if (!types.isAssignable(functionParam.type, namedArgExprType)) {
                    // Name matched, type mismatched.
                    return false;
                }
                requiredParams.remove(functionParam);
                defaultableParams.remove(functionParam);
            }
            if (!foundNamedArg) {
                return false;
            }
        }

        // all required params are not given by positional or named args.
        return requiredParams.size() <= 0;
    }

    public void visit(BLangWaitForAllExpr waitForAllExpr) {
        switch (expType.tag) {
            case TypeTags.RECORD:
                checkTypesForRecords(waitForAllExpr);
                break;
            case TypeTags.MAP:
                checkTypesForMap(waitForAllExpr.keyValuePairs, ((BMapType) expType).constraint);
                LinkedHashSet<BType> memberTypesForMap = collectWaitExprTypes(waitForAllExpr.keyValuePairs);
                if (memberTypesForMap.size() == 1) {
                    resultType = new BMapType(TypeTags.MAP,
                            memberTypesForMap.iterator().next(), symTable.mapType.tsymbol);
                    break;
                }
                BUnionType constraintTypeForMap = BUnionType.create(null, memberTypesForMap);
                resultType = new BMapType(TypeTags.MAP, constraintTypeForMap, symTable.mapType.tsymbol);
                break;
            case TypeTags.NONE:
            case TypeTags.ANY:
                checkTypesForMap(waitForAllExpr.keyValuePairs, expType);
                LinkedHashSet<BType> memberTypes = collectWaitExprTypes(waitForAllExpr.keyValuePairs);
                if (memberTypes.size() == 1) {
                    resultType = new BMapType(TypeTags.MAP, memberTypes.iterator().next(), symTable.mapType.tsymbol);
                    break;
                }
                BUnionType constraintType = BUnionType.create(null, memberTypes);
                resultType = new BMapType(TypeTags.MAP, constraintType, symTable.mapType.tsymbol);
                break;
            default:
                dlog.error(waitForAllExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType,
                           getWaitForAllExprReturnType(waitForAllExpr.keyValuePairs));
                resultType = symTable.semanticError;
                break;
        }
        waitForAllExpr.type = resultType;

        if (resultType != null && resultType != symTable.semanticError) {
            types.setImplicitCastExpr(waitForAllExpr, waitForAllExpr.type, expType);
        }
    }

    private BRecordType getWaitForAllExprReturnType(List<BLangWaitForAllExpr.BLangWaitKeyValue> keyVals) {
        BRecordType retType = new BRecordType(null);

        for (BLangWaitForAllExpr.BLangWaitKeyValue keyVal : keyVals) {
            BLangIdentifier fieldName;
            if (keyVal.valueExpr == null || keyVal.valueExpr.getKind() != NodeKind.SIMPLE_VARIABLE_REF) {
                fieldName = keyVal.key;
            } else {
                fieldName = ((BLangSimpleVarRef) keyVal.valueExpr).variableName;
            }

            BSymbol symbol = symResolver.lookupSymbolInMainSpace(env, names.fromIdNode(fieldName));
            BType fieldType = symbol.type.tag == TypeTags.FUTURE ? ((BFutureType) symbol.type).constraint : symbol.type;
            BField field = new BField(names.fromIdNode(keyVal.key), null,
                                      new BVarSymbol(0, names.fromIdNode(keyVal.key), env.enclPkg.packageID,
                                                     fieldType, null));
            retType.fields.put(field.name.value, field);
        }

        retType.restFieldType = symTable.noType;
        retType.sealed = true;
        retType.tsymbol = Symbols.createRecordSymbol(0, Names.EMPTY, env.enclPkg.packageID, retType, null);
        return retType;
    }

    private LinkedHashSet<BType> collectWaitExprTypes(List<BLangWaitForAllExpr.BLangWaitKeyValue> keyVals) {
        LinkedHashSet<BType> memberTypes = new LinkedHashSet<>();
        for (BLangWaitForAllExpr.BLangWaitKeyValue keyVal : keyVals) {
            BType bType = keyVal.keyExpr != null ? keyVal.keyExpr.type : keyVal.valueExpr.type;
            if (bType.tag == TypeTags.FUTURE) {
                memberTypes.add(((BFutureType) bType).constraint);
            } else {
                memberTypes.add(bType);
            }
        }
        return memberTypes;
    }

    private void checkTypesForMap(List<BLangWaitForAllExpr.BLangWaitKeyValue> keyValuePairs, BType expType) {
        keyValuePairs.forEach(keyVal -> checkWaitKeyValExpr(keyVal, expType));
    }

    private void checkTypesForRecords(BLangWaitForAllExpr waitExpr) {
        List<BLangWaitForAllExpr.BLangWaitKeyValue> rhsFields = waitExpr.getKeyValuePairs();
        Map<String, BField> lhsFields = ((BRecordType) expType).fields;

        // check if the record is sealed, if so check if the fields in wait collection is more than the fields expected
        // by the lhs record
        if (((BRecordType) expType).sealed && rhsFields.size() > lhsFields.size()) {
            dlog.error(waitExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType,
                       getWaitForAllExprReturnType(waitExpr.keyValuePairs));
            resultType = symTable.semanticError;
            return;
        }

        for (BLangWaitForAllExpr.BLangWaitKeyValue keyVal : rhsFields) {
            String key = keyVal.key.value;
            if (!lhsFields.containsKey(key)) {
                // Check if the field is sealed if so you cannot have dynamic fields
                if (((BRecordType) expType).sealed) {
                    dlog.error(waitExpr.pos, DiagnosticCode.INVALID_FIELD_NAME_RECORD_LITERAL, key, expType);
                    resultType = symTable.semanticError;
                } else {
                    // Else if the record is an open record, then check if the rest field type matches the expression
                    BType restFieldType = ((BRecordType) expType).restFieldType;
                    checkWaitKeyValExpr(keyVal, restFieldType);
                }
            } else {
                checkWaitKeyValExpr(keyVal, lhsFields.get(key).type);
            }
        }
        // If the record literal is of record type and types are validated for the fields, check if there are any
        // required fields missing.
        checkMissingReqFieldsForWait(((BRecordType) expType), rhsFields, waitExpr.pos);

        if (symTable.semanticError != resultType) {
            resultType = expType;
        }
    }

    private void checkMissingReqFieldsForWait(BRecordType type, List<BLangWaitForAllExpr.BLangWaitKeyValue> keyValPairs,
                                              DiagnosticPos pos) {
        type.fields.values().forEach(field -> {
            // Check if `field` is explicitly assigned a value in the record literal
            boolean hasField = keyValPairs.stream().anyMatch(keyVal -> field.name.value.equals(keyVal.key.value));

            // If a required field is missing, it's a compile error
            if (!hasField && Symbols.isFlagOn(field.symbol.flags, Flags.REQUIRED)) {
                dlog.error(pos, DiagnosticCode.MISSING_REQUIRED_RECORD_FIELD, field.name);
            }
        });
    }

    private void checkWaitKeyValExpr(BLangWaitForAllExpr.BLangWaitKeyValue keyVal, BType type) {
        BLangExpression expr;
        if (keyVal.keyExpr != null) {
            BSymbol symbol = symResolver.lookupSymbolInMainSpace(env, names.fromIdNode
                    (((BLangSimpleVarRef) keyVal.keyExpr).variableName));
            keyVal.keyExpr.type = symbol.type;
            expr = keyVal.keyExpr;
        } else {
            expr = keyVal.valueExpr;
        }
        BFutureType futureType = new BFutureType(TypeTags.FUTURE, type, null);
        checkExpr(expr, env, futureType);
    }

    public void visit(BLangTernaryExpr ternaryExpr) {
        BType condExprType = checkExpr(ternaryExpr.expr, env, this.symTable.booleanType);

        SymbolEnv thenEnv = typeNarrower.evaluateTruth(ternaryExpr.expr, ternaryExpr.thenExpr, env);
        BType thenType = checkExpr(ternaryExpr.thenExpr, thenEnv, expType);

        SymbolEnv elseEnv = typeNarrower.evaluateFalsity(ternaryExpr.expr, ternaryExpr.elseExpr, env);
        BType elseType = checkExpr(ternaryExpr.elseExpr, elseEnv, expType);

        if (condExprType == symTable.semanticError || thenType == symTable.semanticError ||
                elseType == symTable.semanticError) {
            resultType = symTable.semanticError;
        } else if (expType == symTable.noType) {
            if (types.isAssignable(elseType, thenType)) {
                resultType = thenType;
            } else if (types.isAssignable(thenType, elseType)) {
                resultType = elseType;
            } else {
                dlog.error(ternaryExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES, thenType, elseType);
                resultType = symTable.semanticError;
            }
        } else {
            resultType = expType;
        }
    }

    public void visit(BLangWaitExpr waitExpr) {
        expType = new BFutureType(TypeTags.FUTURE, expType, null);
        checkExpr(waitExpr.getExpression(), env, expType);
        // Handle union types in lhs
        if (resultType.tag == TypeTags.UNION) {
            LinkedHashSet<BType> memberTypes = collectMemberTypes((BUnionType) resultType, new LinkedHashSet<>());
            if (memberTypes.size() == 1) {
                resultType = memberTypes.toArray(new BType[0])[0];
            } else {
                resultType = BUnionType.create(null, memberTypes);
            }
        } else if (resultType != symTable.semanticError) {
            // Handle other types except for semantic errors
            resultType = ((BFutureType) resultType).constraint;
        }
        waitExpr.type = resultType;

        if (resultType != null && resultType != symTable.semanticError) {
            types.setImplicitCastExpr(waitExpr, waitExpr.type, ((BFutureType) expType).constraint);
        }
    }

    private LinkedHashSet<BType> collectMemberTypes(BUnionType unionType, LinkedHashSet<BType> memberTypes) {
        for (BType memberType : unionType.getMemberTypes()) {
            if (memberType.tag == TypeTags.FUTURE) {
                memberTypes.add(((BFutureType) memberType).constraint);
            } else {
                memberTypes.add(memberType);
            }
        }
        return memberTypes;
    }

    @Override
    public void visit(BLangTrapExpr trapExpr) {
        boolean firstVisit = trapExpr.expr.type == null;
        BType actualType;
        BType exprType = checkExpr(trapExpr.expr, env, expType);
        boolean definedWithVar = expType == symTable.noType;

        if (trapExpr.expr.getKind() == NodeKind.WORKER_RECEIVE) {
            if (firstVisit) {
                isTypeChecked = false;
                resultType = expType;
                return;
            } else {
                expType = trapExpr.type;
                exprType = trapExpr.expr.type;
            }
        }

        if (expType == symTable.semanticError || exprType == symTable.semanticError) {
            actualType = symTable.semanticError;
        } else {
            LinkedHashSet<BType> resultTypes = new LinkedHashSet<>();
            if (exprType.tag == TypeTags.UNION) {
                resultTypes.addAll(((BUnionType) exprType).getMemberTypes());
            } else {
                resultTypes.add(exprType);
            }
            resultTypes.add(symTable.errorType);
            actualType = BUnionType.create(null, resultTypes);
        }

        resultType = types.checkType(trapExpr, actualType, expType);
        if (definedWithVar && resultType != null && resultType != symTable.semanticError) {
            types.setImplicitCastExpr(trapExpr.expr, trapExpr.expr.type, resultType);
        }
    }

    public void visit(BLangBinaryExpr binaryExpr) {
        // Bitwise operator should be applied for the future types in the wait expression
        if (expType.tag == TypeTags.FUTURE && binaryExpr.opKind == OperatorKind.BITWISE_OR) {
            BType lhsResultType = checkExpr(binaryExpr.lhsExpr, env, expType);
            BType rhsResultType = checkExpr(binaryExpr.rhsExpr, env, expType);
            // Return if both or atleast one of lhs and rhs types are errors
            if (lhsResultType == symTable.semanticError || rhsResultType == symTable.semanticError) {
                resultType = symTable.semanticError;
                return;
            }
            resultType = BUnionType.create(null, lhsResultType, rhsResultType);
            return;
        }

        checkDecimalCompatibilityForBinaryArithmeticOverLiteralValues(binaryExpr);

        SymbolEnv rhsExprEnv;
        BType lhsType = checkExpr(binaryExpr.lhsExpr, env);
        if (binaryExpr.opKind == OperatorKind.AND) {
            rhsExprEnv = typeNarrower.evaluateTruth(binaryExpr.lhsExpr, binaryExpr.rhsExpr, env, true);
        } else if (binaryExpr.opKind == OperatorKind.OR) {
            rhsExprEnv = typeNarrower.evaluateFalsity(binaryExpr.lhsExpr, binaryExpr.rhsExpr, env);
        } else {
            rhsExprEnv = env;
        }

        BType rhsType = checkExpr(binaryExpr.rhsExpr, rhsExprEnv);

        // Set error type as the actual type.
        BType actualType = symTable.semanticError;

        //noinspection SwitchStatementWithTooFewBranches
        switch (binaryExpr.opKind) {
            // Do not lookup operator symbol for xml sequence additions
            case ADD:
                BType leftConstituent = getXMLConstituents(lhsType);
                BType rightConstituent = getXMLConstituents(rhsType);

                if (leftConstituent != null && rightConstituent != null) {
                    actualType = new BXMLType(BUnionType.create(null, leftConstituent, rightConstituent), null);
                    break;
                }
                // Fall through
            default:
                if (lhsType != symTable.semanticError && rhsType != symTable.semanticError) {
                    // Look up operator symbol if both rhs and lhs types aren't error or xml types
                    BSymbol opSymbol = symResolver.resolveBinaryOperator(binaryExpr.opKind, lhsType, rhsType);

                    if (opSymbol == symTable.notFoundSymbol) {
                        opSymbol = symResolver.getBinaryEqualityForTypeSets(binaryExpr.opKind, lhsType, rhsType,
                                binaryExpr);
                    }

                    if (opSymbol == symTable.notFoundSymbol) {
                        dlog.error(binaryExpr.pos, DiagnosticCode.BINARY_OP_INCOMPATIBLE_TYPES, binaryExpr.opKind,
                                lhsType, rhsType);
                    } else {
                        if ((binaryExpr.opKind == OperatorKind.EQUAL || binaryExpr.opKind == OperatorKind.NOT_EQUAL) &&
                                (couldHoldTableValues(lhsType, new ArrayList<>()) &&
                                        couldHoldTableValues(rhsType, new ArrayList<>()))) {
                            dlog.error(binaryExpr.pos, DiagnosticCode.EQUALITY_NOT_YET_SUPPORTED, TABLE_TNAME);
                        }

                        binaryExpr.opSymbol = (BOperatorSymbol) opSymbol;
                        actualType = opSymbol.type.getReturnType();
                    }
                }
        }

        resultType = types.checkType(binaryExpr, actualType, expType);
    }

    private BType getXMLConstituents(BType type) {
        BType constituent = null;
        if (type.tag == TypeTags.XML) {
            constituent = ((BXMLType) type).constraint;
        } else if (TypeTags.isXMLNonSequenceType(type.tag)) {
            constituent = type;
        }
        return constituent;
    }

    private void checkDecimalCompatibilityForBinaryArithmeticOverLiteralValues(BLangBinaryExpr binaryExpr) {
        if (expType.tag != TypeTags.DECIMAL) {
            return;
        }

        switch (binaryExpr.opKind) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                checkExpr(binaryExpr.lhsExpr, env, expType);
                checkExpr(binaryExpr.rhsExpr, env, expType);
                break;
            default:
                break;
        }
    }

    public void visit(BLangElvisExpr elvisExpr) {
        BType lhsType = checkExpr(elvisExpr.lhsExpr, env);
        BType actualType = symTable.semanticError;
        if (lhsType != symTable.semanticError) {
            if (lhsType.tag == TypeTags.UNION && lhsType.isNullable()) {
                BUnionType unionType = (BUnionType) lhsType;
                LinkedHashSet<BType> memberTypes = unionType.getMemberTypes().stream()
                        .filter(type -> type.tag != TypeTags.NIL)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                if (memberTypes.size() == 1) {
                    actualType = memberTypes.toArray(new BType[0])[0];
                } else {
                    actualType = BUnionType.create(null, memberTypes);
                }
            } else {
                dlog.error(elvisExpr.pos, DiagnosticCode.OPERATOR_NOT_SUPPORTED, OperatorKind.ELVIS,
                           lhsType);
            }
        }
        BType rhsReturnType = checkExpr(elvisExpr.rhsExpr, env, expType);
        BType lhsReturnType = types.checkType(elvisExpr.lhsExpr.pos, actualType, expType,
                DiagnosticCode.INCOMPATIBLE_TYPES);
        if (rhsReturnType == symTable.semanticError || lhsReturnType == symTable.semanticError) {
            resultType = symTable.semanticError;
        } else if (expType == symTable.noType) {
            if (types.isSameType(rhsReturnType, lhsReturnType)) {
                resultType = lhsReturnType;
            } else {
                dlog.error(elvisExpr.rhsExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES, lhsReturnType,
                           rhsReturnType);
                resultType = symTable.semanticError;
            }
        } else {
            resultType = expType;
        }
    }

    @Override
    public void visit(BLangGroupExpr groupExpr) {
        resultType = checkExpr(groupExpr.expression, env, expType);
    }

    public void visit(BLangTypedescExpr accessExpr) {
        accessExpr.resolvedType = symResolver.resolveTypeNode(accessExpr.typeNode, env);
        int resolveTypeTag = accessExpr.resolvedType.tag;
        final BType actualType;

        if (resolveTypeTag != TypeTags.TYPEDESC && resolveTypeTag != TypeTags.NONE) {
            actualType = new BTypedescType(accessExpr.resolvedType, null);
        } else {
            actualType = accessExpr.resolvedType;
        }
        resultType = types.checkType(accessExpr, actualType, expType);
    }

    public void visit(BLangUnaryExpr unaryExpr) {
        BType exprType;
        BType actualType = symTable.semanticError;
        if (OperatorKind.UNTAINT.equals(unaryExpr.operator)) {
            exprType = checkExpr(unaryExpr.expr, env);
            if (exprType != symTable.semanticError) {
                actualType = exprType;
            }
        } else if (OperatorKind.TYPEOF.equals(unaryExpr.operator)) {
            exprType = checkExpr(unaryExpr.expr, env);
            if (exprType != symTable.semanticError) {
                actualType = new BTypedescType(exprType, null);
            }
        } else {
            exprType = OperatorKind.ADD.equals(unaryExpr.operator) ? checkExpr(unaryExpr.expr, env, expType) :
                    checkExpr(unaryExpr.expr, env);
            if (exprType != symTable.semanticError) {
                BSymbol symbol = symResolver.resolveUnaryOperator(unaryExpr.pos, unaryExpr.operator, exprType);
                if (symbol == symTable.notFoundSymbol) {
                    dlog.error(unaryExpr.pos, DiagnosticCode.UNARY_OP_INCOMPATIBLE_TYPES,
                               unaryExpr.operator, exprType);
                } else {
                    unaryExpr.opSymbol = (BOperatorSymbol) symbol;
                    actualType = symbol.type.getReturnType();
                }
            }
        }

        resultType = types.checkType(unaryExpr, actualType, expType);
    }

    public void visit(BLangTypeConversionExpr conversionExpr) {
        // Set error type as the actual type.
        BType actualType = symTable.semanticError;

        // Annotation such as <@untainted [T]>, where T is not provided,
        // it's merely a annotation on contextually expected type.
        BLangExpression expr = conversionExpr.expr;
        if (conversionExpr.typeNode == null && !conversionExpr.annAttachments.isEmpty()) {
            resultType = checkExpr(expr, env, this.expType);
            return;
        }

        BType targetType = symResolver.resolveTypeNode(conversionExpr.typeNode, env);
        conversionExpr.targetType = targetType;
        BType expType = requireTypeInference(expr, false) ? targetType : symTable.noType;
        BType sourceType = checkExpr(expr, env, expType);

        if (types.isTypeCastable(expr, sourceType, targetType)) {
            // We reach this block only if the cast is valid, so we set the target type as the actual type.
            actualType = targetType;
        } else {
            dlog.error(conversionExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES_CAST, sourceType, targetType);
        }
        resultType = types.checkType(conversionExpr, actualType, this.expType);
    }

    @Override
    public void visit(BLangLambdaFunction bLangLambdaFunction) {
        bLangLambdaFunction.type = bLangLambdaFunction.function.symbol.type;
        // creating a copy of the env to visit the lambda function later
        bLangLambdaFunction.capturedClosureEnv = env.createClone();
        env.enclPkg.lambdaFunctions.add(bLangLambdaFunction);
        resultType = types.checkType(bLangLambdaFunction, bLangLambdaFunction.type, expType);
    }

    @Override
    public void visit(BLangArrowFunction bLangArrowFunction) {
        BType expectedType = expType;
        if (expectedType.tag == TypeTags.UNION) {
            BUnionType unionType = (BUnionType) expectedType;
            BType invokableType = unionType.getMemberTypes().stream().filter(type -> type.tag == TypeTags.INVOKABLE)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                                if (list.size() != 1) {
                                    return null;
                                }
                                return list.get(0);
                            }
                    ));

            if (invokableType != null) {
                expectedType = invokableType;
            }
        }
        if (expectedType.tag != TypeTags.INVOKABLE) {
            dlog.error(bLangArrowFunction.pos,
                       DiagnosticCode.ARROW_EXPRESSION_CANNOT_INFER_TYPE_FROM_LHS);
            resultType = symTable.semanticError;
            return;
        }

        BInvokableType expectedInvocation = (BInvokableType) expectedType;
        populateArrowExprParamTypes(bLangArrowFunction, expectedInvocation.paramTypes);
        bLangArrowFunction.body.expr.type = populateArrowExprReturn(bLangArrowFunction, expectedInvocation.retType);
        // if function return type is none, assign the inferred return type
        if (expectedInvocation.retType.tag == TypeTags.NONE) {
            expectedInvocation.retType = bLangArrowFunction.body.expr.type;
        }
        resultType = bLangArrowFunction.funcType = expectedInvocation;
    }

    public void visit(BLangXMLQName bLangXMLQName) {
        String prefix = bLangXMLQName.prefix.value;
        resultType = types.checkType(bLangXMLQName, symTable.stringType, expType);
        // TODO: check isLHS

        if (env.node.getKind() == NodeKind.XML_ATTRIBUTE && prefix.isEmpty()
                && bLangXMLQName.localname.value.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            ((BLangXMLAttribute) env.node).isNamespaceDeclr = true;
            return;
        }

        if (env.node.getKind() == NodeKind.XML_ATTRIBUTE && prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            ((BLangXMLAttribute) env.node).isNamespaceDeclr = true;
            return;
        }

        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            dlog.error(bLangXMLQName.pos, DiagnosticCode.INVALID_NAMESPACE_PREFIX, prefix);
            bLangXMLQName.type = symTable.semanticError;
            return;
        }

        // XML attributes without a namespace prefix does not inherit default namespace
        // https://www.w3.org/TR/xml-names/#defaulting
        if (bLangXMLQName.prefix.value.isEmpty()) {
            return;
        }

        BSymbol xmlnsSymbol = symResolver.lookupSymbolInPrefixSpace(env, names.fromIdNode(bLangXMLQName.prefix));
        if (prefix.isEmpty() && xmlnsSymbol == symTable.notFoundSymbol) {
            return;
        }

        if (!prefix.isEmpty() && xmlnsSymbol == symTable.notFoundSymbol) {
            logUndefinedSymbolError(bLangXMLQName.pos, prefix);
            bLangXMLQName.type = symTable.semanticError;
            return;
        }

        if (xmlnsSymbol.getKind() == SymbolKind.PACKAGE) {
            xmlnsSymbol = findXMLNamespaceFromPackageConst(bLangXMLQName.localname.value, bLangXMLQName.prefix.value,
                    (BPackageSymbol) xmlnsSymbol, bLangXMLQName.pos);
        }

        if (xmlnsSymbol == null || xmlnsSymbol.getKind() != SymbolKind.XMLNS) {
            resultType = symTable.semanticError;
            return;
        }

        bLangXMLQName.nsSymbol = (BXMLNSSymbol) xmlnsSymbol;
        bLangXMLQName.namespaceURI = bLangXMLQName.nsSymbol.namespaceURI;
    }

    private BSymbol findXMLNamespaceFromPackageConst(String localname, String prefix,
                                                     BPackageSymbol pkgSymbol, DiagnosticPos pos) {
        // Resolve a const from module scope.
        BSymbol constSymbol = symResolver.lookupMemberSymbol(pos, pkgSymbol.scope, env,
                names.fromString(localname), SymTag.CONSTANT);
        if (constSymbol == symTable.notFoundSymbol) {
            if (!missingNodesHelper.isMissingNode(prefix) && !missingNodesHelper.isMissingNode(localname)) {
                dlog.error(pos, DiagnosticCode.UNDEFINED_SYMBOL, prefix + ":" + localname);
            }
            return null;
        }

        // If Resolved const is not a string, it is an error.
        BConstantSymbol constantSymbol = (BConstantSymbol) constSymbol;
        if (constantSymbol.literalType.tag != TypeTags.STRING) {
            dlog.error(pos, DiagnosticCode.INCOMPATIBLE_TYPES, symTable.stringType, constantSymbol.literalType);
            return null;
        }

        // If resolve const contain a string in {namespace url}local form extract namespace uri and local part.
        String constVal = (String) constantSymbol.value.value;
        int s = constVal.indexOf('{');
        int e = constVal.lastIndexOf('}');
        if (e > s + 1) {
            pkgSymbol.isUsed = true;
            String nsURI = constVal.substring(s + 1, e);
            String local = constVal.substring(e);
            return new BXMLNSSymbol(names.fromString(local), nsURI, constantSymbol.pkgID, constantSymbol.owner);
        }

        // Resolved const string is not in valid format.
        dlog.error(pos, DiagnosticCode.INVALID_ATTRIBUTE_REFERENCE, prefix + ":" + localname);
        return null;
    }

    public void visit(BLangXMLAttribute bLangXMLAttribute) {
        SymbolEnv xmlAttributeEnv = SymbolEnv.getXMLAttributeEnv(bLangXMLAttribute, env);

        // check attribute name
        BLangXMLQName name = (BLangXMLQName) bLangXMLAttribute.name;
        checkExpr(name, xmlAttributeEnv, symTable.stringType);
        // XML attributes without a prefix does not belong to enclosing elements default namespace.
        // https://www.w3.org/TR/xml-names/#uniqAttrs
        if (name.prefix.value.isEmpty()) {
            name.namespaceURI = null;
        }

        // check attribute value
        checkExpr(bLangXMLAttribute.value, xmlAttributeEnv, symTable.stringType);

        symbolEnter.defineNode(bLangXMLAttribute, env);
    }

    public void visit(BLangXMLElementLiteral bLangXMLElementLiteral) {
        SymbolEnv xmlElementEnv = SymbolEnv.getXMLElementEnv(bLangXMLElementLiteral, env);

        // Keep track of used namespace prefixes in this element and only add namespace attr for those used ones.
        Set<String> usedPrefixes = new HashSet<>();
        BLangIdentifier elemNamePrefix = ((BLangXMLQName) bLangXMLElementLiteral.startTagName).prefix;
        if (elemNamePrefix != null && !elemNamePrefix.value.isEmpty()) {
            usedPrefixes.add(elemNamePrefix.value);
        }

        // Visit in-line namespace declarations and define the namespace.
        for (BLangXMLAttribute attribute : bLangXMLElementLiteral.attributes) {
            if (attribute.name.getKind() == NodeKind.XML_QNAME && isXmlNamespaceAttribute(attribute)) {
                BLangXMLQuotedString value = attribute.value;
                if (value.getKind() == NodeKind.XML_QUOTED_STRING && value.textFragments.size() > 1) {
                    dlog.error(value.pos, DiagnosticCode.INVALID_XML_NS_INTERPOLATION);
                }
                checkExpr(attribute, xmlElementEnv, symTable.noType);
            }
            BLangIdentifier prefix = ((BLangXMLQName) attribute.name).prefix;
            if (prefix != null && !prefix.value.isEmpty()) {
                usedPrefixes.add(prefix.value);
            }
        }

        // Visit attributes, this may depend on the namespace defined in previous attribute iteration.
        bLangXMLElementLiteral.attributes.forEach(attribute -> {
            if (!(attribute.name.getKind() == NodeKind.XML_QNAME && isXmlNamespaceAttribute(attribute))) {
                checkExpr(attribute, xmlElementEnv, symTable.noType);
            }
        });

        Map<Name, BXMLNSSymbol> namespaces = symResolver.resolveAllNamespaces(xmlElementEnv);
        Name defaultNs = names.fromString(XMLConstants.DEFAULT_NS_PREFIX);
        if (namespaces.containsKey(defaultNs)) {
            bLangXMLElementLiteral.defaultNsSymbol = namespaces.remove(defaultNs);
        }
        for (Map.Entry<Name, BXMLNSSymbol> nsEntry : namespaces.entrySet()) {
            if (usedPrefixes.contains(nsEntry.getKey().value)) {
                bLangXMLElementLiteral.namespacesInScope.put(nsEntry.getKey(), nsEntry.getValue());
            }
        }

        // Visit the tag names
        validateTags(bLangXMLElementLiteral, xmlElementEnv);

        // Visit the children
        bLangXMLElementLiteral.modifiedChildren =
                concatSimilarKindXMLNodes(bLangXMLElementLiteral.children, xmlElementEnv);

        if (expType == symTable.noType) {
            resultType = types.checkType(bLangXMLElementLiteral, symTable.xmlElementType, expType);
            return;
        }

        resultType = checkXmlSubTypeLiteralCompatibility(bLangXMLElementLiteral.pos, symTable.xmlElementType,
                                                         this.expType);

        if (Symbols.isFlagOn(resultType.flags, Flags.READONLY)) {
            markChildrenAsImmutable(bLangXMLElementLiteral);
        }
    }

    private boolean isXmlNamespaceAttribute(BLangXMLAttribute attribute) {
        BLangXMLQName attrName = (BLangXMLQName) attribute.name;
        return (attrName.prefix.value.isEmpty()
                    && attrName.localname.value.equals(XMLConstants.XMLNS_ATTRIBUTE))
                || attrName.prefix.value.equals(XMLConstants.XMLNS_ATTRIBUTE);
    }

    public void visit(BLangXMLTextLiteral bLangXMLTextLiteral) {
        checkStringTemplateExprs(bLangXMLTextLiteral.textFragments, false);
        resultType = types.checkType(bLangXMLTextLiteral, symTable.xmlTextType, expType);
    }

    public void visit(BLangXMLCommentLiteral bLangXMLCommentLiteral) {
        checkStringTemplateExprs(bLangXMLCommentLiteral.textFragments, false);

        if (expType == symTable.noType) {
            resultType = types.checkType(bLangXMLCommentLiteral, symTable.xmlCommentType, expType);
            return;
        }
        resultType = checkXmlSubTypeLiteralCompatibility(bLangXMLCommentLiteral.pos, symTable.xmlCommentType,
                                                         this.expType);
    }

    public void visit(BLangXMLProcInsLiteral bLangXMLProcInsLiteral) {
        checkExpr(bLangXMLProcInsLiteral.target, env, symTable.stringType);
        checkStringTemplateExprs(bLangXMLProcInsLiteral.dataFragments, false);
        if (expType == symTable.noType) {
            resultType = types.checkType(bLangXMLProcInsLiteral, symTable.xmlPIType, expType);
            return;
        }
        resultType = checkXmlSubTypeLiteralCompatibility(bLangXMLProcInsLiteral.pos, symTable.xmlPIType, this.expType);
    }

    public void visit(BLangXMLQuotedString bLangXMLQuotedString) {
        checkStringTemplateExprs(bLangXMLQuotedString.textFragments, false);
        resultType = types.checkType(bLangXMLQuotedString, symTable.stringType, expType);
    }

    public void visit(BLangXMLAttributeAccess xmlAttributeAccessExpr) {
        dlog.error(xmlAttributeAccessExpr.pos,
                           DiagnosticCode.DEPRECATED_XML_ATTRIBUTE_ACCESS);
        resultType = symTable.semanticError;
    }

    public void visit(BLangStringTemplateLiteral stringTemplateLiteral) {
        checkStringTemplateExprs(stringTemplateLiteral.exprs, false);
        resultType = types.checkType(stringTemplateLiteral, symTable.stringType, expType);
    }

    @Override
    public void visit(BLangIntRangeExpression intRangeExpression) {
        checkExpr(intRangeExpression.startExpr, env, symTable.intType);
        checkExpr(intRangeExpression.endExpr, env, symTable.intType);
        resultType = new BArrayType(symTable.intType);
    }

    @Override
    public void visit(BLangRestArgsExpression bLangRestArgExpression) {
        resultType = checkExpr(bLangRestArgExpression.expr, env, expType);
    }

    @Override
    public void visit(BLangNamedArgsExpression bLangNamedArgsExpression) {
        resultType = checkExpr(bLangNamedArgsExpression.expr, env, expType);
        bLangNamedArgsExpression.type = bLangNamedArgsExpression.expr.type;
    }

    @Override
    public void visit(BLangMatchExpression bLangMatchExpression) {
        SymbolEnv matchExprEnv = SymbolEnv.createBlockEnv((BLangBlockStmt) TreeBuilder.createBlockNode(), env);
        checkExpr(bLangMatchExpression.expr, matchExprEnv);

        // Type check and resolve patterns and their expressions
        bLangMatchExpression.patternClauses.forEach(pattern -> {
            if (!pattern.variable.name.value.endsWith(Names.IGNORE.value)) {
                symbolEnter.defineNode(pattern.variable, matchExprEnv);
            }
            checkExpr(pattern.expr, matchExprEnv, expType);
            pattern.variable.type = symResolver.resolveTypeNode(pattern.variable.typeNode, matchExprEnv);
        });

        LinkedHashSet<BType> matchExprTypes = getMatchExpressionTypes(bLangMatchExpression);

        BType actualType;
        if (matchExprTypes.contains(symTable.semanticError)) {
            actualType = symTable.semanticError;
        } else if (matchExprTypes.size() == 1) {
            actualType = matchExprTypes.toArray(new BType[0])[0];
        } else {
            actualType = BUnionType.create(null, matchExprTypes);
        }

        resultType = types.checkType(bLangMatchExpression, actualType, expType);
    }

    @Override
    public void visit(BLangCheckedExpr checkedExpr) {
        visitCheckAndCheckPanicExpr(checkedExpr);
    }

    @Override
    public void visit(BLangCheckPanickedExpr checkedExpr) {
        visitCheckAndCheckPanicExpr(checkedExpr);
    }

    @Override
    public void visit(BLangQueryExpr queryExpr) {
        selectClause = queryExpr.getSelectClause();
        narrowedQueryEnv = env;
        List<BLangNode> clauses = queryExpr.getQueryClauses();
        BLangExpression collectionNode = (BLangExpression) ((BLangFromClause) clauses.get(0)).getCollection();
        clauses.forEach(clause -> clause.accept(this));
        BType actualType = findAssignableType(narrowedQueryEnv, selectClause.expression, collectionNode.type,
                expType, queryExpr);
        if (actualType != symTable.semanticError) {
            resultType = types.checkType(queryExpr.pos, actualType, expType, DiagnosticCode.INCOMPATIBLE_TYPES);
        } else {
            resultType = actualType;
        }
    }

    private BType findAssignableType(SymbolEnv env, BLangExpression selectExp, BType collectionType, BType targetType,
                                     BLangQueryExpr queryExpr) {
        List<BType> assignableSelectTypes = new ArrayList<>();
        BType actualType = symTable.semanticError;

        //type checks select type against expected element type
        Map<Boolean, List<BType>> resultTypeMap = types.getAllTypes(targetType).stream()
                .collect(Collectors.groupingBy(memberType -> (types.isAssignable(memberType, symTable.errorType) ||
                        (types.isAssignable(memberType, symTable.nilType)))));
        for (BType type : resultTypeMap.get(false)) {
            BType selectType;
            switch (type.tag) {
                case TypeTags.ARRAY:
                    selectType = checkExpr(selectExp, env, ((BArrayType) type).eType);
                    break;
                case TypeTags.TABLE:
                    selectType = checkExpr(selectExp, env, types.getSafeType(((BTableType) type).constraint,
                            true, true));
                    break;
                case TypeTags.STREAM:
                    selectType = checkExpr(selectExp, env, types.getSafeType(((BStreamType) type).constraint,
                            true, true));
                    break;
                default:
                    selectType = checkExpr(selectExp, env, type);
            }
            if (selectType != symTable.semanticError) {
                assignableSelectTypes.add(selectType);
            }
        }

        if (assignableSelectTypes.size() == 1) {
            actualType = assignableSelectTypes.get(0);
            if ((!queryExpr.isStream && !queryExpr.isTable)) {
                if (targetType.tag != TypeTags.STRING
                        && targetType.tag != TypeTags.XML) {
                    actualType = new BArrayType(actualType);
                }
            }
        } else if (assignableSelectTypes.size() > 1) {
            dlog.error(selectExp.pos, DiagnosticCode.AMBIGUOUS_TYPES, assignableSelectTypes);
            return actualType;
        } else {
            return actualType;
        }

        //checks whether iterable collection's next() method returns an error
        BType nextMethodReturnType = null;
        BType errorType = null;
        switch (collectionType.tag) {
            case TypeTags.STREAM:
                errorType = ((BStreamType) collectionType).error;
                break;
            case TypeTags.OBJECT:
                nextMethodReturnType = types.getVarTypeFromIterableObject((BObjectType) collectionType);
                break;
            default:
                BInvokableSymbol iteratorSymbol = (BInvokableSymbol) symResolver.lookupLangLibMethod(collectionType,
                        names.fromString(BLangCompilerConstants.ITERABLE_COLLECTION_ITERATOR_FUNC));
                nextMethodReturnType =
                        (BUnionType) types.getResultTypeOfNextInvocation((BObjectType) iteratorSymbol.retType);
        }

        if (nextMethodReturnType != null) {
            Map<Boolean, List<BType>> collectionTypeMap = types.getAllTypes(nextMethodReturnType).stream()
                    .collect(Collectors.groupingBy(memberType -> types.isAssignable(memberType, symTable.errorType)));
            List<BType> errorTypes = collectionTypeMap.get(true);
            if (errorTypes != null && !errorTypes.isEmpty()) {
                if (errorTypes.size() == 1) {
                    errorType = errorTypes.get(0);
                } else {
                    errorType = BUnionType.create(null, errorTypes.toArray(new BType[errorTypes.size()]));
                }
            }
        }

        if (queryExpr.isStream) {
            return new BStreamType(TypeTags.STREAM, actualType, errorType, symTable.streamType.tsymbol);
        } else if (queryExpr.isTable) {
            final BTableType tableType = new BTableType(TypeTags.TABLE, actualType, symTable.tableType.tsymbol);
            if (!queryExpr.fieldNameIdentifierList.isEmpty()) {
                tableType.fieldNameList = queryExpr.fieldNameIdentifierList.stream()
                        .map(identifier -> identifier.value).collect(Collectors.toList());
                return BUnionType.create(null, tableType, symTable.errorType);
            }
            return tableType;
        } else if (errorType != null) {
            return BUnionType.create(null, actualType, errorType);
        }
        return actualType;
    }

    @Override
    public void visit(BLangQueryAction queryAction) {
        narrowedQueryEnv = env;
        BLangDoClause doClause = queryAction.getDoClause();
        List<BLangNode> clauses = queryAction.getQueryClauses();
        clauses.forEach(clause -> clause.accept(this));
        SymbolEnv blockEnv = SymbolEnv.createBlockEnv(doClause.body, narrowedQueryEnv);
        // Analyze foreach node's statements.
        semanticAnalyzer.analyzeStmt(doClause.body, blockEnv);
        BType actualType = BUnionType.create(null, symTable.errorType, symTable.nilType);
        resultType = types.checkType(doClause.pos, actualType, expType,
                DiagnosticCode.INCOMPATIBLE_TYPES);
    }

    @Override
    public void visit(BLangFromClause fromClause) {
        checkExpr(fromClause.collection, narrowedQueryEnv);
        // Set the type of the foreach node's type node.
        types.setInputClauseTypedBindingPatternType(fromClause);
        narrowedQueryEnv = SymbolEnv.createTypeNarrowedEnv(fromClause, narrowedQueryEnv);
        handleInputClauseVariables(fromClause, narrowedQueryEnv);
    }

    @Override
    public void visit(BLangJoinClause joinClause) {
        checkExpr(joinClause.collection, narrowedQueryEnv);
        // Set the type of the foreach node's type node.
        types.setInputClauseTypedBindingPatternType(joinClause);
        narrowedQueryEnv = SymbolEnv.createTypeNarrowedEnv(joinClause, narrowedQueryEnv);
        handleInputClauseVariables(joinClause, narrowedQueryEnv);
    }

    @Override
    public void visit(BLangLetClause letClause) {
        narrowedQueryEnv = SymbolEnv.createTypeNarrowedEnv(letClause, narrowedQueryEnv);
        for (BLangLetVariable letVariable : letClause.letVarDeclarations) {
            semanticAnalyzer.analyzeDef((BLangNode) letVariable.definitionNode, narrowedQueryEnv);
        }
    }

    @Override
    public void visit(BLangWhereClause whereClause) {
        handleFilterClauses(whereClause.expression);
    }

    @Override
    public void visit(BLangSelectClause selectClause) {
    }

    @Override
    public void visit(BLangDoClause doClause) {
    }

    @Override
    public void visit(BLangOnConflictClause onConflictClause) {
        BType exprType = checkExpr(onConflictClause.expression, narrowedQueryEnv, symTable.errorType);
        if (!types.isAssignable(exprType, symTable.errorType)) {
            dlog.error(onConflictClause.expression.pos, DiagnosticCode.ERROR_TYPE_EXPECTED,
                    symTable.errorType, exprType);
        }
    }

    @Override
    public void visit(BLangLimitClause limitClause) {
        BType exprType = checkExpr(limitClause.expression, narrowedQueryEnv);
        if (!types.isAssignable(exprType, symTable.intType)) {
            dlog.error(limitClause.expression.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                    symTable.intType, exprType);
        }
    }

    @Override
    public void visit(BLangOnClause onClause) {
        handleFilterClauses(onClause.expression);
    }

    private void handleFilterClauses (BLangExpression filterExpression) {
        checkExpr(filterExpression, narrowedQueryEnv, symTable.booleanType);
        BType actualType = filterExpression.type;
        if (TypeTags.TUPLE == actualType.tag) {
            dlog.error(filterExpression.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                    symTable.booleanType, actualType);
        }
        narrowedQueryEnv = typeNarrower.evaluateTruth(filterExpression, selectClause, narrowedQueryEnv);
    }

    private void handleInputClauseVariables(BLangInputClause bLangInputClause, SymbolEnv blockEnv) {
        if (bLangInputClause.variableDefinitionNode == null) {
            //not-possible
            return;
        }

        BLangVariable variableNode = (BLangVariable) bLangInputClause.variableDefinitionNode.getVariable();
        // Check whether the foreach node's variables are declared with var.
        if (bLangInputClause.isDeclaredWithVar) {
            // If the foreach node's variables are declared with var, type is `varType`.
            semanticAnalyzer.handleDeclaredVarInForeach(variableNode, bLangInputClause.varType, blockEnv);
            return;
        }
        // If the type node is available, we get the type from it.
        BType typeNodeType = symResolver.resolveTypeNode(variableNode.typeNode, blockEnv);
        // Then we need to check whether the RHS type is assignable to LHS type.
        if (types.isAssignable(bLangInputClause.varType, typeNodeType)) {
            // If assignable, we set types to the variables.
            semanticAnalyzer.handleDeclaredVarInForeach(variableNode, bLangInputClause.varType, blockEnv);
            return;
        }
        // Log an error and define a symbol with the node's type to avoid undeclared symbol errors.
        if (typeNodeType != symTable.semanticError) {
            dlog.error(variableNode.typeNode.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                    bLangInputClause.varType, typeNodeType);
        }
        semanticAnalyzer.handleDeclaredVarInForeach(variableNode, typeNodeType, blockEnv);
    }

    private void visitCheckAndCheckPanicExpr(BLangCheckedExpr checkedExpr) {
        String operatorType = checkedExpr.getKind() == NodeKind.CHECK_EXPR ? "check" : "checkpanic";
        boolean firstVisit = checkedExpr.expr.type == null;
        BType exprExpType;
        if (expType == symTable.noType) {
            exprExpType = symTable.noType;
        } else {
            exprExpType = BUnionType.create(null, expType, symTable.errorType);
        }

        BType exprType = checkExpr(checkedExpr.expr, env, exprExpType);
        if (checkedExpr.expr.getKind() == NodeKind.WORKER_RECEIVE) {
            if (firstVisit) {
                isTypeChecked = false;
                resultType = expType;
                return;
            } else {
                expType = checkedExpr.type;
                exprType = checkedExpr.expr.type;
            }
        }

        if (exprType.tag != TypeTags.UNION) {
            if (types.isAssignable(exprType, symTable.errorType)) {
                dlog.error(checkedExpr.expr.pos,
                           DiagnosticCode.CHECKED_EXPR_INVALID_USAGE_ALL_ERROR_TYPES_IN_RHS,
                           operatorType);
            } else if (exprType != symTable.semanticError) {
                dlog.error(checkedExpr.expr.pos,
                           DiagnosticCode.CHECKED_EXPR_INVALID_USAGE_NO_ERROR_TYPE_IN_RHS,
                           operatorType);
            }
            checkedExpr.type = symTable.semanticError;
            return;
        }

        BUnionType unionType = (BUnionType) exprType;
        // Filter out the list of types which are not equivalent with the error type.
        Map<Boolean, List<BType>> resultTypeMap = unionType.getMemberTypes().stream()
                .collect(Collectors.groupingBy(memberType -> types.isAssignable(memberType, symTable.errorType)));

        // This list will be used in the desugar phase
        checkedExpr.equivalentErrorTypeList = resultTypeMap.get(true);
        if (checkedExpr.equivalentErrorTypeList == null ||
                checkedExpr.equivalentErrorTypeList.size() == 0) {
            // No member types in this union is equivalent to the error type
            dlog.error(checkedExpr.expr.pos,
                       DiagnosticCode.CHECKED_EXPR_INVALID_USAGE_NO_ERROR_TYPE_IN_RHS, operatorType);
            checkedExpr.type = symTable.semanticError;
            return;
        }

        List<BType> nonErrorTypeList = resultTypeMap.get(false);
        if (nonErrorTypeList == null || nonErrorTypeList.size() == 0) {
            // All member types in the union are equivalent to the error type.
            // Checked expression requires at least one type which is not equivalent to the error type.
            dlog.error(checkedExpr.expr.pos,
                       DiagnosticCode.CHECKED_EXPR_INVALID_USAGE_ALL_ERROR_TYPES_IN_RHS, operatorType);
            checkedExpr.type = symTable.semanticError;
            return;
        }

        BType actualType;
        if (nonErrorTypeList.size() == 1) {
            actualType = nonErrorTypeList.get(0);
        } else {
            actualType = BUnionType.create(null, new LinkedHashSet<>(nonErrorTypeList));
        }

        resultType = types.checkType(checkedExpr, actualType, expType);
    }

    @Override
    public void visit(BLangServiceConstructorExpr serviceConstructorExpr) {
        resultType = serviceConstructorExpr.serviceNode.symbol.type;
    }

    @Override
    public void visit(BLangTypeTestExpr typeTestExpr) {
        typeTestExpr.typeNode.type = symResolver.resolveTypeNode(typeTestExpr.typeNode, env);
        checkExpr(typeTestExpr.expr, env);

        resultType = types.checkType(typeTestExpr, symTable.booleanType, expType);
    }

    public void visit(BLangAnnotAccessExpr annotAccessExpr) {
        checkExpr(annotAccessExpr.expr, this.env, symTable.typeDesc);

        BType actualType = symTable.semanticError;
        BSymbol symbol =
                this.symResolver.resolveAnnotation(annotAccessExpr.pos, env,
                        names.fromString(annotAccessExpr.pkgAlias.getValue()),
                        names.fromString(annotAccessExpr.annotationName.getValue()));
        if (symbol == this.symTable.notFoundSymbol) {
            this.dlog.error(annotAccessExpr.pos, DiagnosticCode.UNDEFINED_ANNOTATION,
                            annotAccessExpr.annotationName.getValue());
        } else {
            annotAccessExpr.annotationSymbol = (BAnnotationSymbol) symbol;
            BType annotType = ((BAnnotationSymbol) symbol).attachedType == null ? symTable.trueType :
                    ((BAnnotationSymbol) symbol).attachedType.type;
            actualType = BUnionType.create(null, annotType, symTable.nilType);
        }

        this.resultType = this.types.checkType(annotAccessExpr, actualType, this.expType);
    }

    // Private methods

    private boolean isValidVariableReference(BLangExpression varRef) {
        switch (varRef.getKind()) {
            case SIMPLE_VARIABLE_REF:
            case RECORD_VARIABLE_REF:
            case TUPLE_VARIABLE_REF:
            case ERROR_VARIABLE_REF:
            case FIELD_BASED_ACCESS_EXPR:
            case INDEX_BASED_ACCESS_EXPR:
            case XML_ATTRIBUTE_ACCESS_EXPR:
                return true;
            default:
                dlog.error(varRef.pos, DiagnosticCode.INVALID_RECORD_BINDING_PATTERN, varRef.type);
                return false;
        }
    }

    private BType populateArrowExprReturn(BLangArrowFunction bLangArrowFunction, BType expectedRetType) {
        SymbolEnv arrowFunctionEnv = SymbolEnv.createArrowFunctionSymbolEnv(bLangArrowFunction, env);
        bLangArrowFunction.params.forEach(param -> symbolEnter.defineNode(param, arrowFunctionEnv));
        return checkExpr(bLangArrowFunction.body.expr, arrowFunctionEnv, expectedRetType);
    }

    private void populateArrowExprParamTypes(BLangArrowFunction bLangArrowFunction, List<BType> paramTypes) {
        if (paramTypes.size() != bLangArrowFunction.params.size()) {
            dlog.error(bLangArrowFunction.pos,
                       DiagnosticCode.ARROW_EXPRESSION_MISMATCHED_PARAMETER_LENGTH,
                       paramTypes.size(), bLangArrowFunction.params.size());
            resultType = symTable.semanticError;
            bLangArrowFunction.params.forEach(param -> param.type = symTable.semanticError);
            return;
        }

        for (int i = 0; i < bLangArrowFunction.params.size(); i++) {
            BLangSimpleVariable paramIdentifier = bLangArrowFunction.params.get(i);
            BType bType = paramTypes.get(i);
            BLangValueType valueTypeNode = (BLangValueType) TreeBuilder.createValueTypeNode();
            valueTypeNode.setTypeKind(bType.getKind());
            paramIdentifier.setTypeNode(valueTypeNode);
            paramIdentifier.type = bType;
        }
    }

    private void checkSefReferences(DiagnosticPos pos, SymbolEnv env, BVarSymbol varSymbol) {
        if (env.enclVarSym == varSymbol) {
            dlog.error(pos, DiagnosticCode.SELF_REFERENCE_VAR, varSymbol.name);
        }
    }

    public List<BType> getListWithErrorTypes(int count) {
        List<BType> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(symTable.semanticError);
        }

        return list;
    }

    private void checkFunctionInvocationExpr(BLangInvocation iExpr) {
        Name funcName = names.fromIdNode(iExpr.name);
        Name pkgAlias = names.fromIdNode(iExpr.pkgAlias);
        BSymbol funcSymbol = symTable.notFoundSymbol;

        BSymbol pkgSymbol = symResolver.resolvePrefixSymbol(env, pkgAlias, getCurrentCompUnit(iExpr));
        if (pkgSymbol == symTable.notFoundSymbol) {
            dlog.error(iExpr.pos, DiagnosticCode.UNDEFINED_MODULE, pkgAlias);
        } else {
            if (funcSymbol == symTable.notFoundSymbol) {
                BSymbol symbol = symResolver.lookupMainSpaceSymbolInPackage(iExpr.pos, env, pkgAlias, funcName);
                if ((symbol.tag & SymTag.VARIABLE) == SymTag.VARIABLE) {
                    funcSymbol = symbol;
                }
                if (symTable.rootPkgSymbol.pkgID.equals(symbol.pkgID) &&
                        (symbol.tag & SymTag.VARIABLE_NAME) == SymTag.VARIABLE_NAME) {
                    funcSymbol = symbol;
                }
            }
            if (funcSymbol == symTable.notFoundSymbol) {
                funcSymbol = symResolver.lookupConstructorSpaceSymbolInPackage(iExpr.pos, env, pkgAlias, funcName);
            }
        }

        if ((funcSymbol.tag & SymTag.ERROR) == SymTag.ERROR
            || ((funcSymbol.tag & SymTag.CONSTRUCTOR) == SymTag.CONSTRUCTOR && funcSymbol.type.tag == TypeTags.ERROR)) {
            iExpr.symbol = funcSymbol;
            iExpr.type = funcSymbol.type;
            checkErrorConstructorInvocation(iExpr);
            return;
        } else if (funcSymbol == symTable.notFoundSymbol || isNotFunction(funcSymbol)) {
            dlog.error(iExpr.pos, DiagnosticCode.UNDEFINED_FUNCTION, funcName);
            iExpr.argExprs.forEach(arg -> checkExpr(arg, env));
            resultType = symTable.semanticError;
            return;
        }
        if (isFunctionPointer(funcSymbol)) {
            iExpr.functionPointerInvocation = true;
            markAndRegisterClosureVariable(funcSymbol, iExpr.pos);
        }
        if (Symbols.isFlagOn(funcSymbol.flags, Flags.REMOTE)) {
            dlog.error(iExpr.pos, DiagnosticCode.INVALID_ACTION_INVOCATION_SYNTAX, iExpr.name.value);
        }
        if (Symbols.isFlagOn(funcSymbol.flags, Flags.RESOURCE)) {
            dlog.error(iExpr.pos, DiagnosticCode.INVALID_RESOURCE_FUNCTION_INVOCATION);
        }

        if (PackageID.isLangLibPackageID(pkgSymbol.pkgID)) {
            // This will enable, type param support, if the function is called directly.
            this.env = SymbolEnv.createInvocationEnv(iExpr, this.env);
        }
        // Set the resolved function symbol in the invocation expression.
        // This is used in the code generation phase.
        iExpr.symbol = funcSymbol;
        checkInvocationParamAndReturnType(iExpr);
    }

    private void markAndRegisterClosureVariable(BSymbol symbol, DiagnosticPos pos) {
        BLangInvokableNode encInvokable = env.enclInvokable;
        if (symbol.owner instanceof BPackageSymbol) {
            return;
        }
        if (encInvokable != null && encInvokable.flagSet.contains(Flag.LAMBDA)
                && !isFunctionArgument(symbol, encInvokable.requiredParams)) {
            SymbolEnv encInvokableEnv = findEnclosingInvokableEnv(env, encInvokable);
            BSymbol resolvedSymbol = symResolver.lookupClosureVarSymbol(encInvokableEnv, symbol.name, SymTag.VARIABLE);
            if (resolvedSymbol != symTable.notFoundSymbol && !encInvokable.flagSet.contains(Flag.ATTACHED)) {
                resolvedSymbol.closure = true;
                ((BLangFunction) encInvokable).closureVarSymbols.add(new ClosureVarSymbol(resolvedSymbol, pos));
            }
        }
        if (env.node.getKind() == NodeKind.ARROW_EXPR
                && !isFunctionArgument(symbol, ((BLangArrowFunction) env.node).params)) {
            SymbolEnv encInvokableEnv = findEnclosingInvokableEnv(env, encInvokable);
            BSymbol resolvedSymbol = symResolver.lookupClosureVarSymbol(encInvokableEnv, symbol.name, SymTag.VARIABLE);
            if (resolvedSymbol != symTable.notFoundSymbol) {
                resolvedSymbol.closure = true;
                ((BLangArrowFunction) env.node).closureVarSymbols.add(new ClosureVarSymbol(resolvedSymbol, pos));
            }
        }
        if (env.enclType != null && env.enclType.getKind() == NodeKind.RECORD_TYPE) {
            SymbolEnv encInvokableEnv = findEnclosingInvokableEnv(env, (BLangRecordTypeNode) env.enclType);
            BSymbol resolvedSymbol = symResolver.lookupClosureVarSymbol(encInvokableEnv, symbol.name, SymTag.VARIABLE);
            if (resolvedSymbol != symTable.notFoundSymbol && !encInvokable.flagSet.contains(Flag.ATTACHED)) {
                resolvedSymbol.closure = true;
                ((BLangFunction) encInvokable).closureVarSymbols.add(new ClosureVarSymbol(resolvedSymbol, pos));
            }
        }

        // Iterate through parent nodes until a function node is met to find if the variable is used inside
        // a transaction block to mark it as a closure, blocks inside transactions are desugared into functions later.
        BLangNode node = env.node;
        SymbolEnv cEnv = env;
        while (node != null && node.getKind() != NodeKind.FUNCTION) {
            if (node.getKind() == NodeKind.TRANSACTION) {
                SymbolEnv encInvokableEnv = findEnclosingInvokableEnv(env, encInvokable);
                BSymbol resolvedSymbol = symResolver.lookupClosureVarSymbol(encInvokableEnv, symbol.name,
                        SymTag.VARIABLE);
                if (resolvedSymbol != symTable.notFoundSymbol) {
                    resolvedSymbol.closure = true;
                }
                break;
            } else {
                SymbolEnv enclEnv = cEnv.enclEnv;
                if (enclEnv == null) {
                    break;
                }
                cEnv = enclEnv;
                node = cEnv.node;
            }
        }
    }

    private boolean isNotFunction(BSymbol funcSymbol) {
        if ((funcSymbol.tag & SymTag.FUNCTION) == SymTag.FUNCTION
                || (funcSymbol.tag & SymTag.CONSTRUCTOR) == SymTag.CONSTRUCTOR) {
            return false;
        }

        if (isFunctionPointer(funcSymbol)) {
            return false;
        }

        return true;
    }

    private boolean isFunctionPointer(BSymbol funcSymbol) {
        if ((funcSymbol.tag & SymTag.FUNCTION) == SymTag.FUNCTION) {
            return false;
        }
        return (funcSymbol.tag & SymTag.FUNCTION) == SymTag.VARIABLE
                && funcSymbol.kind == SymbolKind.FUNCTION
                && (funcSymbol.flags & Flags.NATIVE) != Flags.NATIVE;
    }

    private void checkErrorConstructorInvocation(BLangInvocation iExpr) {
        BType expectedType = this.expType;

        if (expType.getKind() == TypeKind.UNION) {
            BType[] errorMembers = ((BUnionType) expectedType).getMemberTypes()
                    .stream()
                    .filter(memberType -> types.isAssignable(memberType, symTable.errorType))
                    .toArray(BType[]::new);
            if (errorMembers.length > 0) {
                expectedType = BUnionType.create(null, errorMembers);
            }
        }

        if (expType.getKind() == TypeKind.UNION && iExpr.symbol.type == symTable.errorType) {
            BUnionType unionType = (BUnionType) expType;
            long count = unionType.getMemberTypes().stream()
                    .filter(member -> types.isAssignable(iExpr.symbol.type, member))
                    .count();
            // More than one compatible members found, this is ambiguous.
            if (count > 1) {
                dlog.error(iExpr.pos, DiagnosticCode.CANNOT_INFER_ERROR_TYPE, this.expType);
                return;
            }
        } else if (!types.isAssignable(expectedType, symTable.errorType)) {
            if ((iExpr.symbol.tag & SymTag.CONSTRUCTOR) == SymTag.CONSTRUCTOR) {
                expectedType = iExpr.type;
            } else if (expectedType != symTable.noType) {
                // Cannot infer error type from error constructor. 'T1|T2|T3 e = error("r", a="b", b="c");
                dlog.error(iExpr.pos, DiagnosticCode.CANNOT_INFER_ERROR_TYPE, this.expType);
                resultType = symTable.semanticError;
                return;
            } else {
                // var e = <error> error("r");
                expectedType = symTable.errorType;
            }
        }

        BErrorType expectedError = getExpectedErrorType(iExpr.pos, expectedType, iExpr.symbol);
        if (expectedError == null) {
            return;
        }

        if (iExpr.argExprs.isEmpty() && iExpr.requiredArgs.isEmpty() && checkNoArgErrorCtorInvocation(expectedError,
                iExpr.name, iExpr.pos)) {
            return;
        }

        if (nonNamedArgsGiven(iExpr) && (iExpr.symbol.tag & SymTag.CONSTRUCTOR) == SymTag.CONSTRUCTOR) {
            dlog.error(iExpr.argExprs.get(0).pos, DiagnosticCode.INDIRECT_ERROR_CTOR_REASON_NOT_ALLOWED);
            resultType = symTable.semanticError;
            return;
        }

        boolean reasonArgGiven = checkErrorReasonArg(iExpr, expectedError);

        if (expectedError.detailType.tag == TypeTags.RECORD) {
            BRecordType targetErrorDetailRec = (BRecordType) expectedError.detailType;
            BRecordType recordType = createErrorDetailRecordType(iExpr, reasonArgGiven, targetErrorDetailRec);
            if (resultType == symTable.semanticError) {
                return;
            }

            if (!types.isAssignable(recordType, targetErrorDetailRec)) {
                dlog.error(iExpr.pos, DiagnosticCode.INVALID_ERROR_CONSTRUCTOR_DETAIL, iExpr);
                resultType = symTable.semanticError;
                return;
            }
        } else {
            // This is when there is a semantic error in error type, bail out!
            return;
        }
        setErrorReasonParam(iExpr, reasonArgGiven, expectedError);
        setErrorDetailArgsToNamedArgsList(iExpr);

        resultType = expectedError;
        if (iExpr.symbol == symTable.errorType.tsymbol) {
            iExpr.symbol = ((BErrorTypeSymbol) expectedError.tsymbol).ctorSymbol;
        }
    }

    private BErrorType getExpectedErrorType(DiagnosticPos pos, BType expType, BSymbol iExprSymbol) {
        // Direct error ctor invocation.
        if (iExprSymbol == symTable.errorType.tsymbol) {
            if (expType.tag == TypeTags.UNION) {
                List<BType> matchedErrors = ((BUnionType) expType).getMemberTypes().stream()
                        .filter(m -> types.isAssignable(m, iExprSymbol.type))
                        .collect(Collectors.toList());
                if (matchedErrors.size() == 1) {
                    return (BErrorType) matchedErrors.get(0);
                } else {
                    // More than one matched, Cannot infer error type from error constructor.
                    // 'Error0|Error1|T3 e = error(...);
                    dlog.error(pos, DiagnosticCode.CANNOT_INFER_ERROR_TYPE, expType);
                    resultType = symTable.semanticError;
                    return null;
                }
            }
            return (BErrorType) expType;
        } else {
            // Indirect error constructor.
            return (BErrorType) iExprSymbol.type;
        }
    }

    private boolean nonNamedArgsGiven(BLangInvocation iExpr) {
        return iExpr.argExprs.stream().anyMatch(arg -> arg.getKind() != NodeKind.NAMED_ARGS_EXPR);
    }

    private boolean checkErrorReasonArg(BLangInvocation iExpr, BErrorType ctorType) {
        // User defined error
        if (iExpr.type != symTable.errorType) {
            if (ctorType.reasonType.getKind() != TypeKind.FINITE) {
                dlog.error(iExpr.pos, DiagnosticCode.INDIRECT_ERROR_CTOR_NOT_ALLOWED_ON_NON_CONST_REASON,
                           iExpr.type);
                return false;
            } else {
                BFiniteType reasonType = (BFiniteType) ctorType.reasonType;
                if (reasonType.getValueSpace().size() > 1) {
                    dlog.error(iExpr.pos,
                               DiagnosticCode.INDIRECT_ERROR_CTOR_NOT_ALLOWED_ON_NON_CONST_REASON,
                               iExpr.type);
                    return false;
                }
            }
        }
        if (iExpr.argExprs.isEmpty()) {
            return false;
        }

        // if present, error reason should be the first and only positional argument to error constructor.
        BLangExpression firstErrorArg = iExpr.argExprs.get(0);
        if (firstErrorArg.getKind() != NodeKind.NAMED_ARGS_EXPR) {
            checkExpr(firstErrorArg, env, ctorType.reasonType, DiagnosticCode.INVALID_ERROR_REASON_TYPE);
            return true;
        } else if (iExpr.type == symTable.errorType) {
            dlog.error(iExpr.pos, DiagnosticCode.DIRECT_ERROR_CTOR_REASON_NOT_PROVIDED);
        }
        return false;
    }

    private boolean checkNoArgErrorCtorInvocation(BErrorType errorType, BLangIdentifier name, DiagnosticPos pos) {
        if (errorType.reasonType.tag != TypeTags.FINITE) {
            dlog.error(pos, DiagnosticCode.INDIRECT_ERROR_CTOR_NOT_ALLOWED_ON_NON_CONST_REASON, name);
            resultType = symTable.semanticError;
            return true;
        } else {
            BFiniteType finiteType = (BFiniteType) errorType.reasonType;
            if (finiteType.getValueSpace().size() != 1) {
                if (errorType == symTable.errorType) {
                    dlog.error(pos, DiagnosticCode.CANNOT_INFER_ERROR_TYPE, expType.tsymbol.name);
                } else {
                    dlog.error(pos, DiagnosticCode.INDIRECT_ERROR_CTOR_NOT_ALLOWED_ON_NON_CONST_REASON,
                               expType.tsymbol.name);
                }
                resultType = symTable.semanticError;
                return true;
            }
        }
        return false;
    }

    private void setErrorDetailArgsToNamedArgsList(BLangInvocation iExpr) {
        List<BLangExpression> namedArgPositions = new ArrayList<>(iExpr.argExprs.size());
        for (int i = 0; i < iExpr.argExprs.size(); i++) {
            BLangExpression argExpr = iExpr.argExprs.get(i);
            checkExpr(argExpr, env, symTable.pureType);
            if (argExpr.getKind() == NodeKind.NAMED_ARGS_EXPR) {
                iExpr.requiredArgs.add(argExpr);
                namedArgPositions.add(argExpr);
            } else {
                dlog.error(argExpr.pos, DiagnosticCode.ERROR_DETAIL_ARG_IS_NOT_NAMED_ARG);
                resultType = symTable.semanticError;
            }
        }

        for (BLangExpression expr : namedArgPositions) {
            // This check is to filter out additional field assignments when Error detail type is a open record.
            iExpr.argExprs.remove(expr);
        }
    }

    private void setErrorReasonParam(BLangInvocation iExpr, boolean reasonArgGiven, BErrorType ctorType) {
        if (!reasonArgGiven && ctorType.reasonType.getKind() == TypeKind.FINITE) {
            BFiniteType finiteType = (BFiniteType) ctorType.reasonType;
            BLangExpression reasonExpr = (BLangExpression) finiteType.getValueSpace().toArray()[0];
            iExpr.requiredArgs.add(reasonExpr);
            return;
        }
        if (!iExpr.argExprs.isEmpty()) {
            iExpr.requiredArgs.add(iExpr.argExprs.get(0));
            iExpr.argExprs.remove(0);
        }
    }

    /**
     * Create a error detail record using all metadata from {@code targetErrorDetailsType} and put actual error details
     * from {@code iExpr} expression.
     *
     * @param iExpr error constructor invocation
     * @param reasonArgGiven error reason is provided as first argument
     * @param targetErrorDetailsType target error details type to extract metadata such as pkgId from
     * @return error detail record
     */
    // todo: try to re-use recrod literal checking
    private BRecordType createErrorDetailRecordType(BLangInvocation iExpr, boolean reasonArgGiven,
                                                    BRecordType targetErrorDetailsType) {
        List<BLangNamedArgsExpression> namedArgs = getProvidedErrorDetails(iExpr, reasonArgGiven);
        if (namedArgs == null) {
            // error in provided error details
            return null;
        }
        BRecordTypeSymbol recordTypeSymbol = new BRecordTypeSymbol(
                SymTag.RECORD, targetErrorDetailsType.tsymbol.flags, Names.EMPTY, targetErrorDetailsType.tsymbol.pkgID,
                symTable.recordType, null);
        BRecordType recordType = new BRecordType(recordTypeSymbol);
        recordType.sealed = targetErrorDetailsType.sealed;
        recordType.restFieldType = targetErrorDetailsType.restFieldType;

        Set<Name> availableErrorDetailFields = new HashSet<>();
        for (BLangNamedArgsExpression arg : namedArgs) {
            Name fieldName = names.fromIdNode(arg.name);
            BField field = new BField(fieldName, arg.pos, new BVarSymbol(0, fieldName, null, arg.type, null));
            recordType.fields.put(field.name.value, field);
            availableErrorDetailFields.add(fieldName);
        }

        for (BField field : targetErrorDetailsType.fields.values()) {
            boolean notRequired = (field.symbol.flags & Flags.REQUIRED) != Flags.REQUIRED;
            if (notRequired && !availableErrorDetailFields.contains(field.name)) {
                BField defaultableField = new BField(field.name, iExpr.pos,
                                                     new BVarSymbol(field.symbol.flags, field.name, null, field.type,
                                                                    null));
                recordType.fields.put(defaultableField.name.value, defaultableField);
            }
        }

        return recordType;
    }

    private List<BLangNamedArgsExpression> getProvidedErrorDetails(BLangInvocation iExpr, boolean reasonArgGiven) {
        List<BLangNamedArgsExpression> namedArgs = new ArrayList<>();
        for (int i = reasonArgGiven ? 1 : 0; i < iExpr.argExprs.size(); i++) {
            BLangExpression argExpr = iExpr.argExprs.get(i);
            checkExpr(argExpr, env);
            if (argExpr.getKind() != NodeKind.NAMED_ARGS_EXPR) {
                dlog.error(argExpr.pos, DiagnosticCode.ERROR_DETAIL_ARG_IS_NOT_NAMED_ARG);
                resultType = symTable.semanticError;
                return null;
            }

            namedArgs.add((BLangNamedArgsExpression) argExpr);
        }
        return namedArgs;
    }

    private void checkObjectFunctionInvocationExpr(BLangInvocation iExpr, BObjectType objectType) {
        if (objectType.getKind() == TypeKind.SERVICE &&
                !(iExpr.expr.getKind() == NodeKind.SIMPLE_VARIABLE_REF &&
                (Names.SELF.equals(((BLangSimpleVarRef) iExpr.expr).symbol.name)))) {
            dlog.error(iExpr.pos, DiagnosticCode.SERVICE_FUNCTION_INVALID_INVOCATION);
            return;
        }
        // check for object attached function
        Name funcName =
                names.fromString(Symbols.getAttachedFuncSymbolName(objectType.tsymbol.name.value, iExpr.name.value));
        BSymbol funcSymbol =
                symResolver.resolveObjectMethod(iExpr.pos, env, funcName, (BObjectTypeSymbol) objectType.tsymbol);
        if (funcSymbol == symTable.notFoundSymbol || funcSymbol.type.tag != TypeTags.INVOKABLE) {
            if (!checkLangLibMethodInvocationExpr(iExpr, objectType)) {
                dlog.error(iExpr.name.pos, DiagnosticCode.UNDEFINED_METHOD_IN_OBJECT, iExpr.name.value,
                           objectType);
                resultType = symTable.semanticError;
                return;
            }
        } else {
            iExpr.symbol = funcSymbol;
        }

        // __init method can be called in a method-call-expr only when the expression
        // preceding the . is self
        if (iExpr.name.value.equals(Names.USER_DEFINED_INIT_SUFFIX.value) &&
                !(iExpr.expr.getKind() == NodeKind.SIMPLE_VARIABLE_REF &&
                (Names.SELF.equals(((BLangSimpleVarRef) iExpr.expr).symbol.name)))) {
            dlog.error(iExpr.pos, DiagnosticCode.INVALID_INIT_INVOCATION);
        }

        if (Symbols.isFlagOn(funcSymbol.flags, Flags.REMOTE)) {
            dlog.error(iExpr.pos, DiagnosticCode.INVALID_ACTION_INVOCATION_SYNTAX, iExpr.name.value);
        }
        if (Symbols.isFlagOn(funcSymbol.flags, Flags.RESOURCE)) {
            dlog.error(iExpr.pos, DiagnosticCode.INVALID_RESOURCE_FUNCTION_INVOCATION);
        }
        checkInvocationParamAndReturnType(iExpr);
    }

    // Here, an action invocation can be either of the following two forms:
    // - foo->bar();
    // - start foo.bar(); or start foo->bar()
    private void checkActionInvocation(BLangInvocation.BLangActionInvocation aInv, BObjectType expType) {
        BLangVariableReference varRef = (BLangVariableReference) aInv.expr;

        if (((varRef.symbol.tag & SymTag.ENDPOINT) != SymTag.ENDPOINT) && !aInv.async) {
            dlog.error(aInv.pos, DiagnosticCode.INVALID_ACTION_INVOCATION, varRef.type);
            this.resultType = symTable.semanticError;
            return;
        }

        BVarSymbol epSymbol = (BVarSymbol) varRef.symbol;

        Name remoteMethodQName = names
                .fromString(Symbols.getAttachedFuncSymbolName(expType.tsymbol.name.value, aInv.name.value));
        Name actionName = names.fromIdNode(aInv.name);
        BSymbol remoteFuncSymbol = symResolver
                .lookupMemberSymbol(aInv.pos, ((BObjectTypeSymbol) epSymbol.type.tsymbol).methodScope, env,
                                    remoteMethodQName, SymTag.FUNCTION);

        if (remoteFuncSymbol == symTable.notFoundSymbol && !checkLangLibMethodInvocationExpr(aInv, expType)) {
            dlog.error(aInv.name.pos, DiagnosticCode.UNDEFINED_METHOD_IN_OBJECT, aInv.name.value, expType);
            resultType = symTable.semanticError;
            return;
        }

        if (!Symbols.isFlagOn(remoteFuncSymbol.flags, Flags.REMOTE) && aInv.remoteMethodCall) {
            dlog.error(aInv.pos, DiagnosticCode.INVALID_METHOD_INVOCATION_SYNTAX, actionName);
            this.resultType = symTable.semanticError;
            return;
        }

        aInv.symbol = remoteFuncSymbol;
        checkInvocationParamAndReturnType(aInv);
    }

    private boolean checkLangLibMethodInvocationExpr(BLangInvocation iExpr, BType bType) {

        Name funcName = names.fromString(iExpr.name.value);
        BSymbol funcSymbol = symResolver.lookupLangLibMethod(bType, funcName);

        if (funcSymbol == symTable.notFoundSymbol) {
            return false;
        }

        iExpr.symbol = funcSymbol;
        iExpr.langLibInvocation = true;
        SymbolEnv enclEnv = this.env;
        this.env = SymbolEnv.createInvocationEnv(iExpr, this.env);
        if (iExpr.argExprs.isEmpty() || !iExpr.argExprs.get(0).equals(iExpr.expr)) {
            iExpr.argExprs.add(0, iExpr.expr);
        }
        checkInvocationParamAndReturnType(iExpr);
        this.env = enclEnv;

        return true;
    }

    private void checkInvocationParamAndReturnType(BLangInvocation iExpr) {
        BType actualType = checkInvocationParam(iExpr);
        resultType = types.checkType(iExpr, actualType, this.expType);
    }

    private BType checkInvocationParam(BLangInvocation iExpr) {
        if (iExpr.symbol.type.tag != TypeTags.INVOKABLE) {
            dlog.error(iExpr.pos, DiagnosticCode.INVALID_FUNCTION_INVOCATION, iExpr.symbol.type);
            return symTable.noType;
        }

        List<BType> paramTypes = ((BInvokableType) iExpr.symbol.type).getParameterTypes();
        Map<String, BVarSymbol> params = new HashMap<>();
        for (BVarSymbol a : ((BInvokableSymbol) iExpr.symbol).params) {
            if (!a.name.equals(Names.EMPTY)) {
                params.put(a.name.getValue(), a);
            }
        }

        int parameterCount = paramTypes.size();
        iExpr.requiredArgs = new ArrayList<>();

        // Split the different argument types: required args, named args and rest args
        int i = 0;
        BLangExpression vararg = null;
        boolean foundNamedArg = false;
        for (BLangExpression expr : iExpr.argExprs) {
            switch (expr.getKind()) {
                case NAMED_ARGS_EXPR:
                    BVarSymbol varSymbol = params.get(((BLangNamedArgsExpression) expr).name.value);
                    if (!env.enclPkg.packageID.equals(iExpr.symbol.pkgID)
                            && (varSymbol != null && !Symbols.isFlagOn(varSymbol.flags, Flags.PUBLIC))) {
                        // can not provide a named arg, if the arg is not public and the caller is not from same package
                        dlog.error(expr.pos, DiagnosticCode.NON_PUBLIC_ARG_ACCESSED_WITH_NAMED_ARG,
                                   ((BLangNamedArgsExpression) expr).name.value, iExpr.toString());
                    }
                    foundNamedArg = true;
                    if (i < parameterCount) {
                        iExpr.requiredArgs.add(expr);
                    } else {
                        // can not provide a rest parameters as named args
                        dlog.error(expr.pos, DiagnosticCode.TOO_MANY_ARGS_FUNC_CALL, iExpr.name.value);
                    }
                    i++;
                    break;
                case REST_ARGS_EXPR:
                    if (foundNamedArg) {
                        dlog.error(expr.pos, DiagnosticCode.REST_ARG_DEFINED_AFTER_NAMED_ARG);
                        continue;
                    }
                    vararg = expr;
                    break;
                default: // positional args
                    if (foundNamedArg) {
                        dlog.error(expr.pos, DiagnosticCode.POSITIONAL_ARG_DEFINED_AFTER_NAMED_ARG);
                    }
                    if (i < parameterCount) {
                        iExpr.requiredArgs.add(expr);
                    } else {
                        iExpr.restArgs.add(expr);
                    }
                    i++;
                    break;
            }
        }

        return checkInvocationArgs(iExpr, paramTypes, vararg);
    }

    private BType checkInvocationArgs(BLangInvocation iExpr, List<BType> paramTypes, BLangExpression vararg) {
        BInvokableSymbol invokableSymbol = (BInvokableSymbol) iExpr.symbol;
        BInvokableType bInvokableType = (BInvokableType) invokableSymbol.type;
        BInvokableTypeSymbol invokableTypeSymbol = (BInvokableTypeSymbol) bInvokableType.tsymbol;
        List<BVarSymbol> nonRestParams = new ArrayList<>(invokableTypeSymbol.params);

        List<BLangExpression> nonRestArgs = iExpr.requiredArgs;
        List<BVarSymbol> valueProvidedParams = new ArrayList<>();

        List<BVarSymbol> requiredParams = new ArrayList<>();

        for (BVarSymbol nonRestParam : nonRestParams) {
            if (nonRestParam.defaultableParam) {
                continue;
            }

            requiredParams.add(nonRestParam);
        }

        int i = 0;
        for (; i < nonRestArgs.size(); i++) {
            BLangExpression arg = nonRestArgs.get(i);
            BType expectedType = paramTypes.get(i);

            // Special case handling for the first param because for parameterized invocations, we have added the
            // value on which the function is invoked as the first param of the function call. If we run checkExpr()
            // on it, it will recursively add the first param to argExprs again, resulting in a too many args in
            // function call error.
            if (i == 0 && arg.typeChecked && iExpr.expr != null && iExpr.expr == arg) {
                types.checkType(arg.pos, arg.type, expectedType, DiagnosticCode.INCOMPATIBLE_TYPES);
                types.setImplicitCastExpr(arg, arg.type, expectedType);
            }

            if (arg.getKind() != NodeKind.NAMED_ARGS_EXPR) {
                // if arg is positional, corresponding parameter in the same position should be of same type.
                if (i < nonRestParams.size()) {
                    BVarSymbol param = nonRestParams.get(i);
                    checkTypeParamExpr(arg, this.env, param.type, iExpr.langLibInvocation);
                    valueProvidedParams.add(param);
                    requiredParams.remove(param);
                    continue;
                }
                // Arg count > required + defaultable param count.
                break;
            }

            if (arg.getKind() == NodeKind.NAMED_ARGS_EXPR) {
                // if arg is named, function should have a parameter with this name.
                BLangIdentifier argName = ((NamedArgNode) arg).getName();
                BVarSymbol varSym = null;

                for (BVarSymbol nonRestParam : nonRestParams) {
                    if (nonRestParam.getName().value.equals(argName.value)) {
                        varSym = nonRestParam;
                    }
                }

                if (varSym == null) {
                    dlog.error(arg.pos, DiagnosticCode.UNDEFINED_PARAMETER, argName);
                    break;
                }
                requiredParams.remove(varSym);
                if (valueProvidedParams.contains(varSym)) {
                    dlog.error(arg.pos, DiagnosticCode.DUPLICATE_NAMED_ARGS, varSym.name.value);
                    continue;
                }
                checkTypeParamExpr(arg, this.env, varSym.type, iExpr.langLibInvocation);
                valueProvidedParams.add(varSym);
            }
        }

        BVarSymbol restParam = invokableTypeSymbol.restParam;

        boolean errored = false;

        if (!requiredParams.isEmpty() && vararg == null) {
            // Log errors if any required parameters are not given as positional/named args and there is
            // no vararg either.
            for (BVarSymbol requiredParam : requiredParams) {
                dlog.error(iExpr.pos, DiagnosticCode.MISSING_REQUIRED_PARAMETER, requiredParam.name,
                           iExpr.name.value);
            }
            errored = true;
        }

        if (restParam == null &&
                (!iExpr.restArgs.isEmpty() ||
                         (vararg != null && valueProvidedParams.size() == nonRestParams.size()))) {
            dlog.error(iExpr.pos, DiagnosticCode.TOO_MANY_ARGS_FUNC_CALL, iExpr.name.value);
            errored = true;
        }

        if (errored) {
            return symTable.semanticError;
        }

        BType restType = restParam == null ? null : restParam.type;

        if (nonRestArgs.size() < nonRestParams.size() && vararg != null) {
            // We only reach here if there are no named args and there is a vararg, and part of the non-rest params
            // are provided via the vararg.
            // Create a new tuple type as the expected rest param type with expected required/defaultable param types
            // as members.
            List<BType> tupleMemberTypes = new ArrayList<>();
            BType tupleRestType = null;

            for (int j = nonRestArgs.size(); j < nonRestParams.size(); j++) {
                tupleMemberTypes.add(paramTypes.get(j));
            }

            if (restType != null) {
                if (restType.tag == TypeTags.ARRAY) {
                    tupleRestType = ((BArrayType) restType).eType;
                } else if (restType.tag == TypeTags.TUPLE) {
                    BTupleType restTupleType = (BTupleType) restType;
                    tupleMemberTypes.addAll(restTupleType.tupleTypes);
                    if (restTupleType.restType != null) {
                        tupleRestType = restTupleType.restType;
                    }
                }
            }

            BTupleType tupleType = new BTupleType(tupleMemberTypes);
            tupleType.restType = tupleRestType;
            restType = tupleType;
        }

        // Check whether the expected param count and the actual args counts are matching.
        if (restType == null && (vararg != null || !iExpr.restArgs.isEmpty())) {
            dlog.error(iExpr.pos, DiagnosticCode.TOO_MANY_ARGS_FUNC_CALL, iExpr.name.value);
            return symTable.semanticError;
        }

        if (vararg != null && !iExpr.restArgs.isEmpty()) {
            // We reach here if args are provided for the rest param as both individual rest args and a vararg.
            // Thus, the rest param type is the original rest param type which is an array type.
            BType elementType = ((BArrayType) restType).eType;

            for (BLangExpression restArg : iExpr.restArgs) {
                checkTypeParamExpr(restArg, this.env, elementType, true);
            }

            checkTypeParamExpr(vararg, this.env, restType, iExpr.langLibInvocation);
            iExpr.restArgs.add(vararg);
        } else if (vararg != null) {
            checkTypeParamExpr(vararg, this.env, restType, iExpr.langLibInvocation);
            iExpr.restArgs.add(vararg);
        } else if (!iExpr.restArgs.isEmpty()) {
            if (restType.tag == TypeTags.ARRAY) {
                BType elementType = ((BArrayType) restType).eType;
                for (BLangExpression restArg : iExpr.restArgs) {
                    checkTypeParamExpr(restArg, this.env, elementType, true);
                }
            } else {
                BTupleType tupleType = (BTupleType) restType;
                List<BType> tupleMemberTypes = tupleType.tupleTypes;
                BType tupleRestType = tupleType.restType;

                int tupleMemCount = tupleMemberTypes.size();

                for (int j = 0; j < iExpr.restArgs.size(); j++) {
                    BLangExpression restArg = iExpr.restArgs.get(j);
                    BType memType = j < tupleMemCount ? tupleMemberTypes.get(j) : tupleRestType;
                    checkTypeParamExpr(restArg, this.env, memType, true);
                }
            }
        }

        BType retType = typeParamAnalyzer.getReturnTypeParams(env, bInvokableType.getReturnType());

        if (iExpr instanceof ActionNode && ((BLangInvocation.BLangActionInvocation) iExpr).async) {
            return this.generateFutureType(invokableSymbol, retType);
        } else {
            return retType;
        }
    }

    private BFutureType generateFutureType(BInvokableSymbol invocableSymbol, BType retType) {

        boolean isWorkerStart = invocableSymbol.name.value.startsWith(WORKER_LAMBDA_VAR_PREFIX);
        return new BFutureType(TypeTags.FUTURE, retType, null, isWorkerStart);
    }

    private void checkTypeParamExpr(BLangExpression arg, SymbolEnv env, BType expectedType,
                                    boolean inferTypeForNumericLiteral) {
        checkTypeParamExpr(arg.pos, arg, env, expectedType, inferTypeForNumericLiteral);
    }

    private void checkTypeParamExpr(DiagnosticPos pos, BLangExpression arg, SymbolEnv env, BType expectedType,
                                    boolean inferTypeForNumericLiteral) {

        if (typeParamAnalyzer.notRequireTypeParams(env)) {
            checkExpr(arg, env, expectedType);
            return;
        }
        if (requireTypeInference(arg, inferTypeForNumericLiteral)) {
            // Need to infer the type. Calculate matching bound type, with no type.
            BType expType = typeParamAnalyzer.getMatchingBoundType(expectedType, env);
            BType inferredType = checkExpr(arg, env, expType);
            typeParamAnalyzer.checkForTypeParamsInArg(pos, inferredType, this.env, expectedType);
            return;
        }
        checkExpr(arg, env, expectedType);
        typeParamAnalyzer.checkForTypeParamsInArg(pos, arg.type, this.env, expectedType);
    }

    private boolean requireTypeInference(BLangExpression expr, boolean inferTypeForNumericLiteral) {

        switch (expr.getKind()) {
            case GROUP_EXPR:
                return requireTypeInference(((BLangGroupExpr) expr).expression, inferTypeForNumericLiteral);
            case ARROW_EXPR:
            case LIST_CONSTRUCTOR_EXPR:
            case RECORD_LITERAL_EXPR:
                return true;
            case NUMERIC_LITERAL:
                return inferTypeForNumericLiteral;
            default:
                return false;
        }
    }

    private BType checkMappingField(RecordLiteralNode.RecordField field, BType mappingType) {
        BType fieldType = symTable.semanticError;
        boolean keyValueField = field.isKeyValueField();
        boolean spreadOpField = field.getKind() == NodeKind.RECORD_LITERAL_SPREAD_OP;

        BLangExpression valueExpr = null;

        if (keyValueField) {
            valueExpr = ((BLangRecordKeyValueField) field).valueExpr;
        } else if (!spreadOpField) {
            valueExpr = (BLangRecordLiteral.BLangRecordVarNameField) field;
        }

        switch (mappingType.tag) {
            case TypeTags.RECORD:
                if (keyValueField) {
                    BLangRecordKey key = ((BLangRecordKeyValueField) field).key;
                    fieldType = checkRecordLiteralKeyExpr(key.expr, key.computedKey, (BRecordType) mappingType);
                } else if (spreadOpField) {
                    BLangExpression spreadExpr = ((BLangRecordLiteral.BLangRecordSpreadOperatorField) field).expr;
                    checkExpr(spreadExpr, this.env);

                    BType spreadExprType = spreadExpr.type;
                    if (spreadExprType.tag == TypeTags.MAP) {
                        return types.checkType(spreadExpr.pos, ((BMapType) spreadExprType).constraint,
                                               getAllFieldType((BRecordType) mappingType),
                                               DiagnosticCode.INCOMPATIBLE_TYPES);
                    }

                    if (spreadExprType.tag != TypeTags.RECORD) {
                        dlog.error(spreadExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES_SPREAD_OP,
                                   spreadExprType);
                        return symTable.semanticError;
                    }

                    boolean errored = false;
                    for (BField bField : ((BRecordType) spreadExprType).fields.values()) {
                        BType specFieldType = bField.type;
                        BType expectedFieldType = checkRecordLiteralKeyByName(spreadExpr.pos, this.env, bField.name,
                                                                              (BRecordType) mappingType);
                        if (expectedFieldType != symTable.semanticError &&
                                !types.isAssignable(specFieldType, expectedFieldType)) {
                            dlog.error(spreadExpr.pos, DiagnosticCode.INCOMPATIBLE_TYPES_FIELD,
                                       expectedFieldType, bField.name, specFieldType);
                            if (!errored) {
                                errored = true;
                            }
                        }
                    }
                    return errored ? symTable.semanticError : symTable.noType;
                } else {
                    fieldType = checkRecordLiteralKeyExpr((BLangRecordLiteral.BLangRecordVarNameField) field, false,
                                                          (BRecordType) mappingType);
                }
                break;
            case TypeTags.MAP:
                if (spreadOpField) {
                    BLangExpression spreadExp = ((BLangRecordLiteral.BLangRecordSpreadOperatorField) field).expr;
                    BType spreadOpType = checkExpr(spreadExp, this.env);
                    BType spreadOpMemberType;

                    switch (spreadOpType.tag) {
                        case TypeTags.RECORD:
                            List<BType> types = new ArrayList<>();
                            BRecordType recordType = (BRecordType) spreadOpType;

                            for (BField recField : recordType.fields.values()) {
                                types.add(recField.type);
                            }

                            if (!recordType.sealed) {
                                types.add(recordType.restFieldType);
                            }

                            spreadOpMemberType = getRepresentativeBroadType(types);
                            break;
                        case TypeTags.MAP:
                            spreadOpMemberType = ((BMapType) spreadOpType).constraint;
                            break;
                        default:
                            dlog.error(spreadExp.pos, DiagnosticCode.INCOMPATIBLE_TYPES_SPREAD_OP,
                                       spreadOpType);
                            return symTable.semanticError;
                    }

                    return types.checkType(spreadExp.pos, spreadOpMemberType, ((BMapType) mappingType).constraint,
                                           DiagnosticCode.INCOMPATIBLE_TYPES);
                }

                boolean validMapKey;
                if (keyValueField) {
                    BLangRecordKey key = ((BLangRecordKeyValueField) field).key;
                    validMapKey = checkValidJsonOrMapLiteralKeyExpr(key.expr, key.computedKey);
                } else {
                    validMapKey = checkValidJsonOrMapLiteralKeyExpr((BLangRecordLiteral.BLangRecordVarNameField) field,
                                                                    false);
                }

                fieldType = validMapKey ? ((BMapType) mappingType).constraint : symTable.semanticError;
                break;
        }


        if (spreadOpField) {
            // If we reach this point for a spread operator it is due to the mapping type being a semantic error.
            // In such a scenario, valueExpr would be null here, and fieldType would be symTable.semanticError.
            // We set the spread op expression as the valueExpr here, to check it against symTable.semanticError.
            valueExpr = ((BLangRecordLiteral.BLangRecordSpreadOperatorField) field).expr;
        }

        BLangExpression exprToCheck = valueExpr;
        if (this.nonErrorLoggingCheck) {
            valueExpr.cloneAttempt++;
            exprToCheck = nodeCloner.clone(valueExpr);
        } else {
            ((BLangNode) field).type = fieldType;
        }

        return checkExpr(exprToCheck, this.env, fieldType);
    }

    private BType checkRecordLiteralKeyExpr(BLangExpression keyExpr, boolean computedKey, BRecordType recordType) {
        Name fieldName;

        if (computedKey) {
            checkExpr(keyExpr, this.env, symTable.stringType);

            if (keyExpr.type == symTable.semanticError) {
                return symTable.semanticError;
            }

            LinkedHashSet<BType> fieldTypes = recordType.fields.values().stream()
                    .map(field -> field.type)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (recordType.restFieldType.tag != TypeTags.NONE) {
                fieldTypes.add(recordType.restFieldType);
            }

            return BUnionType.create(null, fieldTypes);
        } else if (keyExpr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
            BLangSimpleVarRef varRef = (BLangSimpleVarRef) keyExpr;
            fieldName = names.fromIdNode(varRef.variableName);
        } else if (keyExpr.getKind() == NodeKind.LITERAL && ((BLangLiteral) keyExpr).type.tag == TypeTags.STRING) {
            fieldName = names.fromString((String) ((BLangLiteral) keyExpr).value);
        } else {
            dlog.error(keyExpr.pos, DiagnosticCode.INVALID_RECORD_LITERAL_KEY);
            return symTable.semanticError;
        }

        // Check whether the struct field exists
        return checkRecordLiteralKeyByName(keyExpr.pos, this.env, fieldName, recordType);
    }

    private BType checkRecordLiteralKeyByName(DiagnosticPos pos, SymbolEnv env, Name key, BRecordType recordType) {
        BSymbol fieldSymbol = symResolver.resolveStructField(pos, env, key, recordType.tsymbol);
        if (fieldSymbol != symTable.notFoundSymbol) {
            return fieldSymbol.type;
        }

        if (recordType.sealed) {
            dlog.error(pos, DiagnosticCode.UNDEFINED_STRUCTURE_FIELD_WITH_TYPE, key,
                       recordType.tsymbol.type.getKind().typeName(), recordType);
            return symTable.semanticError;
        }

        return recordType.restFieldType;
    }

    private BType getAllFieldType(BRecordType recordType) {
        LinkedHashSet<BType> possibleTypes = new LinkedHashSet<>();

        for (BField field : recordType.fields.values()) {
            possibleTypes.add(field.type);
        }

        BType restFieldType = recordType.restFieldType;

        if (restFieldType != null && restFieldType != symTable.noType) {
            possibleTypes.add(restFieldType);
        }

        return BUnionType.create(null, possibleTypes);
    }

    private boolean checkValidJsonOrMapLiteralKeyExpr(BLangExpression keyExpr, boolean computedKey) {
        if (computedKey) {
            checkExpr(keyExpr, this.env, symTable.stringType);

            if (keyExpr.type == symTable.semanticError) {
                return false;
            }
            return true;
        } else if (keyExpr.getKind() == NodeKind.SIMPLE_VARIABLE_REF ||
                (keyExpr.getKind() == NodeKind.LITERAL && ((BLangLiteral) keyExpr).type.tag == TypeTags.STRING)) {
            return true;
        }
        dlog.error(keyExpr.pos, DiagnosticCode.INVALID_RECORD_LITERAL_KEY);
        return false;
    }

    private BType addNilForNillableAccessType(BType actualType) {
        // index based map/record access always returns a nil-able type for optional/rest fields.
        if (actualType.isNullable()) {
            return actualType;
        }

        return BUnionType.create(null, actualType, symTable.nilType);
    }

    private BType checkRecordRequiredFieldAccess(BLangVariableReference varReferExpr, Name fieldName,
                                                 BRecordType recordType) {
        BSymbol fieldSymbol = symResolver.resolveStructField(varReferExpr.pos, this.env, fieldName, recordType.tsymbol);

        if (fieldSymbol == symTable.notFoundSymbol || Symbols.isOptional(fieldSymbol)) {
            return symTable.semanticError;
        }

        // Set the field symbol to use during the code generation phase.
        varReferExpr.symbol = fieldSymbol;
        return fieldSymbol.type;
    }

    private BType checkRecordOptionalFieldAccess(BLangVariableReference varReferExpr, Name fieldName,
                                                 BRecordType recordType) {
        BSymbol fieldSymbol = symResolver.resolveStructField(varReferExpr.pos, this.env, fieldName, recordType.tsymbol);

        if (fieldSymbol == symTable.notFoundSymbol || !Symbols.isOptional(fieldSymbol)) {
            return symTable.semanticError;
        }

        // Set the field symbol to use during the code generation phase.
        varReferExpr.symbol = fieldSymbol;
        return fieldSymbol.type;
    }

    private BType checkRecordRestFieldAccess(BLangVariableReference varReferExpr, Name fieldName,
                                             BRecordType recordType) {
        BSymbol fieldSymbol = symResolver.resolveStructField(varReferExpr.pos, this.env, fieldName, recordType.tsymbol);

        if (fieldSymbol != symTable.notFoundSymbol) {
            // The field should not exist as a required or optional field.
            return symTable.semanticError;
        }

        if (recordType.sealed) {
            return symTable.semanticError;
        }

        return recordType.restFieldType;
    }

    private BType checkObjectFieldAccess(BLangFieldBasedAccess bLangFieldBasedAccess,
                                         Name fieldName, BObjectType objectType) {
        BSymbol fieldSymbol = symResolver.resolveStructField(bLangFieldBasedAccess.pos,
                this.env, fieldName, objectType.tsymbol);

        if (fieldSymbol != symTable.notFoundSymbol) {
            // Setting the field symbol. This is used during the code generation phase
            bLangFieldBasedAccess.symbol = fieldSymbol;
            return fieldSymbol.type;
        }

        // check if it is an attached function pointer call
        Name objFuncName = names.fromString(Symbols.getAttachedFuncSymbolName(objectType.tsymbol.name.value,
                fieldName.value));
        fieldSymbol = symResolver.resolveObjectField(bLangFieldBasedAccess.pos, env, objFuncName, objectType.tsymbol);

        if (fieldSymbol == symTable.notFoundSymbol) {
            dlog.error(bLangFieldBasedAccess.field.pos,
                       DiagnosticCode.UNDEFINED_STRUCTURE_FIELD_WITH_TYPE, fieldName,
                       objectType.tsymbol.type.getKind().typeName(), objectType.tsymbol);
            return symTable.semanticError;
        }

        // Setting the field symbol. This is used during the code generation phase
        bLangFieldBasedAccess.symbol = fieldSymbol;
        return fieldSymbol.type;
    }

    private BType checkTupleFieldType(BType tupleType, int indexValue) {
        BTupleType bTupleType = (BTupleType) tupleType;
        if (bTupleType.tupleTypes.size() <= indexValue && bTupleType.restType != null) {
            return bTupleType.restType;
        } else if (indexValue < 0 || bTupleType.tupleTypes.size() <= indexValue) {
            return symTable.semanticError;
        }
        return bTupleType.tupleTypes.get(indexValue);
    }

    private void validateTags(BLangXMLElementLiteral bLangXMLElementLiteral, SymbolEnv xmlElementEnv) {
        // check type for start and end tags
        BLangExpression startTagName = bLangXMLElementLiteral.startTagName;
        checkExpr(startTagName, xmlElementEnv, symTable.stringType);
        BLangExpression endTagName = bLangXMLElementLiteral.endTagName;
        if (endTagName == null) {
            return;
        }

        checkExpr(endTagName, xmlElementEnv, symTable.stringType);
        if (startTagName.getKind() == NodeKind.XML_QNAME && endTagName.getKind() == NodeKind.XML_QNAME &&
                startTagName.equals(endTagName)) {
            return;
        }

        if (startTagName.getKind() != NodeKind.XML_QNAME && endTagName.getKind() != NodeKind.XML_QNAME) {
            return;
        }

        dlog.error(bLangXMLElementLiteral.pos, DiagnosticCode.XML_TAGS_MISMATCH);
    }

    private void checkStringTemplateExprs(List<BLangExpression> exprs, boolean allowXml) {
        for (BLangExpression expr : exprs) {
            checkExpr(expr, env);

            BType type = expr.type;

            if (type == symTable.semanticError) {
                continue;
            }

            if (type.tag >= TypeTags.JSON) {
                if (allowXml) {
                    if (type.tag != TypeTags.XML) {
                        dlog.error(expr.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                                   BUnionType.create(null, symTable.intType, symTable.floatType,
                                                                    symTable.decimalType, symTable.stringType,
                                                                    symTable.booleanType, symTable.xmlType), type);
                    }
                    continue;
                }

                dlog.error(expr.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                           BUnionType.create(null, symTable.intType, symTable.floatType,
                                                            symTable.decimalType, symTable.stringType,
                                                            symTable.booleanType), type);
            }
        }
    }

    /**
     * Concatenate the consecutive text type nodes, and get the reduced set of children.
     *
     * @param exprs         Child nodes
     * @param xmlElementEnv
     * @return Reduced set of children
     */
    private List<BLangExpression> concatSimilarKindXMLNodes(List<BLangExpression> exprs, SymbolEnv xmlElementEnv) {
        List<BLangExpression> newChildren = new ArrayList<>();
        List<BLangExpression> tempConcatExpressions = new ArrayList<>();

        for (BLangExpression expr : exprs) {
            BType exprType = checkExpr(expr, xmlElementEnv);
            if (TypeTags.isXMLTypeTag(exprType.tag)) {
                if (!tempConcatExpressions.isEmpty()) {
                    newChildren.add(getXMLTextLiteral(tempConcatExpressions));
                    tempConcatExpressions = new ArrayList<>();
                }
                newChildren.add(expr);
                continue;
            }

            BType type = expr.type;
            if (type.tag >= TypeTags.JSON) {
                if (type != symTable.semanticError && !TypeTags.isXMLTypeTag(type.tag)) {
                    dlog.error(expr.pos, DiagnosticCode.INCOMPATIBLE_TYPES,
                               BUnionType.create(null, symTable.intType, symTable.floatType,
                                                                symTable.decimalType, symTable.stringType,
                                                                symTable.booleanType, symTable.xmlType), type);
                }
                continue;
            }

            tempConcatExpressions.add(expr);
        }

        // Add remaining concatenated text nodes as children
        if (!tempConcatExpressions.isEmpty()) {
            newChildren.add(getXMLTextLiteral(tempConcatExpressions));
        }

        return newChildren;
    }

    private BLangExpression getXMLTextLiteral(List<BLangExpression> exprs) {
        BLangXMLTextLiteral xmlTextLiteral = (BLangXMLTextLiteral) TreeBuilder.createXMLTextLiteralNode();
        xmlTextLiteral.textFragments = exprs;
        xmlTextLiteral.pos = exprs.get(0).pos;
        xmlTextLiteral.type = symTable.xmlType;
        return xmlTextLiteral;
    }

    private BType getTypeOfExprInFieldAccess(BLangExpression expr) {
        checkExpr(expr, this.env, symTable.noType);
        return expr.type;
    }

    private BType getAccessExprFinalType(BLangAccessExpression accessExpr, BType actualType) {

        // Cache the actual type of the field. This will be used in desuagr phase to create safe navigation.
        accessExpr.originalType = actualType;

        BUnionType unionType = BUnionType.create(null, actualType);

        if (returnsNull(accessExpr)) {
            unionType.add(symTable.nilType);
        }

        BType parentType = accessExpr.expr.type;
        if (accessExpr.errorSafeNavigation
                && (parentType.tag == TypeTags.SEMANTIC_ERROR || (parentType.tag == TypeTags.UNION
                && ((BUnionType) parentType).getMemberTypes().contains(symTable.errorType)))) {
            unionType.add(symTable.errorType);
        }

        // If there's only one member, and the one an only member is:
        //    a) nilType OR
        //    b) not-nullable
        // then return that only member, as the return type.
        if (unionType.getMemberTypes().size() == 1) {
            return unionType.getMemberTypes().toArray(new BType[0])[0];
        }

        return unionType;
    }

    private boolean returnsNull(BLangAccessExpression accessExpr) {
        BType parentType = accessExpr.expr.type;
        if (parentType.isNullable() && parentType.tag != TypeTags.JSON) {
            return true;
        }

        // Check whether this is a map access by index. If not, null is not a possible return type.
        if (parentType.tag != TypeTags.MAP) {
            return false;
        }

        // A map access with index, returns nullable type
        if (accessExpr.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR && accessExpr.expr.type.tag == TypeTags.MAP) {
            BType constraintType = ((BMapType) accessExpr.expr.type).constraint;

            // JSON and any is special cased here, since those are two union types, with null within them.
            // Therefore return 'type' will not include null.
            return constraintType != null && constraintType.tag != TypeTags.ANY && constraintType.tag != TypeTags.JSON;
        }

        return false;
    }

    private BType checkObjectFieldAccessExpr(BLangFieldBasedAccess fieldAccessExpr, BType varRefType, Name fieldName) {
        if (varRefType.tag == TypeTags.OBJECT) {
            return checkObjectFieldAccess(fieldAccessExpr, fieldName, (BObjectType) varRefType);
        }

        // If the type is not an object, it needs to be a union of objects.
        // Resultant field type is calculated here.
        Set<BType> memberTypes = ((BUnionType) varRefType).getMemberTypes();

        LinkedHashSet<BType> fieldTypeMembers = new LinkedHashSet<>();

        for (BType memType : memberTypes) {
            BType individualFieldType = checkObjectFieldAccess(fieldAccessExpr, fieldName, (BObjectType) memType);

            if (individualFieldType == symTable.semanticError) {
                return individualFieldType;
            }

            fieldTypeMembers.add(individualFieldType);
        }

        if (fieldTypeMembers.size() == 1) {
            return fieldTypeMembers.iterator().next();
        }

        return BUnionType.create(null, fieldTypeMembers);
    }

    private BType checkRecordFieldAccessExpr(BLangFieldBasedAccess fieldAccessExpr, BType varRefType, Name fieldName) {
        if (varRefType.tag == TypeTags.RECORD) {
            return checkRecordRequiredFieldAccess(fieldAccessExpr, fieldName, (BRecordType) varRefType);
        }

        // If the type is not an record, it needs to be a union of records.
        // Resultant field type is calculated here.
        Set<BType> memberTypes = ((BUnionType) varRefType).getMemberTypes();

        LinkedHashSet<BType> fieldTypeMembers = new LinkedHashSet<>();

        for (BType memType : memberTypes) {
            BType individualFieldType = checkRecordFieldAccessExpr(fieldAccessExpr, memType, fieldName);

            if (individualFieldType == symTable.semanticError) {
                return individualFieldType;
            }

            fieldTypeMembers.add(individualFieldType);
        }

        if (fieldTypeMembers.size() == 1) {
            return fieldTypeMembers.iterator().next();
        }

        return BUnionType.create(null, fieldTypeMembers);
    }

    private BType checkRecordFieldAccessLhsExpr(BLangFieldBasedAccess fieldAccessExpr, BType varRefType,
                                                Name fieldName) {
        if (varRefType.tag == TypeTags.RECORD) {
            BType fieldType = checkRecordRequiredFieldAccess(fieldAccessExpr, fieldName, (BRecordType) varRefType);
            if (fieldType != symTable.semanticError) {
                return fieldType;
            }

            // For the LHS, the field could be optional.
            return checkRecordOptionalFieldAccess(fieldAccessExpr, fieldName, (BRecordType) varRefType);
        }

        // If the type is not an record, it needs to be a union of records.
        // Resultant field type is calculated here.
        Set<BType> memberTypes = ((BUnionType) varRefType).getMemberTypes();

        LinkedHashSet<BType> fieldTypeMembers = new LinkedHashSet<>();

        for (BType memType : memberTypes) {
            BType individualFieldType = checkRecordFieldAccessLhsExpr(fieldAccessExpr, memType, fieldName);

            if (individualFieldType == symTable.semanticError) {
                return symTable.semanticError;
            }

            fieldTypeMembers.add(individualFieldType);
        }

        if (fieldTypeMembers.size() == 1) {
            return fieldTypeMembers.iterator().next();
        }

        return BUnionType.create(null, fieldTypeMembers);
    }

    private BType checkOptionalRecordFieldAccessExpr(BLangFieldBasedAccess fieldAccessExpr, BType varRefType,
                                                     Name fieldName) {
        if (varRefType.tag == TypeTags.RECORD) {
            BType fieldType = checkRecordRequiredFieldAccess(fieldAccessExpr, fieldName, (BRecordType) varRefType);
            if (fieldType != symTable.semanticError) {
                return fieldType;
            }

            fieldType = checkRecordOptionalFieldAccess(fieldAccessExpr, fieldName, (BRecordType) varRefType);
            if (fieldType == symTable.semanticError) {
                return fieldType;
            }
            return BUnionType.create(null, fieldType, symTable.nilType);
        }

        // If the type is not an record, it needs to be a union of records.
        // Resultant field type is calculated here.
        Set<BType> memberTypes = ((BUnionType) varRefType).getMemberTypes();

        BType fieldType;

        boolean nonMatchedRecordExists = false;

        LinkedHashSet<BType> fieldTypeMembers = new LinkedHashSet<>();

        for (BType memType : memberTypes) {
            BType individualFieldType = checkOptionalRecordFieldAccessExpr(fieldAccessExpr, memType, fieldName);

            if (individualFieldType == symTable.semanticError) {
                nonMatchedRecordExists = true;
                continue;
            }

            fieldTypeMembers.add(individualFieldType);
        }

        if (fieldTypeMembers.isEmpty()) {
            return symTable.semanticError;
        }

        if (fieldTypeMembers.size() == 1) {
            fieldType = fieldTypeMembers.iterator().next();
        } else {
            fieldType = BUnionType.create(null, fieldTypeMembers);
        }

        return nonMatchedRecordExists ? addNilForNillableAccessType(fieldType) : fieldType;
    }

    private BType checkFieldAccessExpr(BLangFieldBasedAccess fieldAccessExpr, BType varRefType, Name fieldName) {
        BType actualType = symTable.semanticError;

        if (types.isSubTypeOfBaseType(varRefType, TypeTags.OBJECT)) {
            actualType = checkObjectFieldAccessExpr(fieldAccessExpr, varRefType, fieldName);
            fieldAccessExpr.originalType = actualType;
        } else if (types.isSubTypeOfBaseType(varRefType, TypeTags.RECORD)) {
            actualType = checkRecordFieldAccessExpr(fieldAccessExpr, varRefType, fieldName);

            if (actualType != symTable.semanticError) {
                fieldAccessExpr.originalType = actualType;
                return actualType;
            }

            if (!fieldAccessExpr.lhsVar) {
                dlog.error(fieldAccessExpr.pos,
                           DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_FIELD_ACCESS_FOR_NON_REQUIRED_FIELD,
                           varRefType, fieldName);
                return actualType;
            }

            // If this is an LHS expression, check if there is a required and/ optional field by the specified field
            // name in all records.
            actualType = checkRecordFieldAccessLhsExpr(fieldAccessExpr, varRefType, fieldName);
            fieldAccessExpr.originalType = actualType;
            if (actualType == symTable.semanticError) {
                dlog.error(fieldAccessExpr.pos, DiagnosticCode.UNDEFINED_STRUCTURE_FIELD_WITH_TYPE,
                           fieldName, varRefType.tsymbol.type.getKind().typeName(), varRefType);
            }
        } else if (types.isLax(varRefType)) {
            if (fieldAccessExpr.lhsVar) {
                dlog.error(fieldAccessExpr.pos,
                           DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_FIELD_ACCESS_FOR_ASSIGNMENT,
                           varRefType);
                return symTable.semanticError;
            }
            if (fieldAccessExpr.fieldKind == FieldKind.WITH_NS) {
                resolveXMLNamespace((BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess) fieldAccessExpr);
            }
            BType laxFieldAccessType = getLaxFieldAccessType(varRefType);
            actualType = BUnionType.create(null, laxFieldAccessType, symTable.errorType);
            fieldAccessExpr.originalType = laxFieldAccessType;
        } else if (fieldAccessExpr.expr.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR &&
                hasLaxOriginalType(((BLangFieldBasedAccess) fieldAccessExpr.expr))) {
            BType laxFieldAccessType =
                    getLaxFieldAccessType(((BLangFieldBasedAccess) fieldAccessExpr.expr).originalType);
            if (fieldAccessExpr.fieldKind == FieldKind.WITH_NS) {
                resolveXMLNamespace((BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess) fieldAccessExpr);
            }
            actualType = BUnionType.create(null, laxFieldAccessType, symTable.errorType);
            fieldAccessExpr.errorSafeNavigation = true;
            fieldAccessExpr.originalType = laxFieldAccessType;
        } else if (TypeTags.isXMLTypeTag(varRefType.tag)) {
            if (fieldAccessExpr.lhsVar) {
                dlog.error(fieldAccessExpr.pos, DiagnosticCode.CANNOT_UPDATE_XML_SEQUENCE);
            }
            // todo: field access on a xml value is not attribute access, return type should be string?
            // `_` is a special field that refer to the element name.
            actualType = symTable.xmlType;
            fieldAccessExpr.originalType = actualType;
        } else if (varRefType.tag != TypeTags.SEMANTIC_ERROR) {
            dlog.error(fieldAccessExpr.pos, DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_FIELD_ACCESS,
                       varRefType);
        }

        return actualType;
    }

    private void resolveXMLNamespace(BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess fieldAccessExpr) {
        BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess nsPrefixedFieldAccess = fieldAccessExpr;
        String nsPrefix = nsPrefixedFieldAccess.nsPrefix.value;
        BSymbol nsSymbol = symResolver.lookupSymbolInPrefixSpace(env, names.fromString(nsPrefix));

        if (nsSymbol == symTable.notFoundSymbol) {
            dlog.error(nsPrefixedFieldAccess.nsPrefix.pos, DiagnosticCode.CANNOT_FIND_XML_NAMESPACE,
                    nsPrefixedFieldAccess.nsPrefix);
        } else if (nsSymbol.getKind() == SymbolKind.PACKAGE) {
            nsPrefixedFieldAccess.nsSymbol = (BXMLNSSymbol) findXMLNamespaceFromPackageConst(
                    nsPrefixedFieldAccess.field.value, nsPrefixedFieldAccess.nsPrefix.value,
                    (BPackageSymbol) nsSymbol, fieldAccessExpr.pos);
        } else {
            nsPrefixedFieldAccess.nsSymbol = (BXMLNSSymbol) nsSymbol;
        }
    }

    private boolean hasLaxOriginalType(BLangFieldBasedAccess fieldBasedAccess) {
        return fieldBasedAccess.originalType != null && types.isLax(fieldBasedAccess.originalType);
    }

    private BType getLaxFieldAccessType(BType exprType) {
        switch (exprType.tag) {
            case TypeTags.JSON:
                return symTable.jsonType;
            case TypeTags.XML:
            case TypeTags.XML_ELEMENT:
                return symTable.stringType;
            case TypeTags.MAP:
                return ((BMapType) exprType).constraint;
            case TypeTags.UNION:
                BUnionType unionType = (BUnionType) exprType;
                LinkedHashSet<BType> memberTypes = new LinkedHashSet<>();
                unionType.getMemberTypes().forEach(bType -> memberTypes.add(getLaxFieldAccessType(bType)));
                return memberTypes.size() == 1 ? memberTypes.iterator().next() : BUnionType.create(null, memberTypes);
        }
        return symTable.semanticError;
    }

    private BType checkOptionalFieldAccessExpr(BLangFieldBasedAccess fieldAccessExpr, BType varRefType,
                                               Name fieldName) {
        BType actualType = symTable.semanticError;

        boolean nillableExprType = false;
        BType effectiveType = varRefType;

        if (varRefType.tag == TypeTags.UNION) {
            Set<BType> memTypes = ((BUnionType) varRefType).getMemberTypes();

            if (memTypes.contains(symTable.nilType)) {
                LinkedHashSet<BType> nilRemovedSet = new LinkedHashSet<>();
                for (BType bType : memTypes) {
                    if (bType != symTable.nilType) {
                        nilRemovedSet.add(bType);
                    } else {
                        nillableExprType = true;
                    }
                }

                effectiveType = nilRemovedSet.size() == 1 ? nilRemovedSet.iterator().next() :
                        BUnionType.create(null, nilRemovedSet);
            }
        }

        if (types.isSubTypeOfBaseType(effectiveType, TypeTags.RECORD)) {
            actualType = checkOptionalRecordFieldAccessExpr(fieldAccessExpr, effectiveType, fieldName);
            if (actualType == symTable.semanticError) {
                dlog.error(fieldAccessExpr.pos,
                           DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_OPTIONAL_FIELD_ACCESS_FOR_FIELD,
                           varRefType, fieldName);
            }
            fieldAccessExpr.nilSafeNavigation = nillableExprType;
            fieldAccessExpr.originalType = getSafeType(actualType, fieldAccessExpr);
        } else if (types.isLax(effectiveType)) {
            BType laxFieldAccessType = getLaxFieldAccessType(effectiveType);
            actualType = accessCouldResultInError(effectiveType) ?
                    BUnionType.create(null, laxFieldAccessType, symTable.errorType) : laxFieldAccessType;
            if (fieldAccessExpr.fieldKind == FieldKind.WITH_NS) {
                resolveXMLNamespace((BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess) fieldAccessExpr);
            }
            fieldAccessExpr.originalType = laxFieldAccessType;
            fieldAccessExpr.nilSafeNavigation = true;
            nillableExprType = true;
        } else if (fieldAccessExpr.expr.getKind() == NodeKind.FIELD_BASED_ACCESS_EXPR &&
                hasLaxOriginalType(((BLangFieldBasedAccess) fieldAccessExpr.expr))) {
            BType laxFieldAccessType =
                    getLaxFieldAccessType(((BLangFieldBasedAccess) fieldAccessExpr.expr).originalType);
            actualType = accessCouldResultInError(effectiveType) ?
                    BUnionType.create(null, laxFieldAccessType, symTable.errorType) : laxFieldAccessType;
            if (fieldAccessExpr.fieldKind == FieldKind.WITH_NS) {
                resolveXMLNamespace((BLangFieldBasedAccess.BLangNSPrefixedFieldBasedAccess) fieldAccessExpr);
            }
            fieldAccessExpr.errorSafeNavigation = true;
            fieldAccessExpr.originalType = laxFieldAccessType;
            fieldAccessExpr.nilSafeNavigation = true;
            nillableExprType = true;
        } else if (varRefType.tag != TypeTags.SEMANTIC_ERROR) {
            dlog.error(fieldAccessExpr.pos,
                       DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_OPTIONAL_FIELD_ACCESS, varRefType);
        }

        if (nillableExprType && actualType != symTable.semanticError && !actualType.isNullable()) {
            actualType = BUnionType.create(null, actualType, symTable.nilType);
        }

        return actualType;
    }

    private boolean accessCouldResultInError(BType type) {
        if (type.tag == TypeTags.JSON) {
            return true;
        }

        if (type.tag == TypeTags.MAP) {
            return false;
        }

        if (type.tag == TypeTags.XML) {
            return true;
        }

        if (type.tag == TypeTags.UNION) {
            return ((BUnionType) type).getMemberTypes().stream().anyMatch(this::accessCouldResultInError);
        } else {
            return false;
        }
    }

    private BType checkIndexAccessExpr(BLangIndexBasedAccess indexBasedAccessExpr) {
        BType varRefType = indexBasedAccessExpr.expr.type;

        boolean nillableExprType = false;

        if (varRefType.tag == TypeTags.UNION) {
            Set<BType> memTypes = ((BUnionType) varRefType).getMemberTypes();

            if (memTypes.contains(symTable.nilType)) {
                LinkedHashSet<BType> nilRemovedSet = new LinkedHashSet<>();
                for (BType bType : memTypes) {
                    if (bType != symTable.nilType) {
                        nilRemovedSet.add(bType);
                    } else {
                        nillableExprType = true;
                    }
                }

                if (nillableExprType) {
                    varRefType = nilRemovedSet.size() == 1 ? nilRemovedSet.iterator().next() :
                            BUnionType.create(null, nilRemovedSet);

                    if (!types.isSubTypeOfMapping(varRefType)) {
                        // Member access is allowed on optional types only with mappings.
                        dlog.error(indexBasedAccessExpr.pos,
                                   DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_INDEXING,
                                   indexBasedAccessExpr.expr.type);
                        return symTable.semanticError;
                    }

                    if (indexBasedAccessExpr.lhsVar) {
                        dlog.error(indexBasedAccessExpr.pos,
                                   DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_INDEX_ACCESS_FOR_ASSIGNMENT,
                                   indexBasedAccessExpr.expr.type);
                        return symTable.semanticError;
                    }
                }
            }
        }


        BLangExpression indexExpr = indexBasedAccessExpr.indexExpr;
        BType actualType = symTable.semanticError;

        if (types.isSubTypeOfMapping(varRefType)) {
            checkExpr(indexExpr, this.env, symTable.stringType);

            if (indexExpr.type == symTable.semanticError) {
                return symTable.semanticError;
            }

            actualType = checkMappingIndexBasedAccess(indexBasedAccessExpr, varRefType);

            if (actualType == symTable.semanticError) {
                if (indexExpr.type.tag == TypeTags.STRING && isConst(indexExpr)) {
                    String fieldName = getConstFieldName(indexExpr);
                    dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.UNDEFINED_STRUCTURE_FIELD,
                               fieldName, indexBasedAccessExpr.expr.type);
                    return actualType;
                }

                dlog.error(indexExpr.pos, DiagnosticCode.INVALID_RECORD_INDEX_EXPR, indexExpr.type);
                return actualType;
            }

            indexBasedAccessExpr.nilSafeNavigation = nillableExprType;
            indexBasedAccessExpr.originalType = getSafeType(actualType, indexBasedAccessExpr);
        } else if (types.isSubTypeOfList(varRefType)) {
            checkExpr(indexExpr, this.env, symTable.intType);

            if (indexExpr.type == symTable.semanticError) {
                return symTable.semanticError;
            }

            actualType = checkListIndexBasedAccess(indexBasedAccessExpr, varRefType);
            indexBasedAccessExpr.originalType = actualType;

            if (actualType == symTable.semanticError) {
                if (indexExpr.type.tag == TypeTags.INT && isConst(indexExpr)) {
                    dlog.error(indexBasedAccessExpr.indexExpr.pos,
                               DiagnosticCode.LIST_INDEX_OUT_OF_RANGE, getConstIndex(indexExpr));
                    return actualType;
                }
                dlog.error(indexExpr.pos, DiagnosticCode.INVALID_LIST_INDEX_EXPR, indexExpr.type);
                return actualType;
            }
        } else if (types.isAssignable(varRefType, symTable.stringType)) {
            if (indexBasedAccessExpr.lhsVar) {
                dlog.error(indexBasedAccessExpr.pos,
                           DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_INDEX_ACCESS_FOR_ASSIGNMENT,
                           indexBasedAccessExpr.expr.type);
                return symTable.semanticError;
            }

            checkExpr(indexExpr, this.env, symTable.intType);

            if (indexExpr.type == symTable.semanticError) {
                return symTable.semanticError;
            }

            indexBasedAccessExpr.originalType = symTable.stringType;
            actualType = symTable.stringType;
        } else if (varRefType.tag == TypeTags.XML) {
            if (indexBasedAccessExpr.lhsVar) {
                indexExpr.type = symTable.semanticError;
                dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.CANNOT_UPDATE_XML_SEQUENCE);
                return actualType;
            }

            BType type = checkExpr(indexExpr, this.env, symTable.intType);
            if (type == symTable.semanticError) {
                return type;
            }
            // Note: out of range member access returns empty xml value unlike lists
            // hence, this needs to be set to xml type
            actualType = varRefType;
            indexBasedAccessExpr.originalType = actualType;
        } else if (varRefType.tag == TypeTags.TABLE) {
            BTableType tableType = (BTableType) indexBasedAccessExpr.expr.type;
            BType keyTypeConstraint = tableType.keyTypeConstraint;
            if (tableType.keyTypeConstraint == null) {
                keyTypeConstraint = createTableKeyConstraint(((BTableType) indexBasedAccessExpr.expr.type).
                        fieldNameList, ((BTableType) indexBasedAccessExpr.expr.type).constraint);

                if (keyTypeConstraint == symTable.semanticError) {
                    dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.MEMBER_ACCESS_NOT_SUPPORT_FOR_KEYLESS_TABLE,
                            indexBasedAccessExpr.expr);
                    return symTable.semanticError;
                }
            }

            if (indexExpr.getKind() != NodeKind.TABLE_MULTI_KEY) {
                checkExpr(indexExpr, this.env, keyTypeConstraint);
                if (indexExpr.type == symTable.semanticError) {
                    dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.INVALID_KEY_CONSTRAINT_PROVIDED_FOR_ACCESS,
                            keyTypeConstraint);
                    return symTable.semanticError;
                }
            } else {
                List<BLangExpression> multiKeyExpressionList = ((BLangTableMultiKeyExpr)
                        indexBasedAccessExpr.indexExpr).multiKeyIndexExprs;
                List<BType> keyConstraintTypes = ((BTupleType) keyTypeConstraint).tupleTypes;
                if (keyConstraintTypes.size() != multiKeyExpressionList.size()) {
                    dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.INVALID_KEY_CONSTRAINT_PROVIDED_FOR_ACCESS,
                            keyTypeConstraint);
                    return symTable.semanticError;
                }

                for (int i = 0; i < multiKeyExpressionList.size(); i++) {
                    BLangExpression keyExpr = multiKeyExpressionList.get(i);
                    checkExpr(keyExpr, this.env, keyConstraintTypes.get(i));
                    if (keyExpr.type == symTable.semanticError) {
                        dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.INVALID_KEY_CONSTRAINT_PROVIDED_FOR_ACCESS,
                                keyTypeConstraint);
                        return symTable.semanticError;
                    }
                }
            }

            if (expType.tag != TypeTags.NONE) {
                BType resultType = checkExpr(indexBasedAccessExpr.expr, env, expType);
                if (resultType == symTable.semanticError) {
                    return symTable.semanticError;
                }
            }

            indexBasedAccessExpr.originalType = tableType.constraint;
            actualType = tableType.constraint;
        } else if (varRefType == symTable.semanticError) {
            indexBasedAccessExpr.indexExpr.type = symTable.semanticError;
            return symTable.semanticError;
        } else {
            indexBasedAccessExpr.indexExpr.type = symTable.semanticError;
            dlog.error(indexBasedAccessExpr.pos, DiagnosticCode.OPERATION_DOES_NOT_SUPPORT_INDEXING,
                    indexBasedAccessExpr.expr.type);
            return symTable.semanticError;
        }

        if (nillableExprType && !actualType.isNullable()) {
            actualType = BUnionType.create(null, actualType, symTable.nilType);
        }

        return actualType;
    }

    private Long getConstIndex(BLangExpression indexExpr) {
        return indexExpr.getKind() == NodeKind.NUMERIC_LITERAL ? (Long) ((BLangLiteral) indexExpr).value :
                (Long) ((BConstantSymbol) ((BLangSimpleVarRef) indexExpr).symbol).value.value;
    }

    private String getConstFieldName(BLangExpression indexExpr) {
        return indexExpr.getKind() == NodeKind.LITERAL ? (String) ((BLangLiteral) indexExpr).value :
                (String) ((BConstantSymbol) ((BLangSimpleVarRef) indexExpr).symbol).value.value;
    }

    private BType checkArrayIndexBasedAccess(BLangIndexBasedAccess indexBasedAccess, BType indexExprType,
                                             BArrayType arrayType) {
        BType actualType = symTable.semanticError;
        switch (indexExprType.tag) {
            case TypeTags.INT:
                BLangExpression indexExpr = indexBasedAccess.indexExpr;
                if (!isConst(indexExpr) || arrayType.state == BArrayState.UNSEALED) {
                    actualType = arrayType.eType;
                    break;
                }
                actualType = getConstIndex(indexExpr) >= arrayType.size ? symTable.semanticError : arrayType.eType;
                break;
            case TypeTags.FINITE:
                BFiniteType finiteIndexExpr = (BFiniteType) indexExprType;
                boolean validIndexExists = false;
                for (BLangExpression finiteMember : finiteIndexExpr.getValueSpace()) {
                    int indexValue = ((Long) ((BLangLiteral) finiteMember).value).intValue();
                    if (indexValue >= 0 &&
                            (arrayType.state == BArrayState.UNSEALED || indexValue < arrayType.size)) {
                        validIndexExists = true;
                        break;
                    }
                }
                if (!validIndexExists) {
                    return symTable.semanticError;
                }
                actualType = arrayType.eType;
                break;
            case TypeTags.UNION:
                // address the case where we have a union of finite types
                List<BFiniteType> finiteTypes = ((BUnionType) indexExprType).getMemberTypes().stream()
                        .filter(memType -> memType.tag == TypeTags.FINITE)
                        .map(matchedType -> (BFiniteType) matchedType)
                        .collect(Collectors.toList());

                BFiniteType finiteType;
                if (finiteTypes.size() == 1) {
                    finiteType = finiteTypes.get(0);
                } else {
                    Set<BLangExpression> valueSpace = new LinkedHashSet<>();
                    finiteTypes.forEach(constituent -> valueSpace.addAll(constituent.getValueSpace()));
                    finiteType = new BFiniteType(null, valueSpace);
                }

                BType elementType = checkArrayIndexBasedAccess(indexBasedAccess, finiteType, arrayType);
                if (elementType == symTable.semanticError) {
                    return symTable.semanticError;
                }
                actualType = arrayType.eType;
        }
        return actualType;
    }

    private BType checkListIndexBasedAccess(BLangIndexBasedAccess accessExpr, BType type) {
        if (type.tag == TypeTags.ARRAY) {
            return checkArrayIndexBasedAccess(accessExpr, accessExpr.indexExpr.type, (BArrayType) type);
        }

        if (type.tag == TypeTags.TUPLE) {
            return checkTupleIndexBasedAccess(accessExpr, (BTupleType) type, accessExpr.indexExpr.type);
        }

        LinkedHashSet<BType> fieldTypeMembers = new LinkedHashSet<>();

        for (BType memType : ((BUnionType) type).getMemberTypes()) {
            BType individualFieldType = checkListIndexBasedAccess(accessExpr, memType);

            if (individualFieldType == symTable.semanticError) {
                continue;
            }

            fieldTypeMembers.add(individualFieldType);
        }

        if (fieldTypeMembers.size() == 0) {
            return symTable.semanticError;
        }

        if (fieldTypeMembers.size() == 1) {
            return fieldTypeMembers.iterator().next();
        }
        return BUnionType.create(null, fieldTypeMembers);
    }

    private BType checkTupleIndexBasedAccess(BLangIndexBasedAccess accessExpr, BTupleType tuple, BType currentType) {
        BType actualType = symTable.semanticError;
        BLangExpression indexExpr = accessExpr.indexExpr;
        switch (currentType.tag) {
            case TypeTags.INT:
                if (isConst(indexExpr)) {
                    actualType = checkTupleFieldType(tuple, getConstIndex(indexExpr).intValue());
                } else {
                    BTupleType tupleExpr = (BTupleType) accessExpr.expr.type;
                    LinkedHashSet<BType> tupleTypes = collectTupleFieldTypes(tupleExpr, new LinkedHashSet<>());
                    actualType = tupleTypes.size() == 1 ? tupleTypes.iterator().next() : BUnionType.create(null,
                                                                                                           tupleTypes);
                }
                break;
            case TypeTags.FINITE:
                BFiniteType finiteIndexExpr = (BFiniteType) currentType;
                LinkedHashSet<BType> possibleTypes = new LinkedHashSet<>();
                for (BLangExpression finiteMember : finiteIndexExpr.getValueSpace()) {
                    int indexValue = ((Long) ((BLangLiteral) finiteMember).value).intValue();
                    BType fieldType = checkTupleFieldType(tuple, indexValue);
                    if (fieldType.tag != TypeTags.SEMANTIC_ERROR) {
                        possibleTypes.add(fieldType);
                    }
                }
                if (possibleTypes.size() == 0) {
                    return symTable.semanticError;
                }
                actualType = possibleTypes.size() == 1 ? possibleTypes.iterator().next() :
                        BUnionType.create(null, possibleTypes);
                break;

            case TypeTags.UNION:
                LinkedHashSet<BType> possibleTypesByMember = new LinkedHashSet<>();
                List<BFiniteType> finiteTypes = new ArrayList<>();
                ((BUnionType) currentType).getMemberTypes().forEach(memType -> {
                    if (memType.tag == TypeTags.FINITE) {
                        finiteTypes.add((BFiniteType) memType);
                    } else {
                        BType possibleType = checkTupleIndexBasedAccess(accessExpr, tuple, memType);
                        if (possibleType.tag == TypeTags.UNION) {
                            possibleTypesByMember.addAll(((BUnionType) possibleType).getMemberTypes());
                        } else {
                            possibleTypesByMember.add(possibleType);
                        }
                    }
                });

                BFiniteType finiteType;
                if (finiteTypes.size() == 1) {
                    finiteType = finiteTypes.get(0);
                } else {
                    Set<BLangExpression> valueSpace = new LinkedHashSet<>();
                    finiteTypes.forEach(constituent -> valueSpace.addAll(constituent.getValueSpace()));
                    finiteType = new BFiniteType(null, valueSpace);
                }

                BType possibleType = checkTupleIndexBasedAccess(accessExpr, tuple, finiteType);
                if (possibleType.tag == TypeTags.UNION) {
                    possibleTypesByMember.addAll(((BUnionType) possibleType).getMemberTypes());
                } else {
                    possibleTypesByMember.add(possibleType);
                }

                if (possibleTypesByMember.contains(symTable.semanticError)) {
                    return symTable.semanticError;
                }
                actualType = possibleTypesByMember.size() == 1 ? possibleTypesByMember.iterator().next() :
                        BUnionType.create(null, possibleTypesByMember);
        }
        return actualType;
    }

    private LinkedHashSet<BType> collectTupleFieldTypes(BTupleType tupleType, LinkedHashSet<BType> memberTypes) {
        tupleType.tupleTypes
                .forEach(memberType -> {
                    if (memberType.tag == TypeTags.UNION) {
                        collectMemberTypes((BUnionType) memberType, memberTypes);
                    } else {
                        memberTypes.add(memberType);
                    }
                });
        return memberTypes;
    }

    private BType checkMappingIndexBasedAccess(BLangIndexBasedAccess accessExpr, BType type) {
        if (type.tag == TypeTags.MAP) {
            BType constraint = ((BMapType) type).constraint;
            return accessExpr.lhsVar ? constraint : addNilForNillableAccessType(constraint);
        }

        if (type.tag == TypeTags.RECORD) {
            return checkRecordIndexBasedAccess(accessExpr, (BRecordType) type, accessExpr.indexExpr.type);
        }

        BType fieldType;

        boolean nonMatchedRecordExists = false;

        LinkedHashSet<BType> fieldTypeMembers = new LinkedHashSet<>();

        for (BType memType : ((BUnionType) type).getMemberTypes()) {
            BType individualFieldType = checkMappingIndexBasedAccess(accessExpr, memType);

            if (individualFieldType == symTable.semanticError) {
                nonMatchedRecordExists = true;
                continue;
            }

            fieldTypeMembers.add(individualFieldType);
        }

        if (fieldTypeMembers.size() == 0) {
            return symTable.semanticError;
        }

        if (fieldTypeMembers.size() == 1) {
            fieldType = fieldTypeMembers.iterator().next();
        } else {
            fieldType = BUnionType.create(null, fieldTypeMembers);
        }

        return nonMatchedRecordExists ? addNilForNillableAccessType(fieldType) : fieldType;
    }

    private BType checkRecordIndexBasedAccess(BLangIndexBasedAccess accessExpr, BRecordType record, BType currentType) {
        BType actualType = symTable.semanticError;
        BLangExpression indexExpr = accessExpr.indexExpr;
        switch (currentType.tag) {
            case TypeTags.STRING:
                if (isConst(indexExpr)) {
                    String fieldName = getConstFieldName(indexExpr);
                    actualType = checkRecordRequiredFieldAccess(accessExpr, names.fromString(fieldName), record);
                    if (actualType != symTable.semanticError) {
                        return actualType;
                    }

                    actualType = checkRecordOptionalFieldAccess(accessExpr, names.fromString(fieldName), record);
                    if (actualType == symTable.semanticError) {
                        actualType = checkRecordRestFieldAccess(accessExpr, names.fromString(fieldName), record);
                        if (actualType == symTable.semanticError) {
                            return actualType;
                        }
                        if (actualType == symTable.neverType) {
                            return actualType;
                        }
                        return addNilForNillableAccessType(actualType);
                    }

                    if (accessExpr.lhsVar) {
                        return actualType;
                    }
                    return addNilForNillableAccessType(actualType);
                }

                LinkedHashSet<BType> fieldTypes = record.fields.values().stream()
                        .map(field -> field.type)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                if (record.restFieldType.tag != TypeTags.NONE) {
                    fieldTypes.add(record.restFieldType);
                }

                if (fieldTypes.stream().noneMatch(BType::isNullable)) {
                    fieldTypes.add(symTable.nilType);
                }

                actualType = BUnionType.create(null, fieldTypes);
                break;
            case TypeTags.FINITE:
                BFiniteType finiteIndexExpr = (BFiniteType) currentType;
                LinkedHashSet<BType> possibleTypes = new LinkedHashSet<>();
                for (BLangExpression finiteMember : finiteIndexExpr.getValueSpace()) {
                    String fieldName = (String) ((BLangLiteral) finiteMember).value;
                    BType fieldType = checkRecordRequiredFieldAccess(accessExpr, names.fromString(fieldName), record);
                    if (fieldType == symTable.semanticError) {
                        fieldType = checkRecordOptionalFieldAccess(accessExpr, names.fromString(fieldName), record);
                        if (fieldType == symTable.semanticError) {
                            fieldType = checkRecordRestFieldAccess(accessExpr, names.fromString(fieldName), record);
                        }

                        if (fieldType != symTable.semanticError) {
                            fieldType = addNilForNillableAccessType(fieldType);
                        }
                    }

                    if (fieldType.tag == TypeTags.SEMANTIC_ERROR) {
                        continue;
                    }
                    possibleTypes.add(fieldType);
                }

                if (possibleTypes.isEmpty()) {
                    return symTable.semanticError;
                }

                if (possibleTypes.stream().noneMatch(BType::isNullable)) {
                    possibleTypes.add(symTable.nilType);
                }

                actualType = possibleTypes.size() == 1 ? possibleTypes.iterator().next() :
                        BUnionType.create(null, possibleTypes);
                break;
            case TypeTags.UNION:
                LinkedHashSet<BType> possibleTypesByMember = new LinkedHashSet<>();
                List<BFiniteType> finiteTypes = new ArrayList<>();
                ((BUnionType) currentType).getMemberTypes().forEach(memType -> {
                    if (memType.tag == TypeTags.FINITE) {
                        finiteTypes.add((BFiniteType) memType);
                    } else {
                        BType possibleType = checkRecordIndexBasedAccess(accessExpr, record, memType);
                        if (possibleType.tag == TypeTags.UNION) {
                            possibleTypesByMember.addAll(((BUnionType) possibleType).getMemberTypes());
                        } else {
                            possibleTypesByMember.add(possibleType);
                        }
                    }
                });

                BFiniteType finiteType;
                if (finiteTypes.size() == 1) {
                    finiteType = finiteTypes.get(0);
                } else {
                    Set<BLangExpression> valueSpace = new LinkedHashSet<>();
                    finiteTypes.forEach(constituent -> valueSpace.addAll(constituent.getValueSpace()));
                    finiteType = new BFiniteType(null, valueSpace);
                }

                BType possibleType = checkRecordIndexBasedAccess(accessExpr, record, finiteType);
                if (possibleType.tag == TypeTags.UNION) {
                    possibleTypesByMember.addAll(((BUnionType) possibleType).getMemberTypes());
                } else {
                    possibleTypesByMember.add(possibleType);
                }

                if (possibleTypesByMember.contains(symTable.semanticError)) {
                    return symTable.semanticError;
                }
                actualType = possibleTypesByMember.size() == 1 ? possibleTypesByMember.iterator().next() :
                        BUnionType.create(null, possibleTypesByMember);
        }
        return actualType;
    }

    private BType getSafeType(BType type, BLangAccessExpression accessExpr) {
        if (type.tag != TypeTags.UNION) {
            return type;
        }

        // Extract the types without the error and null, and revisit access expression
        List<BType> lhsTypes = new ArrayList<>(((BUnionType) type).getMemberTypes());

        if (accessExpr.errorSafeNavigation) {
            if (!lhsTypes.contains(symTable.errorType)) {
                dlog.error(accessExpr.pos, DiagnosticCode.SAFE_NAVIGATION_NOT_REQUIRED, type);
                return symTable.semanticError;
            }

            lhsTypes = lhsTypes.stream()
                    .filter(memberType -> memberType != symTable.errorType)
                    .collect(Collectors.toList());

            if (lhsTypes.isEmpty()) {
                dlog.error(accessExpr.pos, DiagnosticCode.SAFE_NAVIGATION_NOT_REQUIRED, type);
                return symTable.semanticError;
            }
        }

        if (accessExpr.nilSafeNavigation) {
            lhsTypes = lhsTypes.stream()
                    .filter(memberType -> memberType != symTable.nilType)
                    .collect(Collectors.toList());
        }

        if (lhsTypes.size() == 1) {
            return lhsTypes.get(0);
        }

        return BUnionType.create(null, new LinkedHashSet<>(lhsTypes));
    }

    private List<BType> getTypesList(BType type) {
        if (type.tag == TypeTags.UNION) {
            BUnionType unionType = (BUnionType) type;
            return new ArrayList<>(unionType.getMemberTypes());
        } else {
            return Lists.of(type);
        }
    }

    private LinkedHashSet<BType> getMatchExpressionTypes(BLangMatchExpression bLangMatchExpression) {
        List<BType> exprTypes = getTypesList(bLangMatchExpression.expr.type);
        LinkedHashSet<BType> matchExprTypes = new LinkedHashSet<>();
        for (BType type : exprTypes) {
            boolean assignable = false;
            for (BLangMatchExprPatternClause pattern : bLangMatchExpression.patternClauses) {
                BType patternExprType = pattern.expr.type;

                // Type of the pattern expression, becomes one of the types of the whole but expression
                matchExprTypes.addAll(getTypesList(patternExprType));

                if (type.tag == TypeTags.SEMANTIC_ERROR || patternExprType.tag == TypeTags.SEMANTIC_ERROR) {
                    return new LinkedHashSet<BType>() {
                        {
                            add(symTable.semanticError);
                        }
                    };
                }

                assignable = this.types.isAssignable(type, pattern.variable.type);
                if (assignable) {
                    break;
                }
            }

            // If the matching expr type is not matching to any pattern, it becomes one of the types
            // returned by the whole but expression
            if (!assignable) {
                matchExprTypes.add(type);
            }
        }

        return matchExprTypes;
    }

    private BSymbol getFunctionPointerCallSymbol(BLangInvocation iExpr) {
        if (iExpr.expr == null) {
            // shouldn't reach here
            return symTable.notFoundSymbol;
        }

        BSymbol varSymbol = ((BLangVariableReference) iExpr.expr).symbol;
        if (varSymbol == null) {
            return symTable.notFoundSymbol;
        }

        BType varType = varSymbol.type;
        if (varType.tag != TypeTags.INVOKABLE) {
            return symTable.notFoundSymbol;
        }

        if (varSymbol.kind != SymbolKind.FUNCTION) {
            varSymbol = new BInvokableSymbol(SymTag.VARIABLE, 0, varSymbol.name, env.enclPkg.symbol.pkgID, varType,
                    env.scope.owner);
            varSymbol.kind = SymbolKind.FUNCTION;
        }

        iExpr.symbol = varSymbol;
        return varSymbol;
    }

    private boolean couldHoldTableValues(BType type, List<BType> encounteredTypes) {
        if (encounteredTypes.contains(type)) {
            return false;
        }
        encounteredTypes.add(type);

        switch (type.tag) {
            case TypeTags.UNION:
                for (BType bType1 : ((BUnionType) type).getMemberTypes()) {
                    if (couldHoldTableValues(bType1, encounteredTypes)) {
                        return true;
                    }
                }
                return false;
            case TypeTags.MAP:
                return couldHoldTableValues(((BMapType) type).constraint, encounteredTypes);
            case TypeTags.RECORD:
                BRecordType recordType = (BRecordType) type;
                for (BField field : recordType.fields.values()) {
                    if (couldHoldTableValues(field.type, encounteredTypes)) {
                        return true;
                    }
                }
                return !recordType.sealed && couldHoldTableValues(recordType.restFieldType, encounteredTypes);
            case TypeTags.ARRAY:
                return couldHoldTableValues(((BArrayType) type).eType, encounteredTypes);
            case TypeTags.TUPLE:
                for (BType bType : ((BTupleType) type).getTupleTypes()) {
                    if (couldHoldTableValues(bType, encounteredTypes)) {
                        return true;
                    }
                }
                return false;
        }
        return false;
    }

    private boolean isConst(BLangExpression expression) {

        if (ConstantAnalyzer.isValidConstantExpressionNode(expression)) {
            return true;
        }

        if (expression.getKind() != NodeKind.SIMPLE_VARIABLE_REF) {
            return false;
        }

        return (((BLangSimpleVarRef) expression).symbol.tag & SymTag.CONSTANT) == SymTag.CONSTANT;
    }

    private Name getCurrentCompUnit(BLangNode node) {
        return names.fromString(node.pos.getSource().getCompilationUnitName());
    }

    private BType getRepresentativeBroadTypeForExprs(List<BLangExpression> exprs) {
        return getRepresentativeBroadType(new ArrayList<>(Arrays.asList(getExprListUniqueTypes(exprs, env))));
    }

    private BType getRepresentativeBroadType(List<BType> inferredTypeList) {
        for (int i = 0; i < inferredTypeList.size(); i++) {
            BType type = inferredTypeList.get(i);
            if (type.tag == TypeTags.SEMANTIC_ERROR) {
                return type;
            }

            for (int j = i + 1; j < inferredTypeList.size(); j++) {
                BType otherType = inferredTypeList.get(j);

                if (otherType.tag == TypeTags.SEMANTIC_ERROR) {
                    return otherType;
                }

                if (types.isAssignable(otherType, type)) {
                    inferredTypeList.remove(j);
                    j -= 1;
                    continue;
                }

                if (types.isAssignable(type, otherType)) {
                    inferredTypeList.remove(i);
                    i -= 1;
                    break;
                }
            }
        }

        if (inferredTypeList.size() == 1) {
            return inferredTypeList.get(0);
        }

        return BUnionType.create(null, inferredTypeList.toArray(new BType[0]));
    }

    private BRecordType defineInferredRecordType(BLangRecordLiteral recordLiteral) {
        PackageID pkgID = env.enclPkg.symbol.pkgID;
        BRecordTypeSymbol recordSymbol =
                Symbols.createRecordSymbol(0, names.fromString(anonymousModelHelper.getNextAnonymousTypeKey(pkgID)),
                                           pkgID, null, env.scope.owner);

        BInvokableType bInvokableType = new BInvokableType(new ArrayList<>(), symTable.nilType, null);
        BInvokableSymbol initFuncSymbol = Symbols.createFunctionSymbol(
                Flags.PUBLIC, Names.EMPTY, env.enclPkg.symbol.pkgID, bInvokableType, env.scope.owner, false);
        initFuncSymbol.retType = symTable.nilType;
        recordSymbol.initializerFunc = new BAttachedFunction(Names.INIT_FUNCTION_SUFFIX, initFuncSymbol,
                                                             bInvokableType);

        recordSymbol.scope = new Scope(recordSymbol);
        recordSymbol.scope.define(
                names.fromString(recordSymbol.name.value + "." + recordSymbol.initializerFunc.funcName.value),
                recordSymbol.initializerFunc.symbol);

        Map<String, FieldInfo> nonRestFieldTypes = new LinkedHashMap<>();
        List<BType> restFieldTypes = new ArrayList<>();

        for (RecordLiteralNode.RecordField field : recordLiteral.fields) {
            if (field.isKeyValueField()) {
                BLangRecordKeyValueField keyValue = (BLangRecordKeyValueField) field;
                BLangRecordKey key = keyValue.key;
                BLangExpression expression = keyValue.valueExpr;
                BLangExpression keyExpr = key.expr;

                if (key.computedKey) {
                    BType exprType = checkExpr(expression, env);
                    if (isUniqueType(restFieldTypes, exprType)) {
                        restFieldTypes.add(exprType);
                    }
                } else {
                    addToNonRestFieldTypes(nonRestFieldTypes, getKeyName(keyExpr), checkExpr(expression, env), true);
                }
            } else if (field.getKind() == NodeKind.RECORD_LITERAL_SPREAD_OP) {
                BType type = checkExpr(((BLangRecordLiteral.BLangRecordSpreadOperatorField) field).expr, env);
                int typeTag = type.tag;

                if (typeTag == TypeTags.MAP) {
                    BType constraintType = ((BMapType) type).constraint;

                    if (isUniqueType(restFieldTypes, constraintType)) {
                        restFieldTypes.add(constraintType);
                    }
                }

                if (type.tag != TypeTags.RECORD) {
                    continue;
                }

                BRecordType recordType = (BRecordType) type;
                for (BField recField : recordType.fields.values()) {
                    addToNonRestFieldTypes(nonRestFieldTypes, recField.name.value, recField.type,
                                           !Symbols.isOptional(recField.symbol));
                }

                if (!recordType.sealed) {
                    BType restFieldType = recordType.restFieldType;
                    if (isUniqueType(restFieldTypes, restFieldType)) {
                        restFieldTypes.add(restFieldType);
                    }
                }
            } else {
                BLangSimpleVarRef varRef = (BLangSimpleVarRef) field;
                addToNonRestFieldTypes(nonRestFieldTypes, getKeyName(varRef), checkExpr(varRef, env), true);
            }
        }

        LinkedHashMap<String, BField> fields = new LinkedHashMap<>();

        for (Map.Entry<String, FieldInfo> entry : nonRestFieldTypes.entrySet()) {
            FieldInfo fieldInfo = entry.getValue();
            List<BType> types = fieldInfo.types;

            String key = entry.getKey();
            Name fieldName = names.fromString(key);
            BType type = types.size() == 1 ? types.get(0) : BUnionType.create(null, types.toArray(new BType[0]));
            BVarSymbol fieldSymbol = new BVarSymbol(fieldInfo.required ? Flags.REQUIRED : Flags.OPTIONAL, fieldName,
                                                    pkgID, type, recordSymbol);
            fields.put(fieldName.value, new BField(fieldName, null, fieldSymbol));
            recordSymbol.scope.define(fieldName, fieldSymbol);
        }

        BRecordType recordType = new BRecordType(recordSymbol);
        recordType.fields = fields;

        if (restFieldTypes.isEmpty()) {
            recordType.sealed = true;
            recordType.restFieldType = symTable.noType;
        } else if (restFieldTypes.size() == 1) {
            recordType.restFieldType = restFieldTypes.get(0);
        } else {
            recordType.restFieldType = BUnionType.create(null, restFieldTypes.toArray(new BType[0]));
        }
        recordSymbol.type = recordType;
        recordType.tsymbol = recordSymbol;

        BLangRecordTypeNode recordTypeNode = TypeDefBuilderHelper.createRecordTypeNode(recordType, pkgID, symTable,
                                                                                       recordLiteral.pos);
        recordTypeNode.initFunction = TypeDefBuilderHelper.createInitFunctionForRecordType(recordTypeNode, env, names,
                                                                                           symTable);
        TypeDefBuilderHelper.addTypeDefinition(recordType, recordSymbol, recordTypeNode, env);

        return recordType;
    }

    private String getKeyName(BLangExpression key) {
        return key.getKind() == NodeKind.SIMPLE_VARIABLE_REF ?
                ((BLangSimpleVarRef) key).variableName.value : (String) ((BLangLiteral) key).value;
    }

    private void addToNonRestFieldTypes(Map<String, FieldInfo> nonRestFieldTypes, String keyString,
                                        BType exprType, boolean required) {
        if (!nonRestFieldTypes.containsKey(keyString)) {
            nonRestFieldTypes.put(keyString, new FieldInfo(new ArrayList<BType>() {{ add(exprType); }}, required));
            return;
        }

        FieldInfo fieldInfo = nonRestFieldTypes.get(keyString);
        List<BType> typeList = fieldInfo.types;

        if (isUniqueType(typeList, exprType)) {
            typeList.add(exprType);
        }

        if (required && !fieldInfo.required) {
            fieldInfo.required = true;
        }
    }

    private boolean isUniqueType(List<BType> typeList, BType type) {
        boolean isRecord = type.tag == TypeTags.RECORD;

        for (BType bType : typeList) {

            if (isRecord) {
                if (type == bType) {
                    return false;
                }
            } else if (types.isSameType(type, bType)) {
                return false;
            }
        }
        return true;
    }

    private BType checkXmlSubTypeLiteralCompatibility(DiagnosticPos pos, BType mutableXmlSubType, BType expType) {
        if (expType == symTable.semanticError) {
            return expType;
        }

        boolean unionExpType = expType.tag == TypeTags.UNION;

        if (expType == mutableXmlSubType) {
            return expType;
        }

        if (!unionExpType && types.isAssignable(mutableXmlSubType, expType)) {
            return mutableXmlSubType;
        }

        BXMLSubType immutableXmlSubType =
                (BXMLSubType) ImmutableTypeCloner.setImmutableType(pos, types, mutableXmlSubType, env, symTable,
                                                                   anonymousModelHelper, names);
        if (expType == immutableXmlSubType) {
            return expType;
        }

        if (!unionExpType && types.isAssignable(immutableXmlSubType, expType)) {
            return immutableXmlSubType;
        }

        if (!unionExpType) {
            dlog.error(pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType, mutableXmlSubType);
            return symTable.semanticError;
        }

        List<BType> compatibleTypes = new ArrayList<>();
        for (BType memberType : ((BUnionType) expType).getMemberTypes()) {
            if (compatibleTypes.contains(memberType)) {
                continue;
            }

            if (memberType == mutableXmlSubType || memberType == immutableXmlSubType) {
                compatibleTypes.add(memberType);
                continue;
            }

            if (types.isAssignable(mutableXmlSubType, memberType) && !compatibleTypes.contains(mutableXmlSubType)) {
                compatibleTypes.add(mutableXmlSubType);
                continue;
            }

            if (types.isAssignable(immutableXmlSubType, memberType) && !compatibleTypes.contains(immutableXmlSubType)) {
                compatibleTypes.add(immutableXmlSubType);
            }
        }

        if (compatibleTypes.isEmpty()) {
            dlog.error(pos, DiagnosticCode.INCOMPATIBLE_TYPES, expType, mutableXmlSubType);
            return symTable.semanticError;
        }

        if (compatibleTypes.size() == 1) {
            return compatibleTypes.get(0);
        }

        dlog.error(pos, DiagnosticCode.AMBIGUOUS_TYPES, expType);
        return symTable.semanticError;
    }

    private void markChildrenAsImmutable(BLangXMLElementLiteral bLangXMLElementLiteral) {
        for (BLangExpression modifiedChild : bLangXMLElementLiteral.modifiedChildren) {
            BType childType = modifiedChild.type;
            if (Symbols.isFlagOn(childType.flags, Flags.READONLY) || !types.isSelectivelyImmutableType(childType)) {
                continue;
            }
            modifiedChild.type = ImmutableTypeCloner.setImmutableType(modifiedChild.pos, types, childType, env,
                                                                      symTable, anonymousModelHelper, names);

            if (modifiedChild.getKind() == NodeKind.XML_ELEMENT_LITERAL) {
                markChildrenAsImmutable((BLangXMLElementLiteral) modifiedChild);
            }
        }
    }

    private void logUndefinedSymbolError(DiagnosticPos pos, String name) {
        if (!missingNodesHelper.isMissingNode(name)) {
            dlog.error(pos, DiagnosticCode.UNDEFINED_SYMBOL, name);
        }
    }

    private static class FieldInfo {
        List<BType> types;
        boolean required;

        private FieldInfo(List<BType> types, boolean required) {
            this.types = types;
            this.required = required;
        }
    }

    private static class FieldTypePosPair {
        BType type;
        DiagnosticPos pos;

        FieldTypePosPair(BType type, DiagnosticPos pos) {
            this.type = type;
            this.pos = pos;
        }
    }
}
