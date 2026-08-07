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

parser grammar CmisQlExtParser;

options {
    tokenVocab = CmisQlExtLexer;
}

root : query EOF;

query
    : SELECT DISTINCT? select_list from_clause where_clause? order_by_clause?
    ;

select_list
    : STAR
    | select_sublist ( COMMA select_sublist )*
    ;

select_sublist
    : value_expression ( AS? column_name )?
    | qualifier DOT STAR
    ;

value_expression
    : column_reference
    | string_value_function
    | numeric_value_function
    ;

string_value_function
    : ID LPAR column_reference RPAR
    ;

column_reference
    : ( qualifier DOT )? column_name
    ;

multi_valued_column_reference
    : ( qualifier DOT )? multi_valued_column_name
    ;

numeric_value_function
    : SCORE LPAR RPAR
    ;

qualifier
    : table_name
    ;

from_clause
    : FROM table_reference
    ;

table_reference
    : one_table table_join*
    ;

table_join
    : join_kind one_table join_specification?
    ;

one_table
    : LPAR table_reference RPAR
    | table_name ( AS? correlation_name )?
    ;

join_kind
    : JOIN
    | INNER JOIN
    | LEFT OUTER? JOIN
    | RIGHT OUTER? JOIN
    ;

join_specification
    : ON column_reference EQ column_reference
    ;

where_clause
    : WHERE search_condition
    ;

search_condition
    : boolean_term ( OR boolean_term )*
    ;

boolean_term
    : boolean_factor ( AND boolean_factor )*
    ;

boolean_factor
    : NOT boolean_test
    | boolean_test
    ;

boolean_test
    : predicate
    | LPAR search_condition RPAR
    ;

predicate
    : comparison_predicate
    | in_predicate
    | like_predicate
    | null_predicate
    | quantified_comparison_predicate
    | quantified_in_predicate
    | text_search_predicate
    | folder_predicate
    ;

comparison_predicate
    : value_expression EQ literal
    | value_expression NEQ literal
    | value_expression LT literal
    | value_expression GT literal
    | value_expression LTEQ literal
    | value_expression GTEQ literal
    ;

literal
    : NUM_LIT
    | STRING_LIT
    | TIME_LIT
    | BOOL_LIT
    ;

in_predicate
    : column_reference IN LPAR in_value_list RPAR
    | column_reference NOT IN LPAR in_value_list RPAR
    ;

in_value_list
    : literal ( COMMA literal )*
    ;

like_predicate
    : column_reference LIKE STRING_LIT
    | column_reference NOT LIKE STRING_LIT
    ;

null_predicate
    : column_reference IS ( NOT NULL | NULL )
    ;

quantified_comparison_predicate
    : literal comp_op ANY multi_valued_column_reference
    ;

comp_op
    : EQ | NEQ | LT | GT | LTEQ | GTEQ
    ;

quantified_in_predicate
    : ANY multi_valued_column_reference ( NOT IN LPAR in_value_list RPAR | IN LPAR in_value_list RPAR )
    ;

text_search_predicate
    : CONTAINS LPAR ( qualifier COMMA )? text_search_expression RPAR
    ;

folder_predicate
    : ( IN_FOLDER | IN_TREE ) LPAR ( qualifier COMMA )? folder_id RPAR
    ;

order_by_clause
    : ORDER BY sort_specification ( COMMA sort_specification )*
    ;

sort_specification
    : column_reference ( ASC | DESC )?
    ;

correlation_name
    : ID
    ;

table_name
    : ID
    ;

column_name
    : ID
    ;

multi_valued_column_name
    : ID
    ;

folder_id
    : STRING_LIT
    ;

text_search_expression
    : STRING_LIT
    ;
