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

/**
 * Mutable tree node compatible with ANTLR3 {@code CommonTree} usage in CMIS
 * query processing.
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

    @Override
    public void addChild(CmisTree child) {
        if (child != null) {
            child.setParent(this);
            children.add(child);
        }
    }

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
        sb.append('(');
        sb.append(text);
        for (CmisTree child : children) {
            sb.append(' ');
            sb.append(child.toStringTree());
        }
        sb.append(')');
        return sb.toString();
    }
}
