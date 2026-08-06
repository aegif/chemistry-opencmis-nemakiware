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

lexer grammar TextSearchLexer;

tokens {
    TEXT_AND,
    TEXT_OR
}

AND : [Aa][Nn][Dd];
OR : [Oo][Rr];
TEXT_MINUS : '-';

fragment QUOTE: '\'';
fragment DOUBLE_QUOTE: '"';
fragment BACKSL: '\\';

fragment
ESC
    : BACKSL (QUOTE | DOUBLE_QUOTE | BACKSL | TEXT_MINUS | '*' | '?')
    ;

WS : [ \t\r\n]+ -> channel(HIDDEN);

fragment
TEXT_SEARCH_PHRASE_STRING
    : ( ESC | ~[\\"\-'] )+
    ;

TEXT_SEARCH_PHRASE_STRING_LIT
    : DOUBLE_QUOTE TEXT_SEARCH_PHRASE_STRING DOUBLE_QUOTE
    ;

TEXT_SEARCH_WORD_LIT
    : ( ESC | ~[\\' \t\r\n\-] )+
    ;
