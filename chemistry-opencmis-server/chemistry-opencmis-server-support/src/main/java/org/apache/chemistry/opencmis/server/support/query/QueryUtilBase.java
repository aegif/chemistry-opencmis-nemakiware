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

import org.antlr.v4.runtime.RecognitionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.server.support.TypeManager;

/**
 * Utility class to help parsing and processing a query statement using the
 * AntLR parser. Subclasses have to implement methods that setup parser and query
 * walker and parse and process a query. This class provides common methods for
 * error handling and for storing the necessary OpenCMIS objects for query
 * support.
 *
 * @param <T>
 *            walker type, usually {@link CmisQueryWalker}
 */
public abstract class QueryUtilBase<T> {

    protected T walker;
    protected QueryObject queryObj;
    protected PredicateWalkerBase predicateWalker;
    protected String statement;

    protected CmisTree parserTree; // the AST after parsing phase

    /**
     * Perform the first phase of query processing. Setup lexer and parser,
     * parse the statement, check for syntax errors and create an AST
     *
     * @return the abstract syntax tree of the parsed statement
     */
    public abstract CmisTree parseStatement() throws RecognitionException;

    /**
     * Perform the second phase of query processing, analyzes the select part,
     * check for semantic errors, fill the query object.
     */
    public abstract void walkStatement() throws RecognitionException;

    /**
     * Fully process a query by parsing and walking it and setting up the
     * supporting objects
     */
    public void processStatement() throws RecognitionException {
        parseStatement();
        walkStatement();
    }

    protected QueryUtilBase(String statement, TypeManager tm, PredicateWalkerBase pw, QueryObject.ParserMode mode) {
        walker = null;
        queryObj = new QueryObject(tm);
        if (mode != null) {
            queryObj.setSelectMode(mode);
        }
        predicateWalker = pw;
        this.statement = statement;
    }

    public T getWalker() {
        return walker;
    }

    public PredicateWalkerBase getPredicateWalker() {
        return predicateWalker;
    }

    public QueryObject getQueryObject() {
        return queryObj;
    }

    public String getStatement() {
        return statement;
    }

    /**
     * Same as traverseStatement but throws only CMIS Exceptions
     */
    public void processStatementUsingCmisExceptions() {
        try {
            processStatement();
        } catch (RecognitionException e) {
            String errorMsg = getErrorMessage(e);
            throw new CmisInvalidArgumentException(
                    "Processing of query statement failed with RecognitionException error: \n   " + errorMsg, e);
        } catch (CmisBaseException e) {
            throw e;
        } catch (Exception e) {
            throw new CmisInvalidArgumentException("Processing of query statement failed with exception: "
                    + e.getMessage(), e);
        }
    }

    public String getErrorMessage(RecognitionException e) {
        if (e instanceof FailedPredicateException) {
            String text = ((FailedPredicateException) e).predicateText;
            if (text != null) {
                return text;
            }
        }
        if (e.getMessage() != null) {
            return e.getMessage();
        }
        return e.toString();
    }

}
