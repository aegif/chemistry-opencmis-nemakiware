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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Builds an ANTLR3-compatible {@link CmisTree} from an ANTLR4 Strict parse
 * tree, matching historic AST rewrite shapes.
 */
public class CmisQlAstBuilder {

    public CmisTree build(ParseTree tree) {
        if (tree == null) {
            return null;
        }
        if (tree instanceof CmisQlStrictParser.RootContext) {
            return build(((CmisQlStrictParser.RootContext) tree).query());
        }
        if (tree instanceof CmisQlStrictParser.QueryContext) {
            return buildQuery((CmisQlStrictParser.QueryContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Select_listContext) {
            return buildSelectList((CmisQlStrictParser.Select_listContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Select_sublistContext) {
            return buildSelectSublist((CmisQlStrictParser.Select_sublistContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Value_expressionContext) {
            return buildValueExpression((CmisQlStrictParser.Value_expressionContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Column_referenceContext) {
            return buildColumnReference((CmisQlStrictParser.Column_referenceContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Multi_valued_column_referenceContext) {
            return buildMultiValuedColumnReference((CmisQlStrictParser.Multi_valued_column_referenceContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Numeric_value_functionContext) {
            return buildScore((CmisQlStrictParser.Numeric_value_functionContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.From_clauseContext) {
            return buildFromClause((CmisQlStrictParser.From_clauseContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Table_referenceContext) {
            return buildTableReference((CmisQlStrictParser.Table_referenceContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Table_joinContext) {
            return buildTableJoin((CmisQlStrictParser.Table_joinContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.One_tableContext) {
            return buildOneTable((CmisQlStrictParser.One_tableContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Join_specificationContext) {
            return buildJoinSpecification((CmisQlStrictParser.Join_specificationContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Where_clauseContext) {
            return buildWhereClause((CmisQlStrictParser.Where_clauseContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Search_conditionContext) {
            return buildSearchCondition((CmisQlStrictParser.Search_conditionContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Boolean_termContext) {
            return buildBooleanTerm((CmisQlStrictParser.Boolean_termContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Boolean_factorContext) {
            return buildBooleanFactor((CmisQlStrictParser.Boolean_factorContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Boolean_testContext) {
            return buildBooleanTest((CmisQlStrictParser.Boolean_testContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.PredicateContext) {
            return buildPredicate((CmisQlStrictParser.PredicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Comparison_predicateContext) {
            return buildComparison((CmisQlStrictParser.Comparison_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.In_predicateContext) {
            return buildInPredicate((CmisQlStrictParser.In_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.In_value_listContext) {
            return buildInValueList((CmisQlStrictParser.In_value_listContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Like_predicateContext) {
            return buildLikePredicate((CmisQlStrictParser.Like_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Null_predicateContext) {
            return buildNullPredicate((CmisQlStrictParser.Null_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Quantified_comparison_predicateContext) {
            return buildQuantifiedComparison((CmisQlStrictParser.Quantified_comparison_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Quantified_in_predicateContext) {
            return buildQuantifiedIn((CmisQlStrictParser.Quantified_in_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Text_search_predicateContext) {
            return buildTextSearchPredicate((CmisQlStrictParser.Text_search_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Folder_predicateContext) {
            return buildFolderPredicate((CmisQlStrictParser.Folder_predicateContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.Order_by_clauseContext) {
            return buildOrderBy((CmisQlStrictParser.Order_by_clauseContext) tree);
        }
        if (tree instanceof CmisQlStrictParser.LiteralContext) {
            return buildLiteral((CmisQlStrictParser.LiteralContext) tree);
        }
        if (tree instanceof TerminalNode) {
            return leaf(((TerminalNode) tree).getSymbol());
        }
        throw new IllegalArgumentException("Unsupported parse tree: " + tree.getClass().getName());
    }

    private CmisTree buildQuery(CmisQlStrictParser.QueryContext ctx) {
        // ANTLR3 kept the matched token, so the node text preserves input casing
        CmisCommonTree select = node(CmisQlStrictLexer.SELECT, ctx.SELECT().getText(), ctx.SELECT().getSymbol());
        select.addChild(buildSelectList(ctx.select_list()));
        select.addChild(buildFromClause(ctx.from_clause()));
        // ANTLR3 rewrite placed order_by before where
        if (ctx.order_by_clause() != null) {
            select.addChild(buildOrderBy(ctx.order_by_clause()));
        }
        if (ctx.where_clause() != null) {
            select.addChild(buildWhereClause(ctx.where_clause()));
        }
        return select;
    }

    private CmisTree buildSelectList(CmisQlStrictParser.Select_listContext ctx) {
        if (ctx.STAR() != null) {
            return leaf(ctx.STAR().getSymbol());
        }
        CmisCommonTree list = imag(CmisQlStrictLexer.SEL_LIST, "SEL_LIST", firstTokenIndex(ctx));
        for (CmisQlStrictParser.Select_sublistContext sub : ctx.select_sublist()) {
            list.addChild(buildSelectSublist(sub));
        }
        return list;
    }

    private CmisTree buildSelectSublist(CmisQlStrictParser.Select_sublistContext ctx) {
        // Alternatives without rewrite rules produced a nil-rooted flat tree in
        // ANTLR3; CmisCommonTree.addChild splices nil children into the parent,
        // so under SEL_LIST these become flat siblings, matching ANTLR3 output.
        if (ctx.STAR() != null) {
            // qualifier DOT STAR → flat tokens: ID . *
            CmisCommonTree nil = new CmisCommonTree(Token.INVALID_TYPE, "nil", tokenIndex(ctx.qualifier().start));
            nil.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
            nil.addChild(leaf(ctx.DOT().getSymbol()));
            nil.addChild(leaf(ctx.STAR().getSymbol()));
            return nil;
        }
        CmisTree value = buildValueExpression(ctx.value_expression());
        if (ctx.column_name() != null) {
            // value_expression (AS!? column_name)? → value and alias as flat siblings
            CmisCommonTree nil = new CmisCommonTree(Token.INVALID_TYPE, "nil", tokenIndex(ctx.start));
            nil.addChild(value);
            nil.addChild(leaf(ctx.column_name().ID().getSymbol()));
            return nil;
        }
        return value;
    }

    private CmisTree buildValueExpression(CmisQlStrictParser.Value_expressionContext ctx) {
        if (ctx.column_reference() != null) {
            return buildColumnReference(ctx.column_reference());
        }
        return buildScore(ctx.numeric_value_function());
    }

    private CmisTree buildColumnReference(CmisQlStrictParser.Column_referenceContext ctx) {
        CmisCommonTree col = imag(CmisQlStrictLexer.COL, "COL", firstTokenIndex(ctx));
        if (ctx.qualifier() != null) {
            col.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
        }
        col.addChild(leaf(ctx.column_name().ID().getSymbol()));
        return col;
    }

    private CmisTree buildMultiValuedColumnReference(CmisQlStrictParser.Multi_valued_column_referenceContext ctx) {
        CmisCommonTree col = imag(CmisQlStrictLexer.COL, "COL", firstTokenIndex(ctx));
        if (ctx.qualifier() != null) {
            col.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
        }
        col.addChild(leaf(ctx.multi_valued_column_name().ID().getSymbol()));
        return col;
    }

    private CmisTree buildScore(CmisQlStrictParser.Numeric_value_functionContext ctx) {
        return imag(CmisQlStrictLexer.SCORE, "SCORE", tokenIndex(ctx.SCORE().getSymbol()));
    }

    private CmisTree buildFromClause(CmisQlStrictParser.From_clauseContext ctx) {
        CmisCommonTree from = node(CmisQlStrictLexer.FROM, ctx.FROM().getText(), ctx.FROM().getSymbol());
        // Flatten table_reference children into FROM
        CmisQlStrictParser.Table_referenceContext tr = ctx.table_reference();
        from.addChild(buildOneTable(tr.one_table()));
        for (CmisQlStrictParser.Table_joinContext join : tr.table_join()) {
            from.addChild(buildTableJoin(join));
        }
        return from;
    }

    private CmisTree buildTableReference(CmisQlStrictParser.Table_referenceContext ctx) {
        // Only used if called directly — produce nil with children
        CmisCommonTree nil = new CmisCommonTree(Token.INVALID_TYPE, "nil", firstTokenIndex(ctx));
        nil.addChild(buildOneTable(ctx.one_table()));
        for (CmisQlStrictParser.Table_joinContext join : ctx.table_join()) {
            nil.addChild(buildTableJoin(join));
        }
        return nil;
    }

    private CmisTree buildTableJoin(CmisQlStrictParser.Table_joinContext ctx) {
        CmisCommonTree join = node(CmisQlStrictLexer.JOIN, "JOIN", findJoinToken(ctx.join_kind()));
        join.addChild(buildJoinKind(ctx.join_kind()));
        join.addChild(buildOneTable(ctx.one_table()));
        if (ctx.join_specification() != null) {
            join.addChild(buildJoinSpecification(ctx.join_specification()));
        }
        return join;
    }

    private Token findJoinToken(CmisQlStrictParser.Join_kindContext ctx) {
        if (ctx.JOIN() != null) {
            return ctx.JOIN().getSymbol();
        }
        return ctx.start;
    }

    private CmisTree buildJoinKind(CmisQlStrictParser.Join_kindContext ctx) {
        if (ctx.LEFT() != null) {
            return imag(CmisQlStrictLexer.LEFT, "LEFT", tokenIndex(ctx.LEFT().getSymbol()));
        }
        if (ctx.RIGHT() != null) {
            return imag(CmisQlStrictLexer.RIGHT, "RIGHT", tokenIndex(ctx.RIGHT().getSymbol()));
        }
        // JOIN alone or INNER JOIN → INNER
        Token t = ctx.INNER() != null ? ctx.INNER().getSymbol() : findJoinToken(ctx);
        return imag(CmisQlStrictLexer.INNER, "INNER", tokenIndex(t));
    }

    private CmisTree buildOneTable(CmisQlStrictParser.One_tableContext ctx) {
        if (ctx.table_reference() != null) {
            // ( table_reference ) — unwrap, return first table (possibly with joins under FROM already)
            // When one_table is parenthesized table_reference, ANTLR3 LPAR! RPAR! unwraps to
            // the table_reference content. For `(foo)` → (TABLE foo). For joins inside parens,
            // return the table_reference structure.
            CmisQlStrictParser.Table_referenceContext tr = ctx.table_reference();
            if (tr.table_join().isEmpty()) {
                return buildOneTable(tr.one_table());
            }
            return buildTableReference(tr);
        }
        CmisCommonTree table = imag(CmisQlStrictLexer.TABLE, "TABLE", tokenIndex(ctx.table_name().ID().getSymbol()));
        table.addChild(leaf(ctx.table_name().ID().getSymbol()));
        if (ctx.correlation_name() != null) {
            table.addChild(leaf(ctx.correlation_name().ID().getSymbol()));
        }
        return table;
    }

    private CmisTree buildJoinSpecification(CmisQlStrictParser.Join_specificationContext ctx) {
        CmisCommonTree on = node(CmisQlStrictLexer.ON, "ON", ctx.ON().getSymbol());
        on.addChild(buildColumnReference(ctx.column_reference(0)));
        on.addChild(leaf(ctx.EQ().getSymbol()));
        on.addChild(buildColumnReference(ctx.column_reference(1)));
        return on;
    }

    private CmisTree buildWhereClause(CmisQlStrictParser.Where_clauseContext ctx) {
        CmisCommonTree where = node(CmisQlStrictLexer.WHERE, ctx.WHERE().getText(), ctx.WHERE().getSymbol());
        where.addChild(buildSearchCondition(ctx.search_condition()));
        return where;
    }

    private CmisTree buildSearchCondition(CmisQlStrictParser.Search_conditionContext ctx) {
        CmisTree result = buildBooleanTerm(ctx.boolean_term(0));
        for (int i = 1; i < ctx.boolean_term().size(); i++) {
            CmisCommonTree or = node(CmisQlStrictLexer.OR, ctx.OR(i - 1).getText(), ctx.OR(i - 1).getSymbol());
            or.addChild(result);
            or.addChild(buildBooleanTerm(ctx.boolean_term(i)));
            result = or;
        }
        return result;
    }

    private CmisTree buildBooleanTerm(CmisQlStrictParser.Boolean_termContext ctx) {
        CmisTree result = buildBooleanFactor(ctx.boolean_factor(0));
        for (int i = 1; i < ctx.boolean_factor().size(); i++) {
            CmisCommonTree and = node(CmisQlStrictLexer.AND, ctx.AND(i - 1).getText(), ctx.AND(i - 1).getSymbol());
            and.addChild(result);
            and.addChild(buildBooleanFactor(ctx.boolean_factor(i)));
            result = and;
        }
        return result;
    }

    private CmisTree buildBooleanFactor(CmisQlStrictParser.Boolean_factorContext ctx) {
        if (ctx.NOT() != null) {
            CmisCommonTree not = node(CmisQlStrictLexer.NOT, ctx.NOT().getText(), ctx.NOT().getSymbol());
            not.addChild(buildBooleanTest(ctx.boolean_test()));
            return not;
        }
        return buildBooleanTest(ctx.boolean_test());
    }

    private CmisTree buildBooleanTest(CmisQlStrictParser.Boolean_testContext ctx) {
        if (ctx.predicate() != null) {
            return buildPredicate(ctx.predicate());
        }
        return buildSearchCondition(ctx.search_condition());
    }

    private CmisTree buildPredicate(CmisQlStrictParser.PredicateContext ctx) {
        if (ctx.comparison_predicate() != null) {
            return buildComparison(ctx.comparison_predicate());
        }
        if (ctx.in_predicate() != null) {
            return buildInPredicate(ctx.in_predicate());
        }
        if (ctx.like_predicate() != null) {
            return buildLikePredicate(ctx.like_predicate());
        }
        if (ctx.null_predicate() != null) {
            return buildNullPredicate(ctx.null_predicate());
        }
        if (ctx.quantified_comparison_predicate() != null) {
            return buildQuantifiedComparison(ctx.quantified_comparison_predicate());
        }
        if (ctx.quantified_in_predicate() != null) {
            return buildQuantifiedIn(ctx.quantified_in_predicate());
        }
        if (ctx.text_search_predicate() != null) {
            return buildTextSearchPredicate(ctx.text_search_predicate());
        }
        return buildFolderPredicate(ctx.folder_predicate());
    }

    private CmisTree buildComparison(CmisQlStrictParser.Comparison_predicateContext ctx) {
        Token op;
        int type;
        if (ctx.EQ() != null) {
            op = ctx.EQ().getSymbol();
            type = CmisQlStrictLexer.EQ;
        } else if (ctx.NEQ() != null) {
            op = ctx.NEQ().getSymbol();
            type = CmisQlStrictLexer.NEQ;
        } else if (ctx.LT() != null) {
            op = ctx.LT().getSymbol();
            type = CmisQlStrictLexer.LT;
        } else if (ctx.GT() != null) {
            op = ctx.GT().getSymbol();
            type = CmisQlStrictLexer.GT;
        } else if (ctx.LTEQ() != null) {
            op = ctx.LTEQ().getSymbol();
            type = CmisQlStrictLexer.LTEQ;
        } else {
            op = ctx.GTEQ().getSymbol();
            type = CmisQlStrictLexer.GTEQ;
        }
        CmisCommonTree root = node(type, op.getText(), op);
        root.addChild(buildValueExpression(ctx.value_expression()));
        root.addChild(buildLiteral(ctx.literal()));
        return root;
    }

    private CmisTree buildLiteral(CmisQlStrictParser.LiteralContext ctx) {
        if (ctx.NUM_LIT() != null) {
            return leaf(ctx.NUM_LIT().getSymbol());
        }
        if (ctx.STRING_LIT() != null) {
            return leaf(ctx.STRING_LIT().getSymbol());
        }
        if (ctx.TIME_LIT() != null) {
            return leaf(ctx.TIME_LIT().getSymbol());
        }
        return leaf(ctx.BOOL_LIT().getSymbol());
    }

    private CmisTree buildInPredicate(CmisQlStrictParser.In_predicateContext ctx) {
        CmisCommonTree root;
        if (ctx.NOT() != null) {
            root = imag(CmisQlStrictLexer.NOT_IN, "NOT_IN", tokenIndex(ctx.NOT().getSymbol()));
        } else {
            root = node(CmisQlStrictLexer.IN, ctx.IN().getText(), ctx.IN().getSymbol());
        }
        root.addChild(buildColumnReference(ctx.column_reference()));
        root.addChild(buildInValueList(ctx.in_value_list()));
        return root;
    }

    private CmisTree buildInValueList(CmisQlStrictParser.In_value_listContext ctx) {
        CmisCommonTree list = imag(CmisQlStrictLexer.IN_LIST, "IN_LIST", firstTokenIndex(ctx));
        for (CmisQlStrictParser.LiteralContext lit : ctx.literal()) {
            list.addChild(buildLiteral(lit));
        }
        return list;
    }

    private CmisTree buildLikePredicate(CmisQlStrictParser.Like_predicateContext ctx) {
        CmisCommonTree root;
        if (ctx.NOT() != null) {
            root = imag(CmisQlStrictLexer.NOT_LIKE, "NOT_LIKE", tokenIndex(ctx.NOT().getSymbol()));
        } else {
            root = node(CmisQlStrictLexer.LIKE, ctx.LIKE().getText(), ctx.LIKE().getSymbol());
        }
        root.addChild(buildColumnReference(ctx.column_reference()));
        root.addChild(leaf(ctx.STRING_LIT().getSymbol()));
        return root;
    }

    private CmisTree buildNullPredicate(CmisQlStrictParser.Null_predicateContext ctx) {
        CmisCommonTree root;
        if (ctx.NOT() != null) {
            root = imag(CmisQlStrictLexer.IS_NOT_NULL, "IS_NOT_NULL", tokenIndex(ctx.IS().getSymbol()));
        } else {
            root = imag(CmisQlStrictLexer.IS_NULL, "IS_NULL", tokenIndex(ctx.IS().getSymbol()));
        }
        root.addChild(buildColumnReference(ctx.column_reference()));
        return root;
    }

    private CmisTree buildQuantifiedComparison(CmisQlStrictParser.Quantified_comparison_predicateContext ctx) {
        CmisCommonTree root = imag(CmisQlStrictLexer.EQ_ANY, "EQ_ANY", tokenIndex(ctx.EQ().getSymbol()));
        root.addChild(buildLiteral(ctx.literal()));
        root.addChild(buildMultiValuedColumnReference(ctx.multi_valued_column_reference()));
        return root;
    }

    private CmisTree buildQuantifiedIn(CmisQlStrictParser.Quantified_in_predicateContext ctx) {
        CmisCommonTree root;
        if (ctx.NOT() != null) {
            root = imag(CmisQlStrictLexer.NOT_IN_ANY, "NOT_IN_ANY", tokenIndex(ctx.ANY().getSymbol()));
        } else {
            root = imag(CmisQlStrictLexer.IN_ANY, "IN_ANY", tokenIndex(ctx.ANY().getSymbol()));
        }
        root.addChild(buildMultiValuedColumnReference(ctx.multi_valued_column_reference()));
        root.addChild(buildInValueList(ctx.in_value_list()));
        return root;
    }

    private CmisTree buildTextSearchPredicate(CmisQlStrictParser.Text_search_predicateContext ctx) {
        CmisCommonTree root = node(CmisQlStrictLexer.CONTAINS, ctx.CONTAINS().getText(), ctx.CONTAINS().getSymbol());
        if (ctx.qualifier() != null) {
            root.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
        }
        root.addChild(leaf(ctx.text_search_expression().STRING_LIT().getSymbol()));
        return root;
    }

    private CmisTree buildFolderPredicate(CmisQlStrictParser.Folder_predicateContext ctx) {
        Token f = ctx.IN_FOLDER() != null ? ctx.IN_FOLDER().getSymbol() : ctx.IN_TREE().getSymbol();
        CmisCommonTree root = node(f.getType(), f.getText(), f);
        if (ctx.qualifier() != null) {
            root.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
        }
        root.addChild(leaf(ctx.folder_id().STRING_LIT().getSymbol()));
        return root;
    }

    private CmisTree buildOrderBy(CmisQlStrictParser.Order_by_clauseContext ctx) {
        CmisCommonTree orderBy = imag(CmisQlStrictLexer.ORDER_BY, "ORDER_BY", tokenIndex(ctx.ORDER().getSymbol()));
        for (CmisQlStrictParser.Sort_specificationContext sort : ctx.sort_specification()) {
            orderBy.addChild(buildColumnReference(sort.column_reference()));
            if (sort.DESC() != null) {
                orderBy.addChild(leaf(sort.DESC().getSymbol()));
            } else if (sort.ASC() != null) {
                orderBy.addChild(leaf(sort.ASC().getSymbol()));
            } else {
                // default ASC inserted as imaginary leaf with text ASC
                orderBy.addChild(imag(CmisQlStrictLexer.ASC, "ASC", firstTokenIndex(sort)));
            }
        }
        return orderBy;
    }

    private static CmisCommonTree node(int type, String text, Token token) {
        return new CmisCommonTree(type, text, tokenIndex(token));
    }

    private static CmisCommonTree imag(int type, String text, int tokenStartIndex) {
        return new CmisCommonTree(type, text, tokenStartIndex);
    }

    private static CmisCommonTree leaf(Token token) {
        return new CmisCommonTree(token.getType(), token.getText(), tokenIndex(token));
    }

    private static int tokenIndex(Token token) {
        return token == null ? -1 : token.getTokenIndex();
    }

    private static int firstTokenIndex(ParserRuleContext ctx) {
        return ctx == null || ctx.start == null ? -1 : ctx.start.getTokenIndex();
    }
}
