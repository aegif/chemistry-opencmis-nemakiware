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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Collects ANTLR4 syntax errors instead of writing them to stderr.
 */
public class CollectingErrorListener extends BaseErrorListener {

    private final List<String> errorMessages = new ArrayList<String>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        errorMessages.add("line " + line + ":" + charPositionInLine + " " + msg);
    }

    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }

    public String getErrorMessages() {
        StringBuilder allMessages = new StringBuilder();
        for (String msg : errorMessages) {
            allMessages.append(msg).append('\n');
        }
        return allMessages.toString();
    }

    public List<String> getErrors() {
        return errorMessages;
    }
}
