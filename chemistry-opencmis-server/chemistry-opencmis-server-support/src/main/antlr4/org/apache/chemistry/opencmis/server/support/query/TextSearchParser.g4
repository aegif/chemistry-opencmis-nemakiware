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

parser grammar TextSearchParser;

options {
    tokenVocab = TextSearchLexer;
}

text_search_expression
    : conjunct ( OR conjunct )+
    | conjunct
    ;

conjunct
    : term ( AND? term )+
    | term
    ;

term
    : TEXT_MINUS? ( word | phrase )
    ;

phrase
    : TEXT_SEARCH_PHRASE_STRING_LIT
    ;

word
    : TEXT_SEARCH_WORD_LIT
    ;
