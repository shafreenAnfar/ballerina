/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.ballerinalang.compiler.tree.statements;

import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.statements.BlockNode;
import org.ballerinalang.model.tree.statements.LockNode;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for the lock tree node.
 *
 * @since 0.961.0
 */
public class BLangLock extends BLangStatement implements LockNode {

    public BLangBlockStmt body;

    public Set<BVarSymbol> lockVariables = new HashSet<>();

    public Map<BLangIndexBasedAccess.BLangStructFieldAccessExpr, Set<String>> fieldVariables = new HashMap<>();

    public Set<BVarSymbol> selfVariables = new HashSet<>();

    public BLangLock() {
    }

    @Override
    public BLangBlockStmt getBody() {
        return body;
    }

    @Override
    public void setBody(BlockNode body) {
        this.body = (BLangBlockStmt) body;
    }

    @Override
    public void accept(BLangNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.LOCK;
    }

    public boolean addLockVariable(BVarSymbol variable) {
        return lockVariables.add(variable);
    }

    public void addFieldVariable(BLangIndexBasedAccess.BLangStructFieldAccessExpr expr, String field) {
        fieldVariables.putIfAbsent(expr, new HashSet<>());
        fieldVariables.get(expr).add(field);
    }

    public void addSelfVariable(BVarSymbol symbol) {
        selfVariables.add(symbol);
    }

    @Override
    public String toString() {
        return "lock {"
                + (body != null ? String.valueOf(body) : "") + "}";
    }
}
