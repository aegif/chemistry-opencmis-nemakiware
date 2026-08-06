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
 * Builds an ANTLR3-compatible {@link CmisTree} for the Ext CMISQL grammar.
 */
public class CmisQlExtAstBuilder {

    public CmisTree build(ParseTree tree) {
        if (tree instanceof CmisQlExtParser.RootContext) {
            return build(((CmisQlExtParser.RootContext) tree).query());
        }
        if (tree instanceof CmisQlExtParser.QueryContext) {
            return buildQuery((CmisQlExtParser.QueryContext) tree);
        }
        if (tree instanceof CmisQlExtParser.Value_expressionContext) {
            return buildValueExpression((CmisQlExtParser.Value_expressionContext) tree);
        }
        // Reuse strict-shaped builder helpers via a temporary approach: convert by
        // walking Ext contexts that mirror Strict rule shapes.
        return buildGeneric(tree);
    }

    private CmisTree buildQuery(CmisQlExtParser.QueryContext ctx) {
        // ANTLR3 kept the matched token, so the node text preserves input casing
        CmisCommonTree select = node(CmisQlExtLexer.SELECT, ctx.SELECT().getText(), ctx.SELECT().getSymbol());
        if (ctx.DISTINCT() != null) {
            select.addChild(leaf(ctx.DISTINCT().getSymbol()));
        }
        select.addChild(buildSelectList(ctx.select_list()));
        select.addChild(buildFromClause(ctx.from_clause()));
        if (ctx.where_clause() != null) {
            select.addChild(buildWhereClause(ctx.where_clause()));
        }
        if (ctx.order_by_clause() != null) {
            select.addChild(buildOrderBy(ctx.order_by_clause()));
        }
        return select;
    }

    private CmisTree buildGeneric(ParseTree tree) {
        if (tree instanceof CmisQlExtParser.Select_listContext) {
            return buildSelectList((CmisQlExtParser.Select_listContext) tree);
        }
        if (tree instanceof CmisQlExtParser.Column_referenceContext) {
            return buildColumnReference((CmisQlExtParser.Column_referenceContext) tree);
        }
        if (tree instanceof CmisQlExtParser.String_value_functionContext) {
            return buildStringFunc((CmisQlExtParser.String_value_functionContext) tree);
        }
        if (tree instanceof CmisQlExtParser.Numeric_value_functionContext) {
            return imag(CmisQlExtLexer.SCORE, "SCORE",
                    tokenIndex(((CmisQlExtParser.Numeric_value_functionContext) tree).SCORE().getSymbol()));
        }
        if (tree instanceof TerminalNode) {
            return leaf(((TerminalNode) tree).getSymbol());
        }
        throw new IllegalArgumentException("Unsupported Ext parse tree: " + tree.getClass().getName());
    }

    private CmisTree buildSelectList(CmisQlExtParser.Select_listContext ctx) {
        if (ctx.STAR() != null) {
            return leaf(ctx.STAR().getSymbol());
        }
        CmisCommonTree list = imag(CmisQlExtLexer.SEL_LIST, "SEL_LIST", firstTokenIndex(ctx));
        for (CmisQlExtParser.Select_sublistContext sub : ctx.select_sublist()) {
            list.addChild(buildSelectSublist(sub));
        }
        return list;
    }

    private CmisTree buildSelectSublist(CmisQlExtParser.Select_sublistContext ctx) {
        if (ctx.STAR() != null) {
            CmisCommonTree nil = new CmisCommonTree(Token.INVALID_TYPE, "nil", tokenIndex(ctx.qualifier().start));
            nil.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
            nil.addChild(leaf(ctx.DOT().getSymbol()));
            nil.addChild(leaf(ctx.STAR().getSymbol()));
            return nil;
        }
        CmisTree value = buildValueExpression(ctx.value_expression());
        if (ctx.column_name() != null) {
            CmisCommonTree nil = new CmisCommonTree(Token.INVALID_TYPE, "nil", tokenIndex(ctx.start));
            nil.addChild(value);
            nil.addChild(leaf(ctx.column_name().ID().getSymbol()));
            return nil;
        }
        return value;
    }

    private CmisTree buildValueExpression(CmisQlExtParser.Value_expressionContext ctx) {
        if (ctx.column_reference() != null) {
            return buildColumnReference(ctx.column_reference());
        }
        if (ctx.string_value_function() != null) {
            return buildStringFunc(ctx.string_value_function());
        }
        return imag(CmisQlExtLexer.SCORE, "SCORE", tokenIndex(ctx.numeric_value_function().SCORE().getSymbol()));
    }

    private CmisTree buildStringFunc(CmisQlExtParser.String_value_functionContext ctx) {
        CmisCommonTree func = imag(CmisQlExtLexer.FUNC, "FUNC", tokenIndex(ctx.ID().getSymbol()));
        func.addChild(leaf(ctx.ID().getSymbol()));
        func.addChild(buildColumnReference(ctx.column_reference()));
        return func;
    }

    private CmisTree buildColumnReference(CmisQlExtParser.Column_referenceContext ctx) {
        CmisCommonTree col = imag(CmisQlExtLexer.COL, "COL", firstTokenIndex(ctx));
        if (ctx.qualifier() != null) {
            col.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
        }
        col.addChild(leaf(ctx.column_name().ID().getSymbol()));
        return col;
    }

    private CmisTree buildFromClause(CmisQlExtParser.From_clauseContext ctx) {
        CmisCommonTree from = node(CmisQlExtLexer.FROM, ctx.FROM().getText(), ctx.FROM().getSymbol());
        CmisQlExtParser.Table_referenceContext tr = ctx.table_reference();
        from.addChild(buildOneTable(tr.one_table()));
        for (CmisQlExtParser.Table_joinContext join : tr.table_join()) {
            from.addChild(buildTableJoin(join));
        }
        return from;
    }

    private CmisTree buildTableJoin(CmisQlExtParser.Table_joinContext ctx) {
        CmisCommonTree join = node(CmisQlExtLexer.JOIN, "JOIN", ctx.join_kind().start);
        join.addChild(buildJoinKind(ctx.join_kind()));
        join.addChild(buildOneTable(ctx.one_table()));
        if (ctx.join_specification() != null) {
            CmisQlExtParser.Join_specificationContext js = ctx.join_specification();
            CmisCommonTree on = node(CmisQlExtLexer.ON, "ON", js.ON().getSymbol());
            on.addChild(buildColumnReference(js.column_reference(0)));
            on.addChild(leaf(js.EQ().getSymbol()));
            on.addChild(buildColumnReference(js.column_reference(1)));
            join.addChild(on);
        }
        return join;
    }

    private CmisTree buildJoinKind(CmisQlExtParser.Join_kindContext ctx) {
        if (ctx.LEFT() != null) {
            return imag(CmisQlExtLexer.LEFT, "LEFT", tokenIndex(ctx.LEFT().getSymbol()));
        }
        if (ctx.RIGHT() != null) {
            return imag(CmisQlExtLexer.RIGHT, "RIGHT", tokenIndex(ctx.RIGHT().getSymbol()));
        }
        Token t = ctx.INNER() != null ? ctx.INNER().getSymbol() : ctx.start;
        return imag(CmisQlExtLexer.INNER, "INNER", tokenIndex(t));
    }

    private CmisTree buildOneTable(CmisQlExtParser.One_tableContext ctx) {
        if (ctx.table_reference() != null) {
            CmisQlExtParser.Table_referenceContext tr = ctx.table_reference();
            if (tr.table_join().isEmpty()) {
                return buildOneTable(tr.one_table());
            }
            CmisCommonTree nil = new CmisCommonTree(Token.INVALID_TYPE, "nil", firstTokenIndex(tr));
            nil.addChild(buildOneTable(tr.one_table()));
            for (CmisQlExtParser.Table_joinContext join : tr.table_join()) {
                nil.addChild(buildTableJoin(join));
            }
            return nil;
        }
        CmisCommonTree table = imag(CmisQlExtLexer.TABLE, "TABLE", tokenIndex(ctx.table_name().ID().getSymbol()));
        table.addChild(leaf(ctx.table_name().ID().getSymbol()));
        if (ctx.correlation_name() != null) {
            table.addChild(leaf(ctx.correlation_name().ID().getSymbol()));
        }
        return table;
    }

    private CmisTree buildWhereClause(CmisQlExtParser.Where_clauseContext ctx) {
        // For Ext tests we mainly need query/value_expression; provide a minimal WHERE
        CmisCommonTree where = node(CmisQlExtLexer.WHERE, ctx.WHERE().getText(), ctx.WHERE().getSymbol());
        where.addChild(buildSearchCondition(ctx.search_condition()));
        return where;
    }

    private CmisTree buildSearchCondition(CmisQlExtParser.Search_conditionContext ctx) {
        CmisTree result = buildBooleanTerm(ctx.boolean_term(0));
        for (int i = 1; i < ctx.boolean_term().size(); i++) {
            CmisCommonTree or = node(CmisQlExtLexer.OR, ctx.OR(i - 1).getText(), ctx.OR(i - 1).getSymbol());
            or.addChild(result);
            or.addChild(buildBooleanTerm(ctx.boolean_term(i)));
            result = or;
        }
        return result;
    }

    private CmisTree buildBooleanTerm(CmisQlExtParser.Boolean_termContext ctx) {
        CmisTree result = buildBooleanFactor(ctx.boolean_factor(0));
        for (int i = 1; i < ctx.boolean_factor().size(); i++) {
            CmisCommonTree and = node(CmisQlExtLexer.AND, ctx.AND(i - 1).getText(), ctx.AND(i - 1).getSymbol());
            and.addChild(result);
            and.addChild(buildBooleanFactor(ctx.boolean_factor(i)));
            result = and;
        }
        return result;
    }

    private CmisTree buildBooleanFactor(CmisQlExtParser.Boolean_factorContext ctx) {
        if (ctx.NOT() != null) {
            CmisCommonTree not = node(CmisQlExtLexer.NOT, ctx.NOT().getText(), ctx.NOT().getSymbol());
            not.addChild(buildBooleanTest(ctx.boolean_test()));
            return not;
        }
        return buildBooleanTest(ctx.boolean_test());
    }

    private CmisTree buildBooleanTest(CmisQlExtParser.Boolean_testContext ctx) {
        if (ctx.search_condition() != null) {
            return buildSearchCondition(ctx.search_condition());
        }
        return buildPredicate(ctx.predicate());
    }

    private CmisTree buildPredicate(CmisQlExtParser.PredicateContext ctx) {
        if (ctx.comparison_predicate() != null) {
            return buildComparison(ctx.comparison_predicate());
        }
        if (ctx.in_predicate() != null) {
            return buildIn(ctx.in_predicate());
        }
        if (ctx.like_predicate() != null) {
            return buildLike(ctx.like_predicate());
        }
        if (ctx.null_predicate() != null) {
            return buildNull(ctx.null_predicate());
        }
        if (ctx.quantified_comparison_predicate() != null) {
            return buildQuantCmp(ctx.quantified_comparison_predicate());
        }
        if (ctx.quantified_in_predicate() != null) {
            return buildQuantIn(ctx.quantified_in_predicate());
        }
        if (ctx.text_search_predicate() != null) {
            CmisQlExtParser.Text_search_predicateContext t = ctx.text_search_predicate();
            CmisCommonTree root = node(CmisQlExtLexer.CONTAINS, t.CONTAINS().getText(), t.CONTAINS().getSymbol());
            if (t.qualifier() != null) {
                root.addChild(leaf(t.qualifier().table_name().ID().getSymbol()));
            }
            root.addChild(leaf(t.text_search_expression().STRING_LIT().getSymbol()));
            return root;
        }
        CmisQlExtParser.Folder_predicateContext f = ctx.folder_predicate();
        Token ft = f.IN_FOLDER() != null ? f.IN_FOLDER().getSymbol() : f.IN_TREE().getSymbol();
        CmisCommonTree root = node(ft.getType(), ft.getText(), ft);
        if (f.qualifier() != null) {
            root.addChild(leaf(f.qualifier().table_name().ID().getSymbol()));
        }
        root.addChild(leaf(f.folder_id().STRING_LIT().getSymbol()));
        return root;
    }

    private CmisTree buildComparison(CmisQlExtParser.Comparison_predicateContext ctx) {
        Token op = ctx.EQ() != null ? ctx.EQ().getSymbol()
                : ctx.NEQ() != null ? ctx.NEQ().getSymbol()
                        : ctx.LT() != null ? ctx.LT().getSymbol()
                                : ctx.GT() != null ? ctx.GT().getSymbol()
                                        : ctx.LTEQ() != null ? ctx.LTEQ().getSymbol() : ctx.GTEQ().getSymbol();
        CmisCommonTree root = node(op.getType(), op.getText(), op);
        root.addChild(buildValueExpression(ctx.value_expression()));
        root.addChild(buildLiteral(ctx.literal()));
        return root;
    }

    private CmisTree buildIn(CmisQlExtParser.In_predicateContext ctx) {
        CmisCommonTree root = ctx.NOT() != null
                ? imag(CmisQlExtLexer.NOT_IN, "NOT_IN", tokenIndex(ctx.NOT().getSymbol()))
                : node(CmisQlExtLexer.IN, ctx.IN().getText(), ctx.IN().getSymbol());
        root.addChild(buildColumnReference(ctx.column_reference()));
        root.addChild(buildInList(ctx.in_value_list()));
        return root;
    }

    private CmisTree buildLike(CmisQlExtParser.Like_predicateContext ctx) {
        CmisCommonTree root = ctx.NOT() != null
                ? imag(CmisQlExtLexer.NOT_LIKE, "NOT_LIKE", tokenIndex(ctx.NOT().getSymbol()))
                : node(CmisQlExtLexer.LIKE, ctx.LIKE().getText(), ctx.LIKE().getSymbol());
        root.addChild(buildColumnReference(ctx.column_reference()));
        root.addChild(leaf(ctx.STRING_LIT().getSymbol()));
        return root;
    }

    private CmisTree buildNull(CmisQlExtParser.Null_predicateContext ctx) {
        CmisCommonTree root = ctx.NOT() != null
                ? imag(CmisQlExtLexer.IS_NOT_NULL, "IS_NOT_NULL", tokenIndex(ctx.IS().getSymbol()))
                : imag(CmisQlExtLexer.IS_NULL, "IS_NULL", tokenIndex(ctx.IS().getSymbol()));
        root.addChild(buildColumnReference(ctx.column_reference()));
        return root;
    }

    private CmisTree buildQuantCmp(CmisQlExtParser.Quantified_comparison_predicateContext ctx) {
        // Ext: ^(OP_ANY comp_op literal mvcol)
        CmisCommonTree root = imag(CmisQlExtLexer.OP_ANY, "OP_ANY", firstTokenIndex(ctx));
        Token op = ctx.comp_op().start;
        root.addChild(leaf(op));
        root.addChild(buildLiteral(ctx.literal()));
        root.addChild(buildMvCol(ctx.multi_valued_column_reference()));
        return root;
    }

    private CmisTree buildQuantIn(CmisQlExtParser.Quantified_in_predicateContext ctx) {
        CmisCommonTree root = ctx.NOT() != null
                ? imag(CmisQlExtLexer.NOT_IN_ANY, "NOT_IN_ANY", tokenIndex(ctx.ANY().getSymbol()))
                : imag(CmisQlExtLexer.IN_ANY, "IN_ANY", tokenIndex(ctx.ANY().getSymbol()));
        root.addChild(buildMvCol(ctx.multi_valued_column_reference()));
        root.addChild(buildInList(ctx.in_value_list()));
        return root;
    }

    private CmisTree buildMvCol(CmisQlExtParser.Multi_valued_column_referenceContext ctx) {
        CmisCommonTree col = imag(CmisQlExtLexer.COL, "COL", firstTokenIndex(ctx));
        if (ctx.qualifier() != null) {
            col.addChild(leaf(ctx.qualifier().table_name().ID().getSymbol()));
        }
        col.addChild(leaf(ctx.multi_valued_column_name().ID().getSymbol()));
        return col;
    }

    private CmisTree buildInList(CmisQlExtParser.In_value_listContext ctx) {
        CmisCommonTree list = imag(CmisQlExtLexer.IN_LIST, "IN_LIST", firstTokenIndex(ctx));
        for (CmisQlExtParser.LiteralContext lit : ctx.literal()) {
            list.addChild(buildLiteral(lit));
        }
        return list;
    }

    private CmisTree buildLiteral(CmisQlExtParser.LiteralContext ctx) {
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

    private CmisTree buildOrderBy(CmisQlExtParser.Order_by_clauseContext ctx) {
        CmisCommonTree orderBy = imag(CmisQlExtLexer.ORDER_BY, "ORDER_BY", tokenIndex(ctx.ORDER().getSymbol()));
        for (CmisQlExtParser.Sort_specificationContext sort : ctx.sort_specification()) {
            orderBy.addChild(buildColumnReference(sort.column_reference()));
            if (sort.DESC() != null) {
                orderBy.addChild(leaf(sort.DESC().getSymbol()));
            } else if (sort.ASC() != null) {
                orderBy.addChild(leaf(sort.ASC().getSymbol()));
            } else {
                orderBy.addChild(imag(CmisQlExtLexer.ASC, "ASC", firstTokenIndex(sort)));
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
