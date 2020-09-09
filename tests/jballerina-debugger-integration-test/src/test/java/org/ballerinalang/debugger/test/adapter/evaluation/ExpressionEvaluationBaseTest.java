/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.debugger.test.adapter.evaluation;

import org.apache.commons.lang3.tuple.Pair;
import org.ballerinalang.debugger.test.DebugAdapterBaseTestCase;
import org.ballerinalang.debugger.test.utils.BallerinaTestDebugPoint;
import org.ballerinalang.debugger.test.utils.DebugUtils;
import org.ballerinalang.test.context.BallerinaTestException;
import org.eclipse.lsp4j.debug.StoppedEventArguments;

import java.nio.file.Paths;

/**
 * Base implementation for debug expression evaluation scenarios.
 */
public abstract class ExpressionEvaluationBaseTest extends DebugAdapterBaseTestCase {

    protected StoppedEventArguments context;

    protected static final String nilVar = "v01_varVariable";
    protected static final String booleanVar = "v02_booleanVar";
    protected static final String intVar = "v03_intVar";
    protected static final String floatVar = "v04_floatVar";
    protected static final String decimalVar = "v05_decimalVar";
    protected static final String stringVar = "v06_stringVar";
    protected static final String xmlVar = "v07_xmlVar";
    protected static final String arrayVar = "v08_arrayVar";
    protected static final String tupleVar = "v09_tupleVar";
    protected static final String mapVar = "v10_mapVar";
    protected static final String recordVar = "v11_john";
    protected static final String anonRecordVar = "v12_anonRecord";
    protected static final String errorVar = "v13_errorVar";
    protected static final String anonFunctionVar = "v14_anonFunctionVar";
    protected static final String futureVar = "v15_futureVar";
    protected static final String objectVar = "v16_objectVar";
    protected static final String anonObjectVar = "v17_anonObjectVar";
    protected static final String typeDescVar = "v18_typedescVar";
    protected static final String unionVar = "v19_unionVar";
    protected static final String optionalVar = "v20_optionalVar";
    protected static final String anyVar = "v21_anyVar";
    protected static final String anydataVar = "v22_anydataVar";
    protected static final String byteVar = "v23_byteVar";
    protected static final String jsonVar = "v24_jsonVar";
    protected static final String tableVar = "v25_tableVar";
    protected static final String streamVar = "v26_oddNumberStream";
    protected static final String neverVar = "v27_neverVar";

    protected void prepareForEvaluation() throws BallerinaTestException {
        testProjectName = "basic-project";
        testModuleName = "advanced";
        testModuleFileName = "main.bal";
        testProjectPath = Paths.get(testProjectBaseDir.toString(), testProjectName).toString();
        testEntryFilePath = Paths.get(testProjectPath, "src", testModuleName, testModuleFileName).toString();

        addBreakPoint(new BallerinaTestDebugPoint(testEntryFilePath, 154));
        initDebugSession(DebugUtils.DebuggeeExecutionKind.RUN);
        Pair<BallerinaTestDebugPoint, StoppedEventArguments> debugHitInfo = waitForDebugHit(25000);
        this.context = debugHitInfo.getRight();
    }

    // 1. literal expressions
    public abstract void literalEvaluationTest() throws BallerinaTestException;

    // 2. list constructor expressions
    public abstract void listConstructorEvaluationTest() throws BallerinaTestException;

    // 3. mapping constructor expressions
    public abstract void mappingConstructorEvaluationTest() throws BallerinaTestException;

    // Todo - table constructor[preview] and service constructor

    // 4. string template expressions
    public abstract void stringTemplateEvaluationTest() throws BallerinaTestException;

    // 5. xml template expressions
    public abstract void xmlTemplateEvaluationTest() throws BallerinaTestException;

    // 6. new constructor expressions
    public abstract void newConstructorEvaluationTest() throws BallerinaTestException;

    // 7. variable reference expressions
    public abstract void variableReferenceEvaluationTest() throws BallerinaTestException;

    // 8. field access expressions
    public abstract void fieldAccessEvaluationTest() throws BallerinaTestException;

    // 9. XML attribute access expressions
    public abstract void xmlAttributeAccessEvaluationTest() throws BallerinaTestException;

    // 10. Annotation access expressions
    public abstract void annotationAccessEvaluationTest() throws BallerinaTestException;

    // 11. Member access expressions
    public abstract void memberAccessEvaluationTest() throws BallerinaTestException;

    // 12. Function call expressions
    public abstract void functionCallEvaluationTest() throws BallerinaTestException;

    // 13. Method call expressions
    public abstract void methodCallEvaluationTest() throws BallerinaTestException;

    // 14. Error constructor expressions
    public abstract void errorConstructorEvaluationTest() throws BallerinaTestException;

    // 15. Anonymous function expressions
    public abstract void anonymousFunctionEvaluationTest() throws BallerinaTestException;

    // 16. Anonymous function expressions
    public abstract void letExpressionEvaluationTest() throws BallerinaTestException;

    // 17. Type cast expressions
    public abstract void typeCastEvaluationTest() throws BallerinaTestException;

    // 18. Typeof expressions
    public abstract void typeOfExpressionEvaluationTest() throws BallerinaTestException;

    // 19. Unary expressions
    public abstract void unaryExpressionEvaluationTest() throws BallerinaTestException;

    // 20. Multiplicative expressions
    public abstract void multiplicativeExpressionEvaluationTest() throws BallerinaTestException;

    // 21. Additive expressions
    public abstract void additiveExpressionEvaluationTest() throws BallerinaTestException;

    // 22. Shift expressions
    public abstract void shiftExpressionEvaluationTest() throws BallerinaTestException;

    // 23. Range expressions
    public abstract void rangeExpressionEvaluationTest() throws BallerinaTestException;

    // 24. Numerical comparison expressions
    public abstract void comparisonEvaluationTest() throws BallerinaTestException;

    // 25. Type test expressions
    public abstract void typeTestEvaluationTest() throws BallerinaTestException;

    // 26. Equality expressions
    public abstract void equalityEvaluationTest() throws BallerinaTestException;

    // 27. Binary bitwise expressions
    public abstract void binaryBitwiseEvaluationTest() throws BallerinaTestException;

    // 28. Logical expressions
    public abstract void logicalEvaluationTest() throws BallerinaTestException;

    // 29. Conditional expressions
    public abstract void conditionalExpressionEvaluationTest() throws BallerinaTestException;

    // 30. Checking expressions
    public abstract void checkingExpressionEvaluationTest() throws BallerinaTestException;

    // 31. Trap expressions
    public abstract void trapExpressionEvaluationTest() throws BallerinaTestException;

    // 32. Query expressions
    public abstract void queryExpressionEvaluationTest() throws BallerinaTestException;

    // 33. XML navigation expressions
    public abstract void xmlNavigationEvaluationTest() throws BallerinaTestException;
}
