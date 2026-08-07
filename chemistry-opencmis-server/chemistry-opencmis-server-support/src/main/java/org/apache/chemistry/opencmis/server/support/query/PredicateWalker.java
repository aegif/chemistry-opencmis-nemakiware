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
 *
 * Contributors:
 *     Florent Guillaume, Nuxeo
 */
package org.apache.chemistry.opencmis.server.support.query;


/**
 * Interface for a tree walker of a WHERE clause.
 * <p>
 * Can be used to build another datastructure, or for direct value evaluation
 * (thus the boolean return values for clauses, and Object for values).
 * <p>
 * The method {@link #walkExpr} is the entry point.
 * <p>
 * <b>Migration (ANTLR4):</b> Node types are {@link CmisTree}, not ANTLR3
 * {@code org.antlr.runtime.tree.Tree}. Custom walkers must recompile against
 * this interface.
 */
public interface PredicateWalker extends PredicateWalkerBase {

    Boolean walkNot(CmisTree opNode, CmisTree leftNode);

    Boolean walkAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    Object walkExpr(CmisTree node);

    Boolean walkEquals(CmisTree eqNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkNotEquals(CmisTree neNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkGreaterThan(CmisTree gtNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkGreaterOrEquals(CmisTree geNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkLessThan(CmisTree ltNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkLessOrEquals(CmisTree leqNode, CmisTree leftNode, CmisTree rightNode);

    Boolean walkIn(CmisTree node, CmisTree colNode, CmisTree listNode);

    Boolean walkNotIn(CmisTree node, CmisTree colNode, CmisTree listNode);

    Boolean walkInAny(CmisTree node, CmisTree colNode, CmisTree listNode);

    Boolean walkNotInAny(CmisTree node, CmisTree colNode, CmisTree listNode);

    Boolean walkEqAny(CmisTree node, CmisTree literalNode, CmisTree colNode);

    Boolean walkIsNull(CmisTree nullNode, CmisTree colNode);

    Boolean walkIsNotNull(CmisTree notNullNode, CmisTree colNode);

    Boolean walkLike(CmisTree node, CmisTree colNode, CmisTree stringNode);

    Boolean walkNotLike(CmisTree node, CmisTree colNode, CmisTree stringNode);

    Boolean walkContains(CmisTree node, CmisTree qualNode, CmisTree paramNode);

    Boolean walkInFolder(CmisTree node, CmisTree qualNode, CmisTree paramNode);

    Boolean walkInTree(CmisTree node, CmisTree qualNode, CmisTree paramNode);

    Object walkList(CmisTree node);

    Object walkBoolean(CmisTree node);

    Object walkNumber(CmisTree node);

    Object walkString(CmisTree node);

    Object walkTimestamp(CmisTree node);

    Object walkCol(CmisTree node);

    Object walkId(CmisTree node);

}
