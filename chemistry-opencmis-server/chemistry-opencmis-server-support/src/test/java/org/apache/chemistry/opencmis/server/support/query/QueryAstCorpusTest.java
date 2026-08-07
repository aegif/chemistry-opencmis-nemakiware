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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Golden AST corpus for ANTLR4 compatibility. Expectations were frozen from
 * ANTLR3-era {@code TestParserStrict} asserts, TextSearch rewrite shapes, and
 * differential parity cases (nil flattening + keyword input casing).
 */
public class QueryAstCorpusTest extends AbstractParserTest {

    static Stream<QueryCompatCorpus.Case> strictCases() throws Exception {
        return QueryCompatCorpus.stream("query-compat/strict-ast.corpus");
    }

    static Stream<QueryCompatCorpus.Case> extCases() throws Exception {
        return QueryCompatCorpus.stream("query-compat/ext-ast.corpus");
    }

    static Stream<QueryCompatCorpus.Case> textSearchCases() throws Exception {
        return QueryCompatCorpus.stream("query-compat/textsearch-ast.corpus");
    }

    @BeforeEach
    public void defaultKind() {
        kind = GrammarKind.STRICT;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strictCases")
    public void strictAstCorpus(QueryCompatCorpus.Case c) {
        kind = GrammarKind.STRICT;
        runCase(c);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("extCases")
    public void extAstCorpus(QueryCompatCorpus.Case c) {
        kind = GrammarKind.EXT;
        runCase(c);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("textSearchCases")
    public void textSearchAstCorpus(QueryCompatCorpus.Case c) {
        kind = GrammarKind.TEXT_SEARCH;
        runCase(c);
    }

    private void runCase(QueryCompatCorpus.Case c) {
        if (c.expectation == QueryCompatCorpus.Expectation.OK) {
            testParser(c.rule, c.input, c.expectedAst);
        } else {
            try {
                execParser(c.rule, c.input);
                fail("expected parse failure for rule=" + c.rule + " input=" + c.input);
            } catch (Exception expected) {
                // must fail; exact message is intentionally not asserted
            }
        }
    }
}
