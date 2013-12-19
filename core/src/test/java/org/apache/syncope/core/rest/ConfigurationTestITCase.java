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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ConfigurationTestITCase extends AbstractTest {

    @Test
    public void create() {
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("testKey");
        configurationTO.setValue("testValue");

        Response response = configurationService.create(configurationTO);
        assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        ConfigurationTO newConfigurationTO = getObject(response.getLocation(), ConfigurationService.class,
                ConfigurationTO.class);
        assertEquals(configurationTO, newConfigurationTO);
    }

    @Test
    public void delete() throws UnsupportedEncodingException {
        try {
            configurationService.delete("nonExistent");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        ConfigurationTO tokenLengthTO = configurationService.read("token.length");

        configurationService.delete("token.length");
        try {
            configurationService.read("token.length");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        Response response = configurationService.create(tokenLengthTO);
        assertNotNull(response);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        ConfigurationTO newConfigurationTO = getObject(response.getLocation(), ConfigurationService.class,
                ConfigurationTO.class);
        assertEquals(tokenLengthTO, newConfigurationTO);
    }

    @Test
    public void list() {
        List<ConfigurationTO> configurations = configurationService.list();
        assertNotNull(configurations);
        for (ConfigurationTO configuration : configurations) {
            assertNotNull(configuration);
        }
    }

    @Test
    public void read() {
        ConfigurationTO configurationTO = configurationService.read("token.expireTime");

        assertNotNull(configurationTO);
    }

    @Test
    public void update() {
        ConfigurationTO configurationTO = configurationService.read("token.expireTime");
        int value = Integer.parseInt(configurationTO.getValue());
        value++;
        configurationTO.setValue(value + "");

        configurationService.update(configurationTO.getKey(), configurationTO);
        ConfigurationTO newConfigurationTO = configurationService.read(configurationTO.getKey());
        assertEquals(configurationTO, newConfigurationTO);

        newConfigurationTO = configurationService.read("token.expireTime");
        assertEquals(configurationTO, newConfigurationTO);
    }

    @Test
    public void dbExport() throws IOException {
        Response response = configurationService.export();
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        assertTrue(response.getMediaType().toString().startsWith(MediaType.TEXT_XML));
        String contentDisposition = response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);

        Object entity = response.getEntity();
        assertTrue(entity instanceof InputStream);
        String configExport = IOUtils.toString((InputStream) entity, SyncopeConstants.DEFAULT_ENCODING);
        assertFalse(configExport.isEmpty());
        assertTrue(configExport.length() > 1000);
    }

    @Test
    public void issueSYNCOPE418() {
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("http://schemas.examples.org/security/authorization/organizationUnit");

        try {
            configurationService.create(configurationTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSyncopeConf, e.getType());

            assertNotNull(e.getElements());
            assertEquals(1, e.getElements().size());
            assertTrue(e.getElements().iterator().next().toString().contains(EntityViolationType.InvalidName.name()));
        }
    }
}
