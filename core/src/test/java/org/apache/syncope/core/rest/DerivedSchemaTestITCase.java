/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.to.DerSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class DerivedSchemaTestITCase extends AbstractTest {

    @Test
    public void list() {
        List<DerSchemaTO> derivedSchemas = schemaService.list(AttributableType.USER, SchemaType.DERIVED);
        assertFalse(derivedSchemas.isEmpty());
        for (DerSchemaTO derivedSchemaTO : derivedSchemas) {
            assertNotNull(derivedSchemaTO);
        }
    }

    @Test
    public void read() {
        DerSchemaTO derivedSchemaTO = schemaService.read(AttributableType.USER, SchemaType.DERIVED,
                "cn");
        assertNotNull(derivedSchemaTO);
    }

    @Test
    public void create() {
        DerSchemaTO schema = new DerSchemaTO();
        schema.setName("derived");
        schema.setExpression("derived_sx + '_' + derived_dx");

        DerSchemaTO actual = createSchema(AttributableType.USER, SchemaType.DERIVED, schema);
        assertNotNull(actual);

        actual = schemaService.read(AttributableType.USER, SchemaType.DERIVED, actual.getName());
        assertNotNull(actual);
        assertEquals(actual.getExpression(), "derived_sx + '_' + derived_dx");
    }

    @Test
    public void delete() {
        DerSchemaTO schema = schemaService.read(AttributableType.ROLE, SchemaType.DERIVED, "rderiveddata");
        assertNotNull(schema);

        schemaService.delete(AttributableType.ROLE, SchemaType.DERIVED, schema.getName());

        try {
            schemaService.read(AttributableType.ROLE, SchemaType.DERIVED, "rderiveddata");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        } finally {
            // Recreate schema to make test re-runnable
            schema = createSchema(AttributableType.ROLE, SchemaType.DERIVED, schema);
            assertNotNull(schema);
        }
    }

    @Test
    public void update() {
        DerSchemaTO schema = schemaService.read(AttributableType.MEMBERSHIP, SchemaType.DERIVED,
                "mderiveddata");
        assertNotNull(schema);
        assertEquals("mderived_sx + '-' + mderived_dx", schema.getExpression());
        try {
            schema.setExpression("mderived_sx + '.' + mderived_dx");

            schemaService.update(AttributableType.MEMBERSHIP, SchemaType.DERIVED,
                    schema.getName(), schema);

            schema = schemaService.read(AttributableType.MEMBERSHIP, SchemaType.DERIVED, "mderiveddata");
            assertNotNull(schema);
            assertEquals("mderived_sx + '.' + mderived_dx", schema.getExpression());
        } finally {
            // Set updated back to make test re-runnable
            schema.setExpression("mderived_sx + '-' + mderived_dx");
            schemaService.update(AttributableType.MEMBERSHIP, SchemaType.DERIVED,
                    schema.getName(), schema);
        }
    }

    @Test
    public void issueSYNCOPE323() {
        DerSchemaTO actual = schemaService.read(AttributableType.ROLE, SchemaType.DERIVED, "rderiveddata");
        assertNotNull(actual);

        try {
            createSchema(AttributableType.ROLE, SchemaType.DERIVED, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setName(null);
        try {
            createSchema(AttributableType.ROLE, SchemaType.DERIVED, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE418() {
        DerSchemaTO schema = new DerSchemaTO();
        schema.setName("http://schemas.examples.org/security/authorization/organizationUnit");
        schema.setExpression("derived_sx + '_' + derived_dx");

        try {
            createSchema(AttributableType.ROLE, SchemaType.DERIVED, schema);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRDerSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().contains(EntityViolationType.InvalidName.name()));
        }
    }
}
