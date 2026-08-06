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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loader for tab-separated AST golden corpora under {@code query-compat/}.
 * <p>
 * Line formats:
 * <ul>
 * <li>{@code ok\trule\tinput\texpected_ast}</li>
 * <li>{@code fail\trule\tinput}</li>
 * </ul>
 */
public final class QueryCompatCorpus {

    public enum Expectation {
        OK, FAIL
    }

    public static final class Case {
        public final Expectation expectation;
        public final String rule;
        public final String input;
        public final String expectedAst;
        public final String displayName;

        Case(Expectation expectation, String rule, String input, String expectedAst, String displayName) {
            this.expectation = expectation;
            this.rule = rule;
            this.input = input;
            this.expectedAst = expectedAst;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private QueryCompatCorpus() {
    }

    public static List<Case> load(String resourcePath) throws IOException {
        InputStream in = QueryCompatCorpus.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Corpus resource not found: " + resourcePath);
        }
        List<Case> cases = new ArrayList<Case>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length < 3) {
                    throw new IOException(resourcePath + ":" + lineNo + ": expected at least 3 tab fields");
                }
                Expectation expectation = Expectation.valueOf(parts[0].trim().toUpperCase());
                String rule = parts[1];
                String input = parts[2];
                String expected = null;
                if (expectation == Expectation.OK) {
                    if (parts.length < 4) {
                        throw new IOException(resourcePath + ":" + lineNo + ": ok rows need expected AST");
                    }
                    expected = parts[3];
                }
                String name = expectation.name().toLowerCase() + ":" + rule + ":" + abbreviate(input);
                cases.add(new Case(expectation, rule, input, expected, name));
            }
        }
        return Collections.unmodifiableList(cases);
    }

    public static Stream<Case> stream(String resourcePath) throws IOException {
        return load(resourcePath).stream();
    }

    private static String abbreviate(String input) {
        String compact = input.replace('\n', ' ').trim();
        if (compact.length() <= 48) {
            return compact;
        }
        return compact.substring(0, 45) + "...";
    }
}
