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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.syncope.core.persistence.beans.membership.MAttrValue;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttrValue;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttrValue;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DerAttrTest extends AbstractDAOTest {

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Test
    public void findAll() {
        List<UDerAttr> list = derAttrDAO.findAll(UDerAttr.class);
        assertEquals("did not get expected number of derived attributes ", 2, list.size());
    }

    @Test
    public void findById() {
        UDerAttr attribute = derAttrDAO.find(100L, UDerAttr.class);
        assertNotNull("did not find expected attribute schema", attribute);
    }

    @Test
    public void saveUDerAttribute() {
        UDerSchema cnSchema = derSchemaDAO.find("cn", UDerSchema.class);
        assertNotNull(cnSchema);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derivedAttribute = new UDerAttr();
        derivedAttribute.setOwner(owner);
        derivedAttribute.setSchema(cnSchema);

        derivedAttribute = derAttrDAO.save(derivedAttribute);

        UDerAttr actual = derAttrDAO.find(derivedAttribute.getId(), UDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derivedAttribute, actual);

        UAttrValue firstnameAttribute = (UAttrValue) owner.getAttr("firstname").getValues().iterator().next();
        UAttrValue surnameAttribute = (UAttrValue) owner.getAttr("surname").getValues().iterator().next();

        assertEquals(surnameAttribute.getValue() + ", " + firstnameAttribute.getValue(), derivedAttribute
                .getValue(owner.getAttrs()));
    }

    @Test
    public void saveMDerAttribute() {
        Membership owner = membershipDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        MDerAttr derAttr = new MDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getSyncopeRole().getAttrTemplate(MDerAttrTemplate.class, "mderiveddata"));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());

        MDerAttr actual = derAttrDAO.find(derAttr.getId(), MDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derAttr, actual);

        MAttrValue sx = (MAttrValue) owner.getAttr("mderived_sx").getValues().iterator().next();
        MAttrValue dx = (MAttrValue) owner.getAttr("mderived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(), derAttr.getValue(owner.getAttrs()));
    }

    @Test
    public void saveRDerAttribute() {
        SyncopeRole owner = roleDAO.find(1L);
        assertNotNull("did not get expected user", owner);

        RDerAttr derAttr = new RDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getAttrTemplate(RDerAttrTemplate.class, "rderiveddata"));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());

        RDerAttr actual = derAttrDAO.find(derAttr.getId(), RDerAttr.class);
        assertNotNull("expected save to work", actual);
        assertEquals(derAttr, actual);

        RAttrValue sx = (RAttrValue) owner.getAttr("rderived_sx").getValues().iterator().next();
        RAttrValue dx = (RAttrValue) owner.getAttr("rderived_dx").getValues().iterator().next();

        assertEquals(sx.getValue() + "-" + dx.getValue(), derAttr.getValue(owner.getAttrs()));
    }

    @Test
    public void delete() {
        UDerAttr attribute = derAttrDAO.find(100L, UDerAttr.class);
        String attributeSchemaName = attribute.getSchema().getName();

        derAttrDAO.delete(attribute.getId(), UDerAttr.class);

        UDerAttr actual = derAttrDAO.find(100L, UDerAttr.class);
        assertNull("delete did not work", actual);

        UDerSchema attributeSchema = derSchemaDAO.find(attributeSchemaName, UDerSchema.class);
        assertNotNull("user derived attribute schema deleted " + "when deleting values", attributeSchema);
    }

    @Test
    public void issueSYNCOPE134User() {
        UDerSchema sderived = new UDerSchema();
        sderived.setName("sderived");
        sderived.setExpression("status + ' - ' + username + ' - ' + creationDate + '[' + failedLogins + ']'");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        UDerSchema actual = derSchemaDAO.find("sderived", UDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        SyncopeUser owner = userDAO.find(3L);
        assertNotNull("did not get expected user", owner);

        UDerAttr derAttr = new UDerAttr();
        derAttr.setOwner(owner);
        derAttr.setSchema(sderived);

        derAttr = derAttrDAO.save(derAttr);
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getId(), UDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("active - vivaldi - 2010-10-20"));
        assertTrue(value.endsWith("[0]"));
    }

    @Test
    public void issueSYNCOPE134Role() {
        RDerSchema sderived = new RDerSchema();
        sderived.setName("sderived");
        sderived.setExpression("name");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        RDerSchema actual = derSchemaDAO.find("sderived", RDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        SyncopeRole owner = roleDAO.find(7L);
        assertNotNull("did not get expected role", owner);

        RDerAttrTemplate template = new RDerAttrTemplate();
        template.setSchema(sderived);
        owner.getAttrTemplates(RDerAttrTemplate.class).add(template);

        RDerAttr derAttr = new RDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getAttrTemplate(RDerAttrTemplate.class, sderived.getName()));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getId(), RDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("managingDirector"));
    }

    @Test
    public void issueSYNCOPE134Memb() {
        MDerSchema sderived = new MDerSchema();
        sderived.setName("sderived");
        sderived.setExpression("id");

        sderived = derSchemaDAO.save(sderived);
        derSchemaDAO.flush();

        MDerSchema actual = derSchemaDAO.find("sderived", MDerSchema.class);
        assertNotNull("expected save to work", actual);
        assertEquals(sderived, actual);

        Membership owner = membershipDAO.find(4L);
        assertNotNull("did not get expected membership", owner);

        MDerAttrTemplate template = new MDerAttrTemplate();
        template.setSchema(sderived);
        owner.getSyncopeRole().getAttrTemplates(MDerAttrTemplate.class).add(template);

        MDerAttr derAttr = new MDerAttr();
        derAttr.setOwner(owner);
        derAttr.setTemplate(owner.getSyncopeRole().getAttrTemplate(MDerAttrTemplate.class, sderived.getName()));

        derAttr = derAttrDAO.save(derAttr);
        assertNotNull(derAttr.getTemplate());
        derAttrDAO.flush();

        derAttr = derAttrDAO.find(derAttr.getId(), MDerAttr.class);
        assertNotNull("expected save to work", derAttr);

        String value = derAttr.getValue(owner.getAttrs());
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.equalsIgnoreCase("4"));
    }
}
