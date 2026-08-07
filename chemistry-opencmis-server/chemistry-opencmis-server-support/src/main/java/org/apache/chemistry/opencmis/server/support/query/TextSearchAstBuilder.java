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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Builds an ANTLR3-compatible text-search {@link CmisTree}.
 */
public class TextSearchAstBuilder {

    public CmisTree build(ParseTree tree) {
        if (tree instanceof TextSearchParser.Text_search_expressionContext) {
            return buildExpression((TextSearchParser.Text_search_expressionContext) tree);
        }
        if (tree instanceof TextSearchParser.ConjunctContext) {
            return buildConjunct((TextSearchParser.ConjunctContext) tree);
        }
        if (tree instanceof TextSearchParser.TermContext) {
            return buildTerm((TextSearchParser.TermContext) tree);
        }
        if (tree instanceof TextSearchParser.PhraseContext) {
            return leaf(((TextSearchParser.PhraseContext) tree).TEXT_SEARCH_PHRASE_STRING_LIT().getSymbol());
        }
        if (tree instanceof TextSearchParser.WordContext) {
            return leaf(((TextSearchParser.WordContext) tree).TEXT_SEARCH_WORD_LIT().getSymbol());
        }
        if (tree instanceof TerminalNode) {
            return leaf(((TerminalNode) tree).getSymbol());
        }
        throw new IllegalArgumentException("Unsupported text-search tree: " + tree.getClass().getName());
    }

    private CmisTree buildExpression(TextSearchParser.Text_search_expressionContext ctx) {
        if (ctx.OR() == null || ctx.OR().isEmpty()) {
            return buildConjunct(ctx.conjunct(0));
        }
        CmisCommonTree or = imag(TextSearchLexer.TEXT_OR, "TEXT_OR", tokenIndex(ctx.conjunct(0).start));
        for (TextSearchParser.ConjunctContext c : ctx.conjunct()) {
            or.addChild(buildConjunct(c));
        }
        return or;
    }

    private CmisTree buildConjunct(TextSearchParser.ConjunctContext ctx) {
        if (ctx.term().size() == 1) {
            return buildTerm(ctx.term(0));
        }
        CmisCommonTree and = imag(TextSearchLexer.TEXT_AND, "TEXT_AND", tokenIndex(ctx.term(0).start));
        for (TextSearchParser.TermContext t : ctx.term()) {
            and.addChild(buildTerm(t));
        }
        return and;
    }

    private CmisTree buildTerm(TextSearchParser.TermContext ctx) {
        CmisTree wordOrPhrase;
        if (ctx.word() != null) {
            wordOrPhrase = leaf(ctx.word().TEXT_SEARCH_WORD_LIT().getSymbol());
        } else {
            wordOrPhrase = leaf(ctx.phrase().TEXT_SEARCH_PHRASE_STRING_LIT().getSymbol());
        }
        if (ctx.TEXT_MINUS() != null) {
            CmisCommonTree minus = node(TextSearchLexer.TEXT_MINUS, "-", ctx.TEXT_MINUS().getSymbol());
            minus.addChild(wordOrPhrase);
            return minus;
        }
        return wordOrPhrase;
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
}
