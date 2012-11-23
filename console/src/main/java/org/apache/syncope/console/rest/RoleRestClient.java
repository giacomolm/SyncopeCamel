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
package org.apache.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.SyncopeSession;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Role's services.
 */
@Component
public class RoleRestClient extends AbstractBaseRestClient {

    /**
     * Get all Roles.
     *
     * @return SchemaTOs
     */
    public List<RoleTO> getAllRoles()
            throws SyncopeClientCompositeErrorException {

        List<RoleTO> roles = null;

        try {
            roles = Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "role/list.json", RoleTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While listing all roles", e);
        }

        return roles;
    }

    public void create(final RoleTO roleTO) {
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "role/create", roleTO, RoleTO.class);
    }

    public RoleTO read(final Long id) {
        RoleTO roleTO = null;

        try {
            roleTO = SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "role/read/{roleId}.json", RoleTO.class, id);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a role", e);
        }
        return roleTO;
    }

    public void update(final RoleMod roleMod) {
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "role/update", roleMod, RoleTO.class);
    }

    public RoleTO delete(final Long id) {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "role/delete/{roleId}.json", RoleTO.class, id);
    }
}