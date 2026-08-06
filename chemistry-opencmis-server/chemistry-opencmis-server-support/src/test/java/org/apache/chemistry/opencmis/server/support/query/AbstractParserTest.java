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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ANTLR4-based parser/lexer test harness replacing the ANTLR3 gUnit clone.
 */
public class AbstractParserTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractParserTest.class);

    protected enum GrammarKind {
        STRICT, EXT, TEXT_SEARCH
    }

    protected GrammarKind kind = GrammarKind.STRICT;

    protected void setUp(Class<?> lexerClass, Class<?> parserClass, String baseGrammar, String baseLexer) {
        if (lexerClass.getSimpleName().startsWith("TextSearch")) {
            kind = GrammarKind.TEXT_SEARCH;
        } else if (lexerClass.getSimpleName().startsWith("CmisQlExt")) {
            kind = GrammarKind.EXT;
        } else {
            kind = GrammarKind.STRICT;
        }
    }

    protected void tearDown() {
    }

    protected void testLexerOk(String rule, String statement) {
        try {
            execLexer(rule, statement);
            log.debug("testing rule {} parsed ok", rule);
        } catch (Exception e) {
            fail("testing rule " + rule + ": " + e.toString());
        }
    }

    protected void testLexerFail(String rule, String statement) {
        try {
            execLexer(rule, statement);
            fail("testing rule should fail " + rule);
        } catch (Exception e) {
            log.debug("testing rule {} parsed with exception: {}", rule, e.toString());
        }
    }

    protected void testParserOk(String rule, String statement) {
        try {
            Object retval = execParser(rule, statement);
            log.debug("testing rule {} parsed to: {}", rule, retval);
        } catch (Exception e) {
            fail("testing rule " + rule + " failed: " + e.toString());
        }
    }

    protected void testParserFail(String rule, String statement) {
        try {
            execParser(rule, statement);
            fail("testing rule should fail " + rule);
        } catch (Exception e) {
            log.debug("testing rule {} failed: {}", rule, e.toString());
        }
    }

    protected void testParser(String rule, String statement, String expectedResult) {
        try {
            Object actual = execParser(rule, statement);
            log.debug("testing rule {} parsed to: {}", rule, actual);
            if (expectedResult != null) {
                assertEquals(expectedResult.trim(), String.valueOf(actual).trim());
            }
        } catch (Exception e) {
            fail("testing rule " + rule + " failed: " + e);
        }
    }

    public String execLexer(String testRuleName, String testInput) throws Exception {
        CharStream input = CharStreams.fromString(testInput);
        Lexer lexer = createLexer(input);
        lexer.removeErrorListeners();
        CollectingErrorListener errors = new CollectingErrorListener();
        lexer.addErrorListener(errors);

        int expectedType = lexer.getTokenType(testRuleName);
        if (expectedType == Token.INVALID_TYPE) {
            throw new RuntimeException("Unknown lexer rule: " + testRuleName);
        }

        Token token = lexer.nextToken();
        while (token.getType() != Token.EOF && token.getChannel() != Token.DEFAULT_CHANNEL) {
            token = lexer.nextToken();
        }
        if (token.getType() == Token.EOF) {
            throw new RuntimeException("No token produced for rule " + testRuleName);
        }
        if (token.getType() != expectedType) {
            throw new RuntimeException("Expected token type " + testRuleName + " but got type " + token.getType()
                    + " text='" + token.getText() + "'");
        }
        // consume remaining hidden then require EOF (no extra default-channel text)
        Token next = lexer.nextToken();
        while (next.getType() != Token.EOF && next.getChannel() != Token.DEFAULT_CHANNEL) {
            next = lexer.nextToken();
        }
        if (next.getType() != Token.EOF) {
            throw new RuntimeException("extra text found, '" + next.getText() + "'");
        }
        if (errors.hasErrors()) {
            throw new RuntimeException(errors.getErrorMessages());
        }
        return token.getText();
    }

    public Object execParser(String testRuleName, String testInput) throws Exception {
        CharStream input = CharStreams.fromString(testInput);
        Lexer lexer = createLexer(input);
        lexer.removeErrorListeners();
        CollectingErrorListener lexerErrors = new CollectingErrorListener();
        lexer.addErrorListener(lexerErrors);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = createParser(tokens);
        parser.removeErrorListeners();
        CollectingErrorListener parserErrors = new CollectingErrorListener();
        parser.addErrorListener(parserErrors);

        Method rule = parser.getClass().getMethod(testRuleName);
        Object ruleReturn = rule.invoke(parser);
        if (!(ruleReturn instanceof ParserRuleContext)) {
            throw new RuntimeException("Rule did not return a ParseTree: " + testRuleName);
        }
        ParseTree parseTree = (ParseTree) ruleReturn;

        if (tokens.LA(1) != Token.EOF) {
            throw new RuntimeException("Invalid input.");
        }
        if (lexerErrors.hasErrors()) {
            throw new RuntimeException(lexerErrors.getErrorMessages());
        }
        if (parserErrors.hasErrors()) {
            throw new RuntimeException(parserErrors.getErrorMessages());
        }

        CmisTree tree;
        switch (kind) {
        case TEXT_SEARCH:
            tree = new TextSearchAstBuilder().build(parseTree);
            break;
        case EXT:
            tree = new CmisQlExtAstBuilder().build(parseTree);
            break;
        case STRICT:
        default:
            tree = new CmisQlAstBuilder().build(parseTree);
            break;
        }
        return tree.toStringTree();
    }

    private Lexer createLexer(CharStream input) throws Exception {
        switch (kind) {
        case TEXT_SEARCH:
            return new TextSearchLexer(input);
        case EXT:
            return new CmisQlExtLexer(input);
        case STRICT:
        default:
            return new CmisQlStrictLexer(input);
        }
    }

    private Parser createParser(CommonTokenStream tokens) throws Exception {
        switch (kind) {
        case TEXT_SEARCH:
            return new TextSearchParser(tokens);
        case EXT:
            return new CmisQlExtParser(tokens);
        case STRICT:
        default:
            return new CmisQlStrictParser(tokens);
        }
    }
}
