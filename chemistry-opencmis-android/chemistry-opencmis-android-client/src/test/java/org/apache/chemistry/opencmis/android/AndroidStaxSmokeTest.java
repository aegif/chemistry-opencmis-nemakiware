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
package org.apache.chemistry.opencmis.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.chemistry.opencmis.commons.impl.XMLUtils;
import org.junit.jupiter.api.Test;

/**
 * JVM-side smoke for the Android client's shared StAX/Woodstox stack.
 * Does not replace device/emulator DEX or instrumentation testing.
 */
public class AndroidStaxSmokeTest {

    @Test
    public void woodstoxParsesMinimalAtomEntry() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<entry xmlns=\"http://www.w3.org/2005/Atom\">"
                + "<title>t</title></entry>";

        XMLStreamReader reader = XMLUtils.createParser(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(reader);

        boolean sawTitle = false;
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT
                    && "title".equals(reader.getLocalName())) {
                assertEquals("t", reader.getElementText());
                sawTitle = true;
                break;
            }
        }
        reader.close();

        assertEquals(true, sawTitle);
        assertNotNull(XMLInputFactory.newFactory());
    }
}
