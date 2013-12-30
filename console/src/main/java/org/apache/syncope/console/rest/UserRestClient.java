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

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.ResourceAssociationMod;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeAssociationActionType;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest users services.
 */
@Component
public class UserRestClient extends AbstractAttributableRestClient {

    private static final long serialVersionUID = -1575748964398293968L;

    @Override
    public int count() {
        return getService(UserService.class).list(1, 1).getTotalCount();
    }

    @Override
    public List<UserTO> list(final int page, final int size, final SortParam<String> sort) {
        return getService(UserService.class).list(page, size, toOrderBy(sort)).getResult();
    }

    public UserTO create(final UserTO userTO) {
        Response response = getService(UserService.class).create(userTO);
        return response.readEntity(UserTO.class);
    }

    public UserTO update(final UserMod userMod) {
        return getService(UserService.class).update(userMod.getId(), userMod).readEntity(UserTO.class);
    }

    @Override
    public UserTO delete(final Long id) {
        return getService(UserService.class).delete(id).readEntity(UserTO.class);
    }

    public UserTO read(final Long id) {
        UserTO userTO = null;
        try {
            userTO = getService(UserService.class).read(id);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a user", e);
        }
        return userTO;
    }

    @Override
    public int searchCount(final String fiql) {
        return getService(UserService.class).search(fiql, 1, 1).getTotalCount();
    }

    @Override
    public List<UserTO> search(final String fiql, final int page, final int size, final SortParam<String> sort) {
        return getService(UserService.class).search(fiql, page, size, toOrderBy(sort)).getResult();
    }

    @Override
    public ConnObjectTO getConnectorObject(final String resourceName, final Long id) {
        return getService(ResourceService.class).getConnectorObject(resourceName, AttributableType.USER, id);
    }

    public void suspend(final long userId, final List<StatusBean> statuses) {
        StatusMod statusMod = StatusUtils.buildStatusMod(statuses, false);
        statusMod.setType(StatusMod.ModType.SUSPEND);
        getService(UserService.class).status(userId, statusMod);
    }

    public void reactivate(final long userId, final List<StatusBean> statuses) {
        StatusMod statusMod = StatusUtils.buildStatusMod(statuses, true);
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        getService(UserService.class).status(userId, statusMod);
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(UserService.class).bulk(action);
    }

    public BulkActionResult unlink(final long userId, final List<StatusBean> statuses) {
        return getService(UserService.class).bulkDeassociation(userId, ResourceDeAssociationActionType.UNLINK,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class))
                .readEntity(BulkActionResult.class);
    }

    public BulkActionResult link(final long userId, final List<StatusBean> statuses) {
        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class));

        return getService(UserService.class).bulkAssociation(userId, ResourceAssociationActionType.LINK, associationMod)
                .readEntity(BulkActionResult.class);
    }

    public BulkActionResult deprovision(final long userId, final List<StatusBean> statuses) {
        return getService(UserService.class).bulkDeassociation(userId, ResourceDeAssociationActionType.DEPROVISION,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class))
                .readEntity(BulkActionResult.class);
    }

    public BulkActionResult provision(
            final long userId, final List<StatusBean> statuses, final boolean changepwd, final String password) {
        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class));
        associationMod.setChangePwd(changepwd);
        associationMod.setPassword(password);

        return getService(UserService.class)
                .bulkAssociation(userId, ResourceAssociationActionType.PROVISION, associationMod)
                .readEntity(BulkActionResult.class);
    }

    public BulkActionResult unassign(final long userId, final List<StatusBean> statuses) {
        return getService(UserService.class).bulkDeassociation(userId, ResourceDeAssociationActionType.UNASSIGN,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class))
                .readEntity(BulkActionResult.class);
    }

    public BulkActionResult assign(
            final long userId, final List<StatusBean> statuses, final boolean changepwd, final String password) {
        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class));
        associationMod.setChangePwd(changepwd);
        associationMod.setPassword(password);

        return getService(UserService.class).bulkAssociation(userId, ResourceAssociationActionType.ASSIGN,
                associationMod)
                .readEntity(BulkActionResult.class);
    }
}
