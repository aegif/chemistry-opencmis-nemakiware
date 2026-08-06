/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.inmemory.query;

import java.util.List;

import org.apache.chemistry.opencmis.server.support.query.CmisTree;
import org.apache.chemistry.opencmis.server.support.query.PredicateWalkerBase;

/**
 * An interface used by the walker when traversing the AST from the grammar. The
 * interface consists of callback methods that are called when a rule is
 * processed (as part of the WHERE statement)
 */
public interface QueryConditionProcessor extends PredicateWalkerBase {

    void onStartProcessing(CmisTree whereNode);

    void onStopProcessing();

    // Compare operators
    void onEquals(CmisTree eqNode, CmisTree leftNode, CmisTree rightNode);

    void onNotEquals(CmisTree neNode, CmisTree leftNode, CmisTree rightNode);

    void onGreaterThan(CmisTree gtNode, CmisTree leftNode, CmisTree rightNode);

    void onGreaterOrEquals(CmisTree geNode, CmisTree leftNode, CmisTree rightNode);

    void onLessThan(CmisTree ltNode, CmisTree leftNode, CmisTree rightNode);

    void onLessOrEquals(CmisTree leqNode, CmisTree leftNode, CmisTree rightNode);

    // Boolean operators
    void onPreNot(CmisTree opNode, CmisTree leftNode);

    void onNot(CmisTree opNode, CmisTree leftNode);

    void onPostNot(CmisTree opNode, CmisTree leftNode);

    void onPreAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    void onAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    void onPostAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    void onPreOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    void onOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    void onPostOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    // Multi-value:
    void onIn(CmisTree node, CmisTree colNode, CmisTree listNode);

    void onNotIn(CmisTree node, CmisTree colNode, CmisTree listNode);

    void onInAny(CmisTree node, CmisTree colNode, CmisTree listNode);

    void onNotInAny(CmisTree node, CmisTree colNode, CmisTree listNode);

    void onEqAny(CmisTree node, CmisTree literalNode, CmisTree colNode);

    // Null comparisons:
    void onIsNull(CmisTree nullNode, CmisTree colNode);

    void onIsNotNull(CmisTree notNullNode, CmisTree colNode);

    // String matching:
    void onIsLike(CmisTree node, CmisTree colNode, CmisTree stringNode);

    void onIsNotLike(CmisTree node, CmisTree colNode, CmisTree stringNode);

    // Functions:
    void onContains(CmisTree node, CmisTree typeNode, CmisTree searchExprNode);

    void onInFolder(CmisTree node, CmisTree colNode, CmisTree paramNode);

    void onInTree(CmisTree node, CmisTree colNode, CmisTree paramNode);

    void onScore(CmisTree node);

    // full text search
    void onTextAnd(CmisTree node, List<CmisTree> conjunctionNodes);

    void onTextOr(CmisTree node, List<CmisTree> termNodes);

    void onTextMinus(CmisTree node, CmisTree notNode);

    void onTextWord(String word);

    void onTextPhrase(String phrase);
}
