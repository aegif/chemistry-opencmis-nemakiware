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
package org.apache.chemistry.opencmis.inmemory.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.inmemory.TypeManagerImpl;
import org.apache.chemistry.opencmis.server.support.query.CmisQueryWalker;
import org.apache.chemistry.opencmis.server.support.query.CmisSelector;
import org.apache.chemistry.opencmis.server.support.query.CmisTree;
import org.apache.chemistry.opencmis.server.support.query.ColumnReference;
import org.apache.chemistry.opencmis.server.support.query.FunctionReference;
import org.apache.chemistry.opencmis.server.support.query.QueryObject;
import org.apache.chemistry.opencmis.server.support.query.QueryObject.SortSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Semantic snapshots: QueryObject + where-tree shape after ANTLR4 walk.
 * Complements golden AST corpora by catching walker/type-resolution regressions.
 */
public class QuerySemanticSnapshotTest extends AbstractQueryTest {

    @BeforeEach
    public void setUp() {
        TypeManagerImpl tm = new TypeManagerImpl();
        tm.initTypeSystem(null, true);
        for (org.apache.chemistry.opencmis.commons.definitions.TypeDefinition td : createTypes()) {
            tm.addTypeDefinition(td, true);
        }
        super.setUp(tm, null);
    }

    @Test
    public void selectWhereOrderSnapshot() throws Exception {
        String statement = "SELECT " + STRING_PROP + " AS s, SCORE() FROM " + MY_DOC_TYPE + " WHERE " + INT_PROP
                + " = 1 ORDER BY " + STRING_PROP + " DESC";

        CmisQueryWalker walker = getWalker(statement);
        QueryObject qo = queryObj;

        assertEquals(MY_DOC_TYPE, qo.getMainTypeAlias());
        assertEquals(1, qo.getTypes().size());
        assertEquals(MY_DOC_TYPE, qo.getTypes().get(MY_DOC_TYPE));

        List<CmisSelector> selects = qo.getSelectReferences();
        assertEquals(2, selects.size());
        assertTrue(selects.get(0) instanceof ColumnReference);
        ColumnReference col = (ColumnReference) selects.get(0);
        assertEquals(STRING_PROP, col.getPropertyQueryName());
        assertEquals("s", col.getAliasName());
        assertTrue(selects.get(1) instanceof FunctionReference);

        CmisTree where = walker.getWherePredicateTree();
        assertNotNull(where);
        assertEquals("(= (COL " + INT_PROP + ") 1)", where.toStringTree());

        List<SortSpec> sorts = qo.getOrderBys();
        assertEquals(1, sorts.size());
        assertFalse(sorts.get(0).isAscending());
        assertTrue(sorts.get(0).getSelector() instanceof ColumnReference);
        assertEquals(STRING_PROP, ((ColumnReference) sorts.get(0).getSelector()).getPropertyQueryName());
    }

    @Test
    public void joinAndContainsSnapshot() throws Exception {
        String statement = "SELECT d." + STRING_PROP + " FROM " + MY_DOC_TYPE + " AS d JOIN " + MY_DOC_TYPE_COPY
                + " AS c ON d." + STRING_PROP + " = c." + STRING_PROP + " WHERE CONTAINS(d, 'alpha beta')";

        CmisQueryWalker walker = getWalker(statement);
        QueryObject qo = queryObj;

        Map<String, String> types = qo.getTypes();
        assertEquals(2, types.size());
        assertEquals(MY_DOC_TYPE, types.get("d"));
        assertEquals(MY_DOC_TYPE_COPY, types.get("c"));
        assertEquals(1, walker.getNumberOfContainsClauses());

        CmisTree where = walker.getWherePredicateTree();
        assertNotNull(where);
        // CONTAINS child is grafted TextSearch AST when full-text parse is on
        assertTrue(where.toStringTree().startsWith("(CONTAINS d "), where.toStringTree());
        assertTrue(where.toStringTree().contains("TEXT_AND") || where.toStringTree().contains("alpha"),
                where.toStringTree());
    }

    @Test
    public void starSelectSnapshot() throws Exception {
        CmisQueryWalker walker = getWalker("SELECT * FROM " + BOOK_TYPE);
        QueryObject qo = queryObj;
        assertEquals(BOOK_TYPE, qo.getMainTypeAlias());
        List<String> names = qo.getSelectReferences().stream().map(CmisSelector::getName).collect(Collectors.toList());
        assertEquals(1, names.size());
        assertEquals("*", names.get(0));
        assertEquals(null, walker.getWherePredicateTree());
        assertTrue(qo.getOrderBys().isEmpty());
    }

    @Test
    public void unknownTypeMustFail() {
        try {
            getWalker("SELECT * FROM NoSuchType");
            fail("expected invalid argument for unknown type");
        } catch (CmisInvalidArgumentException expected) {
            assertNotNull(expected.getMessage());
        } catch (Exception e) {
            fail("unexpected exception type: " + e);
        }
    }
}
