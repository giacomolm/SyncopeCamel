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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserWorkflowController extends AbstractTransactionalController<WorkflowFormTO> {

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected UserDataBinder binder;

    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @Transactional(rollbackFor = { Throwable.class })
    public WorkflowFormTO claimForm(final String taskId) {
        return uwfAdapter.claimForm(taskId, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    public UserTO executeWorkflowTask(final UserTO userTO, final String taskId) {
        WorkflowResult<Long> updated = uwfAdapter.execute(userTO, taskId);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                        new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, null),
                        updated.getPropByRes(), updated.getPerformedTasks()));

        taskExecutor.execute(tasks);

        return binder.getUserTO(updated.getResult());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public WorkflowFormTO getFormForUser(final Long userId) {
        SyncopeUser user = binder.getUserFromId(userId);
        return uwfAdapter.getForm(user.getWorkflowId());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @Transactional(rollbackFor = { Throwable.class })
    public List<WorkflowFormTO> getForms() {
        return uwfAdapter.getForms();
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public List<WorkflowFormTO> getForms(final Long userId, final String formName) {
        SyncopeUser user = binder.getUserFromId(userId);
        return uwfAdapter.getForms(user.getWorkflowId(), formName);
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @Transactional(rollbackFor = { Throwable.class })
    public UserTO submitForm(final WorkflowFormTO form) {
        WorkflowResult<? extends AbstractAttributableMod> updated =
                uwfAdapter.submitForm(form, SecurityContextHolder.getContext().getAuthentication().getName());

        // propByRes can be made empty by the workflow definition if no propagation should occur 
        // (for example, with rejected users)
        if (updated.getResult() instanceof UserMod
                && updated.getPropByRes() != null && !updated.getPropByRes().isEmpty()) {

            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                            new AbstractMap.SimpleEntry<UserMod, Boolean>((UserMod) updated.getResult(), Boolean.TRUE),
                            updated.getPropByRes(),
                            updated.getPerformedTasks()));

            taskExecutor.execute(tasks);
        }

        return binder.getUserTO(updated.getResult().getId());
    }

    @Override
    protected WorkflowFormTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }
}
