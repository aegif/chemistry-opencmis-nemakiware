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

import java.math.BigDecimal;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

/**
 * Hand-written replacement for the ANTLR3 {@code CmisQueryWalker} tree grammar.
 * Walks a compatibility {@link CmisTree} AST, fills {@link QueryObject}, and
 * optionally grafts a TextSearch AST into CONTAINS nodes.
 */
public class CmisQueryWalker {

    private QueryObject queryObj;
    private CmisTree wherePredicateTree;
    private CmisTree ast;
    private boolean doFullTextParse = true;
    private int noContains = 0;

    public CmisTree getWherePredicateTree() {
        return wherePredicateTree;
    }

    public CmisTree getAst() {
        return ast;
    }

    /**
     * Compatibility shim for tests that previously used
     * {@code walker.getTreeNodeStream().getTreeSource()}.
     */
    public TreeSource getTreeNodeStream() {
        return new TreeSource(ast);
    }

    public void setDoFullTextParse(boolean value) {
        doFullTextParse = value;
    }

    public boolean getDoFullTextParse() {
        return doFullTextParse;
    }

    public int getNumberOfContainsClauses() {
        return noContains;
    }

    public void query(CmisTree tree, QueryObject qo, PredicateWalkerBase pw) throws RecognitionException {
        this.ast = tree;
        this.queryObj = qo;
        if (tree == null || tree.getType() != CmisQlStrictLexer.SELECT) {
            throw new RecognitionException("Expected SELECT root", null, null, null);
        }

        int idx = 0;
        CmisTree selectList = tree.getChild(idx++);
        walkSelectList(selectList);

        CmisTree fromClause = tree.getChild(idx++);
        walkFromClause(fromClause);

        CmisTree whereClause = null;
        CmisTree orderByClause = null;
        while (idx < tree.getChildCount()) {
            CmisTree child = tree.getChild(idx++);
            if (child.getType() == CmisQlStrictLexer.ORDER_BY) {
                orderByClause = child;
            } else if (child.getType() == CmisQlStrictLexer.WHERE) {
                whereClause = child;
            }
        }

        if (orderByClause != null) {
            walkOrderBy(orderByClause);
        }
        if (whereClause != null) {
            wherePredicateTree = whereClause.getChildCount() > 0 ? whereClause.getChild(0) : null;
            walkSearchCondition(wherePredicateTree);
        } else {
            wherePredicateTree = null;
        }

        boolean resolved = queryObj.resolveTypes();
        if (pw != null && wherePredicateTree != null) {
            pw.walkPredicate(wherePredicateTree);
        }
        if (!resolved) {
            FailedPredicateException e = new FailedPredicateException(queryObj.getErrorMessage());
            throw e;
        }
    }

    public void query(QueryObject qo, PredicateWalkerBase pw) throws RecognitionException {
        query(ast, qo, pw);
    }

    private void walkSelectList(CmisTree node) {
        if (node.getType() == CmisQlStrictLexer.STAR) {
            queryObj.addSelectReference(node, new ColumnReference(node.getText()));
            return;
        }
        if (node.getType() == CmisQlStrictLexer.SEL_LIST) {
            for (int i = 0; i < node.getChildCount(); i++) {
                walkSelectSublist(node.getChild(i));
            }
        }
    }

    private void walkSelectSublist(CmisTree node) {
        // qualifier DOT STAR
        if (node.getChildCount() >= 3 && node.getChild(1).getType() == CmisQlStrictLexer.DOT
                && node.getChild(2).getType() == CmisQlStrictLexer.STAR) {
            String qualifier = node.getChild(0).getText();
            queryObj.addSelectReference(node.getChild(0), new ColumnReference(qualifier, node.getChild(2).getText()));
            return;
        }
        // also handle without nil: if node itself is unusual

        CmisTree valueNode;
        String alias = null;
        if (node.getType() == Token.INVALID_TYPE || "nil".equals(node.getText())) {
            valueNode = node.getChild(0);
            if (node.getChildCount() > 1) {
                alias = node.getChild(1).getText();
            }
        } else {
            valueNode = node;
        }

        CmisSelector sel = walkValueExpression(valueNode);
        queryObj.addSelectReference(valueNode, sel);
        if (alias != null) {
            queryObj.addAlias(alias, sel);
        }
    }

    private CmisSelector walkValueExpression(CmisTree node) {
        if (node.getType() == CmisQlStrictLexer.SCORE) {
            return new FunctionReference(FunctionReference.CmisQlFunction.SCORE);
        }
        return walkColumnReference(node);
    }

    private ColumnReference walkColumnReference(CmisTree node) {
        if (node.getType() != CmisQlStrictLexer.COL) {
            throw new CmisQueryException("Expected COL node, got type " + node.getType());
        }
        if (node.getChildCount() == 2) {
            return new ColumnReference(node.getChild(0).getText(), node.getChild(1).getText());
        }
        return new ColumnReference(null, node.getChild(0).getText());
    }

    private void walkFromClause(CmisTree node) {
        // FROM one_table table_join*
        if (node.getChildCount() == 0) {
            return;
        }
        walkOneTable(node.getChild(0));
        for (int i = 1; i < node.getChildCount(); i++) {
            walkTableJoin(node.getChild(i));
        }
    }

    private String walkOneTable(CmisTree node) throws RecognitionException {
        if (node.getType() != CmisQlStrictLexer.TABLE) {
            // parenthesized join list etc.
            if (node.getChildCount() > 0) {
                String alias = walkOneTable(node.getChild(0));
                for (int i = 1; i < node.getChildCount(); i++) {
                    walkTableJoin(node.getChild(i));
                }
                return alias;
            }
            throw new CmisQueryException("Expected TABLE node");
        }
        String tableName = node.getChild(0).getText();
        String corr = node.getChildCount() > 1 ? node.getChild(1).getText() : null;
        String alias = queryObj.addType(corr, tableName);
        if (alias == null) {
            throw new FailedPredicateException(queryObj.getErrorMessage());
        }
        return alias;
    }

    private void walkTableJoin(CmisTree node) throws RecognitionException {
        if (node.getType() != CmisQlStrictLexer.JOIN) {
            return;
        }
        String kind = node.getChild(0).getText();
        String alias = walkOneTable(node.getChild(1));
        boolean hasSpec = node.getChildCount() > 2;
        // Match ANTLR3 tree-grammar order: walk join_specification before addJoin
        // so joinReferences are already populated when hasSpec is true.
        if (hasSpec) {
            walkJoinSpecification(node.getChild(2));
        }
        queryObj.addJoin(kind, alias, hasSpec);
    }

    private void walkJoinSpecification(CmisTree node) {
        // ON col EQ col
        ColumnReference cr1 = walkColumnReference(node.getChild(0));
        ColumnReference cr2 = walkColumnReference(node.getChild(2));
        queryObj.addJoinReference(node.getChild(0), cr1);
        queryObj.addJoinReference(node.getChild(2), cr2);
    }

    private void walkOrderBy(CmisTree node) {
        // ORDER_BY (col ASC|DESC)+
        for (int i = 0; i < node.getChildCount();) {
            CmisTree colNode = node.getChild(i++);
            ColumnReference colRef = walkColumnReference(colNode);
            boolean ascending = true;
            if (i < node.getChildCount()) {
                CmisTree dir = node.getChild(i);
                if (dir.getType() == CmisQlStrictLexer.ASC || dir.getType() == CmisQlStrictLexer.DESC) {
                    ascending = dir.getType() == CmisQlStrictLexer.ASC;
                    i++;
                }
            }
            queryObj.addSortCriterium(colNode, colRef, ascending);
        }
    }

    private void walkSearchCondition(CmisTree node) {
        if (node == null) {
            return;
        }
        switch (node.getType()) {
        case CmisQlStrictLexer.OR:
        case CmisQlStrictLexer.AND:
            walkSearchCondition(node.getChild(0));
            walkSearchCondition(node.getChild(1));
            break;
        case CmisQlStrictLexer.NOT:
            walkSearchCondition(node.getChild(0));
            break;
        case CmisQlStrictLexer.EQ:
        case CmisQlStrictLexer.NEQ:
        case CmisQlStrictLexer.LT:
        case CmisQlStrictLexer.GT:
        case CmisQlStrictLexer.GTEQ:
        case CmisQlStrictLexer.LTEQ:
        case CmisQlStrictLexer.LIKE:
        case CmisQlStrictLexer.NOT_LIKE:
            walkSearchCondition(node.getChild(0));
            walkSearchCondition(node.getChild(1));
            break;
        case CmisQlStrictLexer.IS_NULL:
        case CmisQlStrictLexer.IS_NOT_NULL:
            walkSearchCondition(node.getChild(0));
            break;
        case CmisQlStrictLexer.EQ_ANY: {
            ColumnReference mvcr = walkColumnReference(node.getChild(1));
            queryObj.addWhereReference(node.getChild(1), mvcr);
            break;
        }
        case CmisQlStrictLexer.IN_ANY:
        case CmisQlStrictLexer.NOT_IN_ANY: {
            ColumnReference mvcr = walkColumnReference(node.getChild(0));
            queryObj.addWhereReference(node.getChild(0), mvcr);
            break;
        }
        case CmisQlStrictLexer.CONTAINS: {
            CmisTree qual = null;
            CmisTree textExpr;
            if (node.getChildCount() == 1) {
                textExpr = node.getChild(0);
            } else {
                qual = node.getChild(0);
                textExpr = node.getChild(1);
            }
            if (qual != null) {
                queryObj.addWhereTypeReference(qual, qual.getText());
            } else {
                queryObj.addWhereTypeReference(null, null);
            }
            ++noContains;
            if (doFullTextParse && textExpr.getType() == CmisQlStrictLexer.STRING_LIT) {
                CmisTree tse = parseTextSearchPredicate(textExpr.getText());
                // graft into CONTAINS child
                int childIdx = node.getChildCount() == 1 ? 0 : 1;
                ((CmisCommonTree) node).setChild(childIdx, tse);
            }
            break;
        }
        case CmisQlStrictLexer.IN_FOLDER:
        case CmisQlStrictLexer.IN_TREE: {
            if (node.getChildCount() == 2) {
                CmisTree qual = node.getChild(0);
                queryObj.addWhereTypeReference(qual, qual.getText());
            } else {
                queryObj.addWhereTypeReference(null, null);
            }
            break;
        }
        case CmisQlStrictLexer.IN:
        case CmisQlStrictLexer.NOT_IN: {
            ColumnReference col = walkColumnReference(node.getChild(0));
            queryObj.addWhereReference(node.getChild(0), col);
            break;
        }
        case CmisQlStrictLexer.COL:
        case CmisQlStrictLexer.SCORE: {
            CmisSelector sel = walkValueExpression(node);
            queryObj.addWhereReference(node, sel);
            break;
        }
        case CmisQlStrictLexer.NUM_LIT:
        case CmisQlStrictLexer.STRING_LIT:
        case CmisQlStrictLexer.TIME_LIT:
        case CmisQlStrictLexer.BOOL_LIT:
        case CmisQlStrictLexer.IN_LIST:
            // literals / lists — nothing to register
            break;
        default:
            // recurse into children for safety
            for (int i = 0; i < node.getChildCount(); i++) {
                walkSearchCondition(node.getChild(i));
            }
            break;
        }
    }

    private static CmisTree parseTextSearchPredicate(String expr) {
        String unescapedExpr = StringUtil.unescape(expr.substring(1, expr.length() - 1), null);
        TextSearchLexer lexer = new TextSearchLexer(CharStreams.fromString(unescapedExpr));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TextSearchParser parser = new TextSearchParser(tokens);
        parser.removeErrorListeners();
        CollectingErrorListener errors = new CollectingErrorListener();
        parser.addErrorListener(errors);
        try {
            TextSearchParser.Text_search_expressionContext ctx = parser.text_search_expression();
            if (errors.hasErrors()) {
                throw new RuntimeException(errors.getErrorMessages().trim());
            }
            return new TextSearchAstBuilder().build(ctx);
        } catch (RecognitionException e) {
            String hdr = "Error in text search expression, line " + e.getOffendingToken().getLine() + ":"
                    + e.getOffendingToken().getCharPositionInLine();
            throw new RuntimeException(hdr + " " + e.getMessage(), e);
        }
    }

    /**
     * Compatibility holder for {@code getTreeNodeStream().getTreeSource()}.
     */
    public static final class TreeSource {
        private final CmisTree tree;

        TreeSource(CmisTree tree) {
            this.tree = tree;
        }

        public CmisTree getTreeSource() {
            return tree;
        }
    }

    private static final class ThrowingErrorListener extends CollectingErrorListener {
        @Override
        public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                int charPositionInLine, String msg, RecognitionException e) {
            super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
            throw new RuntimeException(e != null ? e : new RuntimeException(msg));
        }
    }

    // silence unused warning for literal parsing parity with old walker
    @SuppressWarnings("unused")
    private static Object literalValue(CmisTree node) {
        switch (node.getType()) {
        case CmisQlStrictLexer.NUM_LIT:
            try {
                return Long.valueOf(node.getText());
            } catch (NumberFormatException e) {
                return new BigDecimal(node.getText());
            }
        case CmisQlStrictLexer.STRING_LIT:
            String s = node.getText();
            return s != null ? s.substring(1, s.length() - 1) : null;
        case CmisQlStrictLexer.BOOL_LIT:
            return Boolean.valueOf(node.getText());
        default:
            return node.getText();
        }
    }
}
