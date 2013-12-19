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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.List;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.common.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SchemaTestITCase extends AbstractTest {

    private SchemaTO buildSchemaTO(final String name, final AttributeSchemaType type) {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName(name + getUUIDString());
        schemaTO.setType(type);
        return schemaTO;
    }

    @Test
    public void create() {
        SchemaTO schemaTO = buildSchemaTO("testAttribute", AttributeSchemaType.String);
        schemaTO.setMandatoryCondition("false");

        SchemaTO newSchemaTO = createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
        assertEquals(schemaTO, newSchemaTO);

        newSchemaTO = createSchema(AttributableType.MEMBERSHIP, SchemaType.NORMAL, schemaTO);
        assertEquals(schemaTO, newSchemaTO);
    }

    @Test
    public void createWithNotPermittedName() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("failedLogins");
        schemaTO.setType(AttributeSchemaType.String);

        try {
            createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().
                    contains(EntityViolationType.InvalidName.name()));
        }
    }

    @Test
    public void createREnumWithoutEnumeration() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("enumcheck");
        schemaTO.setType(AttributeSchemaType.Enum);

        try {
            createSchema(AttributableType.ROLE, SchemaType.NORMAL, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().
                    contains(EntityViolationType.InvalidSchemaEnum.name()));
        }
    }

    @Test
    public void createUEnumWithoutEnumeration() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("enumcheck");
        schemaTO.setType(AttributeSchemaType.Enum);

        try {
            createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().
                    contains(EntityViolationType.InvalidSchemaEnum.name()));
        }
    }

    @Test
    public void delete() {
        SchemaTO schemaTO = buildSchemaTO("todelete", AttributeSchemaType.String);
        schemaTO.setMandatoryCondition("false");
        createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);

        schemaService.delete(AttributableType.USER, SchemaType.NORMAL, schemaTO.getName());
        SchemaTO firstname = null;
        try {
            firstname = schemaService.read(AttributableType.USER, SchemaType.NORMAL, schemaTO.getName());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        assertNull(firstname);
    }

    @Test
    public void list() {
        List<SchemaTO> userSchemas = schemaService.list(AttributableType.USER, SchemaType.NORMAL);
        assertFalse(userSchemas.isEmpty());
        for (SchemaTO schemaTO : userSchemas) {
            assertNotNull(schemaTO);
        }

        List<SchemaTO> roleSchemas = schemaService.list(AttributableType.ROLE, SchemaType.NORMAL);
        assertFalse(roleSchemas.isEmpty());
        for (SchemaTO schemaTO : roleSchemas) {
            assertNotNull(schemaTO);
        }

        List<SchemaTO> membershipSchemas = schemaService.list(AttributableType.MEMBERSHIP, SchemaType.NORMAL);
        assertFalse(membershipSchemas.isEmpty());
        for (SchemaTO schemaTO : membershipSchemas) {
            assertNotNull(schemaTO);
        }
    }

    @Test
    public void update() {
        SchemaTO schemaTO = schemaService.read(AttributableType.ROLE, SchemaType.NORMAL, "icon");
        assertNotNull(schemaTO);

        schemaService.update(AttributableType.ROLE, SchemaType.NORMAL, schemaTO.getName(), schemaTO);
        SchemaTO updatedTO = schemaService.read(AttributableType.ROLE, SchemaType.NORMAL, "icon");
        assertEquals(schemaTO, updatedTO);

        updatedTO.setType(AttributeSchemaType.Date);
        try {
            schemaService.update(AttributableType.ROLE, SchemaType.NORMAL, schemaTO.getName(), updatedTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRSchema, e.getType());
        }
    }

    @Test
    public void issue258() {
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("schema_issue258");
        schemaTO.setType(AttributeSchemaType.Double);

        schemaTO = createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserTestITCase.getUniqueSampleTO("issue258@syncope.apache.org");
        userTO.getAttrs().add(attributeTO(schemaTO.getName(), "1.2"));

        userTO = createUser(userTO);
        assertNotNull(userTO);

        schemaTO.setType(AttributeSchemaType.Long);
        try {
            schemaService.update(AttributableType.USER, SchemaType.NORMAL, schemaTO.getName(), schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUSchema, e.getType());
        }
    }

    @Test
    public void issue259() {
        SchemaTO schemaTO = buildSchemaTO("schema_issue259", AttributeSchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserTestITCase.getUniqueSampleTO("issue259@syncope.apache.org");
        userTO.getAttrs().add(attributeTO(schemaTO.getName(), "1"));
        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserTO newUserTO = AttributableOperations.clone(userTO);
        MembershipTO membership = new MembershipTO();
        membership.setRoleId(2L);
        newUserTO.getMemberships().add(membership);

        UserMod userMod = AttributableOperations.diff(newUserTO, userTO);

        userTO = userService.update(userMod.getId(), userMod).readEntity(UserTO.class);
        assertNotNull(userTO);
    }

    @Test
    public void issue260() {
        SchemaTO schemaTO = buildSchemaTO("schema_issue260", AttributeSchemaType.Double);
        schemaTO.setUniqueConstraint(true);

        schemaTO = createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
        assertNotNull(schemaTO);

        UserTO userTO = UserTestITCase.getUniqueSampleTO("issue260@syncope.apache.org");
        userTO.getAttrs().add(attributeTO(schemaTO.getName(), "1.2"));
        userTO = createUser(userTO);
        assertNotNull(userTO);

        schemaTO.setUniqueConstraint(false);
        try {
            schemaService.update(AttributableType.USER, SchemaType.NORMAL, schemaTO.getName(), schemaTO);
            fail("This should not be reacheable");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUSchema, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE323() {
        SchemaTO actual = schemaService.read(AttributableType.ROLE, SchemaType.NORMAL, "icon");
        assertNotNull(actual);

        try {
            createSchema(AttributableType.ROLE, SchemaType.NORMAL, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setName(null);
        try {
            createSchema(AttributableType.ROLE, SchemaType.NORMAL, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE418() {
        SchemaTO schema = buildSchemaTO("http://schemas.examples.org/security/authorization/organizationUnit",
                AttributeSchemaType.Double);

        try {
            createSchema(AttributableType.ROLE, SchemaType.NORMAL, schema);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRSchema, e.getType());

            assertTrue(e.getElements().iterator().next().toString().
                    contains(EntityViolationType.InvalidName.name()));
        }
    }

    @Test
    public void anonymous() {
        SchemaService unauthenticated = clientFactory.createAnonymous().getService(SchemaService.class);
        try {
            unauthenticated.list(AttributableType.USER, SchemaType.VIRTUAL);
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        SchemaService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(SchemaService.class);
        assertFalse(anonymous.list(AttributableType.USER, SchemaType.VIRTUAL).isEmpty());
    }
}
