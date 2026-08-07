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

lexer grammar CmisQlExtLexer;

tokens {
    TABLE,
    COL,
    SEL_LIST,
    IN_LIST,
    IN_ANY,
    NOT_IN_ANY,
    EQ_ANY,
    NOT_IN,
    NOT_LIKE,
    IS_NULL,
    IS_NOT_NULL,
    ORDER_BY,
    FUNC,
    OP_ANY
}

SELECT : [Ss][Ee][Ll][Ee][Cc][Tt];
DISTINCT : [Dd][Ii][Ss][Tt][Ii][Nn][Cc][Tt];
FROM : [Ff][Rr][Oo][Mm];
AS : [Aa][Ss];
JOIN : [Jj][Oo][Ii][Nn];
INNER : [Ii][Nn][Nn][Ee][Rr];
OUTER : [Oo][Uu][Tt][Ee][Rr];
LEFT : [Ll][Ee][Ff][Tt];
RIGHT : [Rr][Ii][Gg][Hh][Tt];
ON : [Oo][Nn];
WHERE : [Ww][Hh][Ee][Rr][Ee];
ORDER : [Oo][Rr][Dd][Ee][Rr];
BY : [Bb][Yy];
ASC : [Aa][Ss][Cc];
DESC : [Dd][Ee][Ss][Cc];

IS : [Ii][Ss];
NULL : [Nn][Uu][Ll][Ll];
AND : [Aa][Nn][Dd];
OR : [Oo][Rr];
NOT : [Nn][Oo][Tt];
IN : [Ii][Nn];
LIKE : [Ll][Ii][Kk][Ee];
ANY : [Aa][Nn][Yy];
CONTAINS : [Cc][Oo][Nn][Tt][Aa][Ii][Nn][Ss];
SCORE : [Ss][Cc][Oo][Rr][Ee];
IN_FOLDER : [Ii][Nn]'_'[Ff][Oo][Ll][Dd][Ee][Rr];
IN_TREE : [Ii][Nn]'_'[Tt][Rr][Ee][Ee];
TIMESTAMP : 'TIMESTAMP' | 'timestamp';

STAR : '*';
LPAR : '(';
RPAR : ')';
COMMA : ',';
DOT : '.';
EQ : '=';
NEQ : '<>';
LT : '<';
GT : '>';
LTEQ : '<=';
GTEQ : '>=';

BOOL_LIT : 'TRUE' | 'true' | 'FALSE' | 'false';

fragment Sign : [+-]?;
fragment Digits : [0-9]+;
fragment ExactNumLit : Digits DOT Digits | Digits DOT | DOT Digits | Digits;
fragment ApproxNumLit : ExactNumLit [eE] Sign Digits;
NUM_LIT : Sign (ExactNumLit | ApproxNumLit);

fragment QUOTE: '\'';
fragment BACKSL: '\\';
fragment UNDERSCORE: '_';
fragment PERCENT: '%';

fragment
ESC
    : BACKSL (QUOTE | BACKSL | PERCENT | UNDERSCORE)
    | QUOTE QUOTE
    ;

STRING_LIT
    : QUOTE ( ESC | ~[\\'] )* QUOTE
    ;

fragment WS_CHARS : [ \t\r\n]+;

TIME_LIT : TIMESTAMP WS_CHARS? STRING_LIT;

WS : WS_CHARS -> channel(HIDDEN);

ID :
    [a-zA-Z_]
    [a-zA-Z_0-9:]*
    ;
