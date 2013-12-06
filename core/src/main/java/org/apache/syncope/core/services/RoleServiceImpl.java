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

import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.to.ResourceNameTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.RoleController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends AbstractServiceImpl implements RoleService {

    @Autowired
    private RoleController controller;

    @Override
    public List<RoleTO> children(final Long roleId) {
        return controller.children(roleId);
    }

    @Override
    public int count() {
        return controller.count();
    }

    @Override
    public Response create(final RoleTO roleTO) {
        RoleTO created = controller.create(roleTO);
        return createResponse(created.getId(), created).build();
    }

    @Override
    public Response delete(final Long roleId) {
        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
            RoleTO deleted = controller.delete(roleId);
            builder = modificationResponse(deleted);
        }

        return builder.build();
    }

    @Override
    public List<RoleTO> list() {
        return list(1, 25);
    }

    @Override
    public List<RoleTO> list(final int page, final int size) {
        return controller.list(page, size);
    }

    @Override
    public RoleTO parent(final Long roleId) {
        return controller.parent(roleId);
    }

    @Override
    public RoleTO read(final Long roleId) {
        return controller.read(roleId);
    }

    @Override
    public List<RoleTO> search(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.search(searchCondition, 1, 25);
    }

    @Override
    public List<RoleTO> search(final NodeCond searchCondition, final int page, final int size)
            throws InvalidSearchConditionException {

        return controller.search(searchCondition, page, size);
    }

    @Override
    public int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.searchCount(searchCondition);
    }

    @Override
    public RoleTO readSelf(final Long roleId) {
        return controller.readSelf(roleId);
    }

    @Override
    public Response update(final Long roleId, final RoleMod roleMod) {
        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
            roleMod.setId(roleId);
            RoleTO updated = controller.update(roleMod);
            builder = modificationResponse(updated);
        }

        return builder.build();
    }

    @Override
    public Response associate(final Long roleId, final ResourceAssociationActionType type,
            final List<ResourceNameTO> resourceNames) {

        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
            RoleTO updated;

            switch (type) {
                case UNLINK:
                    updated = controller.unlink(roleId, CollectionWrapper.unwrap(resourceNames));
                    break;

                case UNASSIGN:
                    updated = controller.unassign(roleId, CollectionWrapper.unwrap(resourceNames));
                    break;

                case DEPROVISION:
                    updated = controller.deprovision(roleId, CollectionWrapper.unwrap(resourceNames));
                    break;

                default:
                    updated = controller.read(roleId);
            }

            builder = modificationResponse(updated);
        }

        return builder.build();
    }
}
