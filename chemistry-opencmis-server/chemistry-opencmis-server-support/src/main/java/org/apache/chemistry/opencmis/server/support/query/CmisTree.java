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
package org.apache.chemistry.opencmis.server.support.query;

/**
 * Minimal ANTLR3-compatible AST node API used by CMIS query walkers and
 * evaluators.
 * <p>
 * Replaces {@code org.antlr.runtime.tree.Tree} / {@code CommonTree} after the
 * ANTLR4 migration. Downstream walkers should depend on this type (and
 * {@link CmisCommonTree}), not on the ANTLR3 runtime.
 */
public interface CmisTree {

    int getType();

    String getText();

    int getChildCount();

    CmisTree getChild(int i);

    int getTokenStartIndex();

    void setTokenStartIndex(int index);

    CmisTree getParent();

    void setParent(CmisTree parent);

    void addChild(CmisTree child);

    void setChild(int i, CmisTree child);

    /**
     * ANTLR3 {@code CommonTree.toStringTree()} compatible formatting:
     * leaves return text; non-leaves return {@code (text child1 child2 ...)}.
     */
    String toStringTree();
}
