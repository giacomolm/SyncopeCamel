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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.rest.controller.PolicyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolicyServiceImpl implements PolicyService, ContextAware {

    @Autowired
    private PolicyController policyController;

    private UriInfo uriInfo;

    @Override
    public <T extends PolicyTO> Response create(final PolicyType type, final T policyTO) {
        PolicyTO policy;
        switch (type) {
            case ACCOUNT:
                policy = policyController.create(new DummyHTTPServletResponse(), (AccountPolicyTO) policyTO);
                break;

            case PASSWORD:
                policy = policyController.create(new DummyHTTPServletResponse(), (PasswordPolicyTO) policyTO);
                break;

            case SYNC:
                policy = policyController.create(new DummyHTTPServletResponse(), (SyncPolicyTO) policyTO);
                break;

            default:
                throw new BadRequestException();
        }
        URI location = uriInfo.getAbsolutePathBuilder().path(policy.getId() + "").build();
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, policy.getId()).build();
    }

    @Override
    public void delete(final PolicyType type, final Long policyId) {
        try {
            policyController.delete(policyId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> List<T> list(final PolicyType type) {
        return (List<T>) policyController.listByType(type.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> T read(final PolicyType type, final Long policyId) {
        try {
            return (T) policyController.read(policyId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> T readGlobal(final PolicyType type) {
        try {
            switch (type) {
                case ACCOUNT:
                case GLOBAL_ACCOUNT:
                    return (T) policyController.getGlobalAccountPolicy();

                case PASSWORD:
                case GLOBAL_PASSWORD:
                    return (T) policyController.getGlobalPasswordPolicy();

                case SYNC:
                case GLOBAL_SYNC:
                    return (T) policyController.getGlobalSyncPolicy();

                default:
                    throw new BadRequestException();
            }
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public <T extends PolicyTO> void update(final PolicyType type, final Long policyId, final T policyTO) {
        try {
            switch (type) {
                case ACCOUNT:
                case GLOBAL_ACCOUNT:
                    policyController.update((AccountPolicyTO) policyTO);
                    break;

                case PASSWORD:
                case GLOBAL_PASSWORD:
                    policyController.update((PasswordPolicyTO) policyTO);
                    break;

                case SYNC:
                case GLOBAL_SYNC:
                    policyController.update((SyncPolicyTO) policyTO);
                    break;

                default:
                    break;
            }
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }

}
