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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.server.support.TypeManager;

public class QueryUtilStrict extends QueryUtilBase<CmisQueryWalker> {

    private boolean parseFulltext = true;

    public QueryUtilStrict(String statement, TypeManager tm, PredicateWalkerBase pw) {
        super(statement, tm, pw, null);
    }

    public QueryUtilStrict(String statement, TypeManager tm, PredicateWalkerBase pw, boolean parseFulltext) {
        super(statement, tm, pw, null);
        this.parseFulltext = parseFulltext;
    }

    public QueryUtilStrict(String statement, TypeManager tm, PredicateWalkerBase pw, boolean parseFulltext,
            QueryObject.ParserMode mode) {
        super(statement, tm, pw, mode);
        this.parseFulltext = parseFulltext;
    }

    @Override
    public CmisTree parseStatement() throws RecognitionException {
        CmisQlStrictLexer lexer = new CmisQlStrictLexer(CharStreams.fromString(statement));
        lexer.removeErrorListeners();
        CollectingErrorListener lexerErrors = new CollectingErrorListener();
        lexer.addErrorListener(lexerErrors);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CmisQlStrictParser parser = new CmisQlStrictParser(tokens);
        parser.removeErrorListeners();
        CollectingErrorListener parserErrors = new CollectingErrorListener();
        parser.addErrorListener(parserErrors);

        CmisQlStrictParser.RootContext root = parser.root();
        if (lexerErrors.hasErrors()) {
            throw new CmisInvalidArgumentException(lexerErrors.getErrorMessages());
        } else if (parserErrors.hasErrors()) {
            throw new CmisInvalidArgumentException(parserErrors.getErrorMessages());
        }

        parserTree = new CmisQlAstBuilder().build(root);
        return parserTree;
    }

    @Override
    public void walkStatement() throws RecognitionException {
        if (null == parserTree) {
            throw new CmisQueryException("You must parse the query before you can walk it.");
        }

        walker = new CmisQueryWalker();
        walker.setDoFullTextParse(parseFulltext);
        walker.query(parserTree, queryObj, predicateWalker);
        walker.getWherePredicateTree();
    }

}
