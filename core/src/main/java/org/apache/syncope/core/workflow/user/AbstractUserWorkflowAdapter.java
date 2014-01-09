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

import java.util.Map;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowInstanceLoader;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.identityconnectors.common.security.EncryptorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractUserWorkflowAdapter implements UserWorkflowAdapter {

    @Autowired
    protected UserDataBinder dataBinder;

    @Autowired
    protected UserDAO userDAO;

    public static String encrypt(final String clear) {
        byte[] encryptedBytes = EncryptorFactory.getInstance().getDefaultEncryptor().encrypt(clear.getBytes());

        return new String(Base64.encode(encryptedBytes));
    }

    public static String decrypt(final String crypted) {
        byte[] decryptedBytes =
                EncryptorFactory.getInstance().getDefaultEncryptor().decrypt(Base64.decode(crypted.getBytes()));

        return new String(decryptedBytes);
    }

    @Override
    public Class<? extends WorkflowInstanceLoader> getLoaderClass() {
        return null;
    }

    protected abstract WorkflowResult<Long> doActivate(SyncopeUser user, String token) throws WorkflowException;

    @Override
    public WorkflowResult<Long> activate(final Long userId, final String token)
            throws UnauthorizedRoleException, WorkflowException {

        return doActivate(dataBinder.getUserFromId(userId), token);
    }

    protected abstract WorkflowResult<Map.Entry<UserMod, Boolean>> doUpdate(SyncopeUser user, UserMod userMod)
            throws WorkflowException;

    @Override
    public WorkflowResult<Map.Entry<UserMod, Boolean>> update(final UserMod userMod)
            throws UnauthorizedRoleException, WorkflowException {

        return doUpdate(dataBinder.getUserFromId(userMod.getId()), userMod);
    }

    protected abstract WorkflowResult<Long> doSuspend(SyncopeUser user) throws WorkflowException;

    @Override
    public WorkflowResult<Long> suspend(final Long userId)
            throws UnauthorizedRoleException, WorkflowException {

        return suspend(dataBinder.getUserFromId(userId));
    }

    @Override
    public WorkflowResult<Long> suspend(final SyncopeUser user) throws UnauthorizedRoleException, WorkflowException {
        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    protected abstract WorkflowResult<Long> doReactivate(SyncopeUser user) throws WorkflowException;

    @Override
    public WorkflowResult<Long> reactivate(final Long userId) throws UnauthorizedRoleException, WorkflowException {
        final SyncopeUser user = dataBinder.getUserFromId(userId);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doDelete(SyncopeUser user) throws WorkflowException;

    @Override
    public void delete(final Long userId) throws UnauthorizedRoleException, WorkflowException {
        doDelete(dataBinder.getUserFromId(userId));
    }
}
