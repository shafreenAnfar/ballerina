/*
 *   Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.statements.retrystmt;

import org.ballerinalang.model.values.BValue;
import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test cases for flow validation in Retry Statement.
 */
public class RetryStmtTest {

    private CompileResult programFile, managerNegative, retryStmtNegative;

    @BeforeClass
    public void setup() {
        programFile = BCompileUtil.compile("test-src/statements/retrystmt/retry_stmt.bal");
        managerNegative = BCompileUtil.compile("test-src/statements/retrystmt/retry_manager_negative.bal");
        retryStmtNegative = BCompileUtil.compile("test-src/statements/retrystmt/retry_stmt_negative.bal");
    }

    @Test
    public void testRetryStatement() {
        BValue[] params = {};
        BRunUtil.invoke(programFile, "testRetryStatement", params);
    }

    @Test(description = "Test retry manager with errors")
    public void testRetryManagerNegativeCases() {
        Assert.assertEquals(managerNegative.getErrorCount(), 2);
        int index = 0;
        BAssertUtil.validateError(managerNegative, index++, "unknown type 'MyRetryMgr'", 10, 11);
        BAssertUtil.validateError(managerNegative, index++, "undefined symbol 'value'", 16, 28);
    }

    @Test(description = "Test retry statement with errors")
    public void testRetryStatementNegativeCases() {
        Assert.assertEquals(retryStmtNegative.getErrorCount(), 2);
        int index = 0;
        BAssertUtil.validateError(retryStmtNegative, index++, "no implementation found for the function " +
                "'shouldRetry' of non-abstract object 'CustomRetryManager'", 16, 10);
        BAssertUtil.validateError(retryStmtNegative, index++, "unreachable code", 27, 5);
    }
}
