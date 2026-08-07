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

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.chemistry.opencmis.server.support.query.CmisTree;
import org.apache.chemistry.opencmis.server.support.query.CalendarHelper;
import org.apache.chemistry.opencmis.server.support.query.CmisQlStrictLexer;
import org.apache.chemistry.opencmis.server.support.query.PredicateWalkerBase;
import org.apache.chemistry.opencmis.server.support.query.StringUtil;
import org.apache.chemistry.opencmis.server.support.query.TextSearchLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractQueryConditionProcessor implements PredicateWalkerBase {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessQueryTest.class);

    protected abstract void onStartProcessing(CmisTree whereNode);

    protected abstract void onStopProcessing();

    // Compare operators
    protected void onPreEquals(CmisTree eqNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onEquals(CmisTree eqNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostEquals(CmisTree eqNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected void onPreNotEquals(CmisTree neNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onNotEquals(CmisTree neNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostNotEquals(CmisTree neNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected void onPreGreaterThan(CmisTree gtNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onGreaterThan(CmisTree gtNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostGreaterThan(CmisTree gtNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected void onPreGreaterOrEquals(CmisTree geNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onGreaterOrEquals(CmisTree geNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostGreaterOrEquals(CmisTree geNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected void onPreLessThan(CmisTree ltNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onLessThan(CmisTree ltNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostLessThan(CmisTree ltNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected void onPreLessOrEquals(CmisTree leqNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onLessOrEquals(CmisTree leqNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostLessOrEquals(CmisTree leqNode, CmisTree leftNode, CmisTree rightNode) {
    }

    // Boolean operators
    protected abstract void onNot(CmisTree opNode, CmisTree leftNode);

    protected void onPostNot(CmisTree opNode, CmisTree leftNode) {
    }

    protected void onPreAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostAnd(CmisTree opNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected void onPreOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode) {
    }

    protected abstract void onOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode);

    protected void onPostOr(CmisTree opNode, CmisTree leftNode, CmisTree rightNode) {
    }

    // Multi-value:
    protected void onPreIn(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected abstract void onIn(CmisTree node, CmisTree colNode, CmisTree listNode);

    protected void onPostIn(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected void onPreNotIn(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected abstract void onNotIn(CmisTree node, CmisTree colNode, CmisTree listNode);

    protected void onPostNotIn(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected void onPreInAny(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected abstract void onInAny(CmisTree node, CmisTree colNode, CmisTree listNode);

    protected void onPostInAny(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected void onPreNotInAny(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected abstract void onNotInAny(CmisTree node, CmisTree literalNode, CmisTree colNode);

    protected void onPostNotInAny(CmisTree node, CmisTree colNode, CmisTree listNode) {
    }

    protected void onPreEqAny(CmisTree node, CmisTree literalNode, CmisTree colNode) {
    }

    protected abstract void onEqAny(CmisTree node, CmisTree literalNode, CmisTree colNode);

    protected void onPostEqAny(CmisTree node, CmisTree literalNode, CmisTree colNode) {
    }

    // Null comparisons:
    protected abstract void onIsNull(CmisTree nullNode, CmisTree colNode);

    protected void onPostIsNull(CmisTree nullNode, CmisTree colNode) {
    }

    protected abstract void onIsNotNull(CmisTree notNullNode, CmisTree colNode);

    protected void onPostIsNotNull(CmisTree notNullNode, CmisTree colNode) {
    }

    // String matching:
    protected void onPreIsLike(CmisTree node, CmisTree colNode, CmisTree stringNode) {
    }

    protected abstract void onIsLike(CmisTree node, CmisTree colNode, CmisTree stringNode);

    protected void onPostIsLike(CmisTree node, CmisTree colNode, CmisTree stringNode) {
    }

    protected void onPreIsNotLike(CmisTree node, CmisTree colNode, CmisTree stringNode) {
    }

    protected abstract void onIsNotLike(CmisTree node, CmisTree colNode, CmisTree stringNode);

    protected void onPostIsNotLike(CmisTree node, CmisTree colNode, CmisTree stringNode) {
    }

    protected abstract void onInFolder(CmisTree node, CmisTree colNode, CmisTree paramNode);

    protected void onBetweenInFolder(CmisTree node, CmisTree colNode, CmisTree paramNode) {
    }

    protected void onPostInFolder(CmisTree node, CmisTree colNode, CmisTree paramNode) {
    }

    protected abstract void onInTree(CmisTree node, CmisTree colNode, CmisTree paramNode);

    protected void onBetweenInTree(CmisTree node, CmisTree colNode, CmisTree paramNode) {
    }

    protected void onPostInTree(CmisTree node, CmisTree colNode, CmisTree paramNode) {
    }

    protected abstract void onScore(CmisTree node);

    protected abstract void onColNode(CmisTree node);

    protected void onPreTextAnd(CmisTree node, List<CmisTree> conjunctionNodes) {
    }

    protected abstract void onTextAnd(CmisTree node, List<CmisTree> conjunctionNodes, int index);

    protected void onPostTextAnd(CmisTree node, List<CmisTree> conjunctionNodes) {
    }

    protected void onPreTextOr(CmisTree node, List<CmisTree> termNodes) {
    }

    protected abstract void onTextOr(CmisTree node, List<CmisTree> termNodes, int index);

    protected void onPostTextOr(CmisTree node, List<CmisTree> termNodes) {
    }

    protected abstract void onTextMinus(CmisTree node, CmisTree notNode);

    protected void onPostTextMinus(CmisTree node, CmisTree notNode) {
    }

    protected abstract void onTextWord(String word);

    protected abstract void onTextPhrase(String phrase);

    // Base interface called from query parser
    @Override
    public Boolean walkPredicate(CmisTree whereNode) {
        if (null != whereNode) {
            onStartProcessing(whereNode);
            evalWhereNode(whereNode);
            onStopProcessing();
        }
        return null; // unused
    }

    // ///////////////////////////////////////////////////////
    // Processing the WHERE clause

    protected void evalWhereNode(CmisTree node) {
        // Ensure that we receive only valid tokens and nodes in the where
        // clause:
        LOG.debug("evaluating node: " + node.toString());
        switch (node.getType()) {
        case CmisQlStrictLexer.WHERE:
            break; // ignore
        case CmisQlStrictLexer.EQ:
            onPreEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostEquals(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.NEQ:
            onPreNotEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onNotEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostNotEquals(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.GT:
            onPreGreaterThan(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onGreaterThan(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostGreaterThan(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.GTEQ:
            onPreGreaterOrEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onGreaterOrEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostGreaterOrEquals(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.LT:
            onPreLessThan(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onLessThan(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostLessThan(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.LTEQ:
            onPreLessOrEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onLessOrEquals(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostLessOrEquals(node, node.getChild(0), node.getChild(1));
            break;

        case CmisQlStrictLexer.NOT:
            onNot(node, node.getChild(0));
            evalWhereNode(node.getChild(0));
            onPostNot(node, node.getChild(0));
            break;
        case CmisQlStrictLexer.AND:
            onPreAnd(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onAnd(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostAnd(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.OR:
            onPreOr(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onOr(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostOr(node, node.getChild(0), node.getChild(1));
            break;

        // Multi-value:
        case CmisQlStrictLexer.IN:
            onPreIn(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onIn(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostIn(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.NOT_IN:
            onPreNotIn(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onNotIn(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostNotIn(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.IN_ANY:
            onPreInAny(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onInAny(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostInAny(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.NOT_IN_ANY:
            onPreNotInAny(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onNotInAny(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostNotInAny(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.EQ_ANY:
            onPreEqAny(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onEqAny(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostEqAny(node, node.getChild(0), node.getChild(1));
            break;

        // Null comparisons:
        case CmisQlStrictLexer.IS_NULL:
            onIsNull(node, node.getChild(0));
            evalWhereNode(node.getChild(0));
            onPostIsNull(node, node.getChild(0));
            break;
        case CmisQlStrictLexer.IS_NOT_NULL:
            onIsNotNull(node, node.getChild(0));
            evalWhereNode(node.getChild(0));
            onPostIsNotNull(node, node.getChild(0));
            break;

        // String matching
        case CmisQlStrictLexer.LIKE:
            onPreIsLike(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onIsLike(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostIsLike(node, node.getChild(0), node.getChild(1));
            break;
        case CmisQlStrictLexer.NOT_LIKE:
            onPreIsNotLike(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(0));
            onIsNotLike(node, node.getChild(0), node.getChild(1));
            evalWhereNode(node.getChild(1));
            onPostIsNotLike(node, node.getChild(0), node.getChild(1));
            break;

        // Functions
        case CmisQlStrictLexer.CONTAINS:
            CmisTree typeNode = node.getChildCount() == 1 ? null : node.getChild(0);
            CmisTree textSearchNode = node.getChildCount() == 1 ? node.getChild(0) : node.getChild(1);

            onPreContains(node, typeNode, textSearchNode);
            if (node.getChildCount() > 1) {
                evalWhereNode(typeNode);
                onBetweenContains(node, typeNode, textSearchNode);
            }
            onContains(node, typeNode, textSearchNode);
            break;
        case CmisQlStrictLexer.IN_FOLDER:
            if (node.getChildCount() == 1) {
                onInFolder(node, null, node.getChild(0));
                evalWhereNode(node.getChild(0));
                onPostInFolder(node, null, node.getChild(0));
            } else {
                onInFolder(node, node.getChild(0), node.getChild(1));
                evalWhereNode(node.getChild(0));
                onBetweenInFolder(node, node.getChild(0), node.getChild(1));
                evalWhereNode(node.getChild(1));
                onPostInFolder(node, node.getChild(0), node.getChild(1));
            }
            break;
        case CmisQlStrictLexer.IN_TREE:
            if (node.getChildCount() == 1) {
                onInTree(node, null, node.getChild(0));
                evalWhereNode(node.getChild(0));
                onPostInTree(node, null, node.getChild(0));
            } else {
                onInTree(node, node.getChild(0), node.getChild(1));
                evalWhereNode(node.getChild(0));
                onBetweenInTree(node, node.getChild(0), node.getChild(1));
                evalWhereNode(node.getChild(1));
                onPostInTree(node, node.getChild(0), node.getChild(1));
            }
            break;
        case CmisQlStrictLexer.SCORE:
            onScore(node);
            break;
        case CmisQlStrictLexer.COL:
            onColNode(node);
            break;
        case CmisQlStrictLexer.BOOL_LIT:
        case CmisQlStrictLexer.NUM_LIT:
        case CmisQlStrictLexer.STRING_LIT:
        case CmisQlStrictLexer.TIME_LIT:
            onLiteral(node);
            break;
        case CmisQlStrictLexer.IN_LIST:
            onLiteralList(node);
            break;
        case CmisQlStrictLexer.ID:
            onId(node);
            break;
        default:
            // do nothing;
        }
    }

    protected void onPreContains(CmisTree node, CmisTree typeNode, CmisTree searchExprNode) {
    }

    protected void onContains(CmisTree node, CmisTree typeNode, CmisTree searchExprNode) {
        evalTextSearchNode(typeNode, searchExprNode);
    }

    protected void onBetweenContains(CmisTree node, CmisTree typeNode, CmisTree searchExprNode) {
    }

    protected void evalTextSearchNode(CmisTree typeNode, CmisTree node) {
        // Ensure that we receive only valid tokens and nodes in the where
        // clause:
        LOG.debug("evaluating node: " + node.toString());
        switch (node.getType()) {
        case TextSearchLexer.TEXT_AND:
            List<CmisTree> children = getChildrenAsList(node);
            onPreTextAnd(node, children);
            int i = 0;
            for (CmisTree child : children) {
                evalTextSearchNode(typeNode, child);
                onTextAnd(node, children, i++);
            }
            onPostTextAnd(node, children);
            break;
        case TextSearchLexer.TEXT_OR:
            children = getChildrenAsList(node);
            onPreTextOr(node, children);
            int j = 0;
            for (CmisTree child : children) {
                evalTextSearchNode(typeNode, child);
                onTextOr(node, children, j++);
            }
            onPostTextOr(node, children);
            break;
        case TextSearchLexer.TEXT_MINUS:
            onTextMinus(node, node.getChild(0));
            evalTextSearchNode(typeNode, node.getChild(0));
            onPostTextMinus(node, node.getChild(0));
            break;
        case TextSearchLexer.TEXT_SEARCH_PHRASE_STRING_LIT:
            onTextPhrase(onTextLiteral(node));
            break;
        case TextSearchLexer.TEXT_SEARCH_WORD_LIT:
            onTextWord(onTextLiteral(node));
            break;
        }
    }

    // helper functions that are needed by most query tree walkers

    protected Object getLiteral(CmisTree node) {
        int type = node.getType();
        String text = node.getText();
        switch (type) {
        case CmisQlStrictLexer.BOOL_LIT:
            return Boolean.parseBoolean(node.getText());
        case CmisQlStrictLexer.NUM_LIT:
            if (text.contains(".") || text.contains("e") || text.contains("E")) {
                return Double.parseDouble(text);
            } else {
                return Long.parseLong(text);
            }
        case CmisQlStrictLexer.STRING_LIT:
            return text.substring(1, text.length() - 1);
        case CmisQlStrictLexer.TIME_LIT:
            GregorianCalendar gc = CalendarHelper.fromString(text.substring(text.indexOf('\'') + 1,
                    text.lastIndexOf('\'')));
            return gc;
        default:
            throw new RuntimeException("Unknown literal. " + node);
        }
    }

    protected Object onLiteral(CmisTree node) {
        return getLiteral(node);
    }

    protected String onId(CmisTree node) {
        return node.getText();
    }

    protected String onTextLiteral(CmisTree node) {
        int type = node.getType();
        String text = node.getText();
        switch (type) {
        case TextSearchLexer.TEXT_SEARCH_PHRASE_STRING_LIT:
            return StringUtil.unescape(text.substring(1, text.length() - 1), null);
        case TextSearchLexer.TEXT_SEARCH_WORD_LIT:
            return StringUtil.unescape(text, null);
        default:
            throw new RuntimeException("Unknown text literal. " + node);
        }
    }

    protected List<Object> onLiteralList(CmisTree node) {
        List<Object> res = new ArrayList<Object>(node.getChildCount());
        for (int i = 0; i < node.getChildCount(); i++) {
            CmisTree literal = node.getChild(i);
            res.add(getLiteral(literal));
        }
        return res;
    }

    protected List<CmisTree> getChildrenAsList(CmisTree node) {
        List<CmisTree> res = new ArrayList<CmisTree>(node.getChildCount());
        for (int i = 0; i < node.getChildCount(); i++) {
            CmisTree childNnode = node.getChild(i);
            res.add(childNnode);
        }
        return res;
    }
}
