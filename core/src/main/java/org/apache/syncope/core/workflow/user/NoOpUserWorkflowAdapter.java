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
package org.apache.syncope.core.workflow.user;

import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.workflow.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple implementation basically not involving any workflow engine.
 */
@Transactional(rollbackFor = { Throwable.class })
public class NoOpUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    public static final String ENABLED = "enabled";

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck)
            throws WorkflowException {

        return create(userTO, disablePwdPolicyCheck, null);
    }

    @Override
    public WorkflowResult<Map.Entry<Long, Boolean>> create(final UserTO userTO, final boolean disablePwdPolicyCheck,
            final Boolean enabled)
            throws WorkflowException {

        SyncopeUser user = new SyncopeUser();
        dataBinder.create(user, userTO);

        // this will make SyncopeUserValidator not to consider password policies at all
        if (disablePwdPolicyCheck) {
            user.removeClearPassword();
        }

        String status;
        boolean propagateEnable;
        if (enabled == null) {
            status = "created";
            propagateEnable = true;
        } else {
            status = enabled
                    ? "active"
                    : "suspended";
            propagateEnable = enabled;
            user.setSuspended(!enabled);
        }

        user.setStatus(status);
        user = userDAO.save(user);

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, user.getResourceNames());

        return new WorkflowResult<Map.Entry<Long, Boolean>>(new SimpleEntry<Long, Boolean>(user.getId(),
                propagateEnable), propByRes, "create");
    }

    @Override
    protected WorkflowResult<Long> doActivate(final SyncopeUser user, final String token)
            throws WorkflowException {

        if (!user.checkToken(token)) {
            throw new WorkflowException(new IllegalArgumentException("Wrong token: " + token + " for " + user));
        }

        user.removeToken();
        user.setStatus("active");
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, "activate");
    }

    @Override
    protected WorkflowResult<Map.Entry<UserMod, Boolean>> doUpdate(final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        // update password internally only if required
        UserMod actualMod = SerializationUtils.clone(userMod);
        if (actualMod.getPwdPropRequest() != null && !actualMod.getPwdPropRequest().isOnSyncope()) {
            actualMod.setPassword(null);
        }
        // update SyncopeUser
        PropagationByResource propByRes = dataBinder.update(user, actualMod);

        SyncopeUser updated = userDAO.save(user);

        userMod.setId(updated.getId());
        return new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, !user.isSuspended()), propByRes, "update");
    }

    @Override
    protected WorkflowResult<Long> doSuspend(final SyncopeUser user)
            throws WorkflowException {

        user.setStatus("suspended");
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, "suspend");
    }

    @Override
    protected WorkflowResult<Long> doReactivate(final SyncopeUser user)
            throws WorkflowException {

        user.setStatus("active");
        SyncopeUser updated = userDAO.save(user);

        return new WorkflowResult<Long>(updated.getId(), null, "reactivate");
    }

    @Override
    protected void doDelete(final SyncopeUser user)
            throws WorkflowException {

        userDAO.delete(user);
    }

    @Override
    public WorkflowResult<Long> execute(final UserTO userTO, final String taskId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void exportDefinition(final WorkflowDefinitionFormat format, final OutputStream os)
            throws WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void exportDiagram(final OutputStream os) throws WorkflowException {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void importDefinition(final WorkflowDefinitionFormat format, final String definition)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Collections.emptyList();
    }

    @Override
    public List<WorkflowFormTO> getForms(final String workflowId, final String name) {
        return Collections.emptyList();
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId)
            throws NotFoundException, WorkflowException {

        return null;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId, final String username)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowResult<UserMod> submitForm(final WorkflowFormTO form, final String username)
            throws NotFoundException, WorkflowException {

        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }
}
