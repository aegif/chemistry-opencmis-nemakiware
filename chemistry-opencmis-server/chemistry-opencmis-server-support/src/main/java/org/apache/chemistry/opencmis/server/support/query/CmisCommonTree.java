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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

/**
 * Mutable tree node compatible with ANTLR3 {@code CommonTree} usage in CMIS
 * query processing.
 *
 * <p>Mirrors ANTLR3 semantics for "nil" nodes (token type
 * {@link Token#INVALID_TYPE}): adding a nil node as a child splices its
 * children into the parent, and a nil root renders its children
 * space-separated without enclosing parentheses.</p>
 */
public class CmisCommonTree implements CmisTree {

    private final int type;
    private final String text;
    private int tokenStartIndex = -1;
    private CmisTree parent;
    private final List<CmisTree> children = new ArrayList<CmisTree>();

    public CmisCommonTree(int type, String text) {
        this.type = type;
        this.text = text;
    }

    public CmisCommonTree(int type, String text, int tokenStartIndex) {
        this.type = type;
        this.text = text;
        this.tokenStartIndex = tokenStartIndex;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public CmisTree getChild(int i) {
        return children.get(i);
    }

    @Override
    public int getTokenStartIndex() {
        return tokenStartIndex;
    }

    @Override
    public void setTokenStartIndex(int index) {
        this.tokenStartIndex = index;
    }

    @Override
    public CmisTree getParent() {
        return parent;
    }

    @Override
    public void setParent(CmisTree parent) {
        this.parent = parent;
    }

    public boolean isNil() {
        return type == Token.INVALID_TYPE;
    }

    @Override
    public void addChild(CmisTree child) {
        if (child == null) {
            return;
        }
        // ANTLR3 BaseTree.addChild semantics: adding a nil tree splices its
        // children into this node instead of adding the nil node itself.
        if (child instanceof CmisCommonTree && ((CmisCommonTree) child).isNil()) {
            for (CmisTree grandChild : ((CmisCommonTree) child).children) {
                grandChild.setParent(this);
                children.add(grandChild);
            }
            return;
        }
        child.setParent(this);
        children.add(child);
    }

    /**
     * Replaces the child at {@code i}. Unlike {@link #addChild}, nil nodes are
     * <em>not</em> spliced — callers (e.g. {@code CmisQueryWalker}) replace a
     * slot with a concrete typed node.
     */
    @Override
    public void setChild(int i, CmisTree child) {
        if (child != null) {
            child.setParent(this);
        }
        children.set(i, child);
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public String toStringTree() {
        if (children.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        // ANTLR3 CommonTree.toStringTree: a nil root prints only its children.
        if (!isNil()) {
            sb.append('(');
            sb.append(text);
        }
        boolean first = isNil();
        for (CmisTree child : children) {
            if (!first) {
                sb.append(' ');
            }
            first = false;
            sb.append(child.toStringTree());
        }
        if (!isNil()) {
            sb.append(')');
        }
        return sb.toString();
    }
}
