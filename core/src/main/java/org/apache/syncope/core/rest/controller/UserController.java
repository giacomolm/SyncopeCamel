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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.reqres.BulkActionResult.Status;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.provisioning.UserProvisioningManager;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.provisioning.camel.CamelUserProvisioningManager;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Note that this controller does not extend AbstractTransactionalController, hence does not provide any
 * Spring's Transactional logic at class level.
 *
 * @see AbstractTransactionalController
 */
@Component
public class UserController extends AbstractResourceAssociator<UserTO> {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected AttributableSearchDAO searchDAO;

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;
    
    @Resource(name = "defaultUserProvisioningManager")
    protected UserProvisioningManager provisioningManager;

    public boolean isSelfRegistrationAllowed() {
        return Boolean.valueOf(confDAO.find("selfRegistration.allowed", "false").getValue());
    }

    @PreAuthorize("hasRole('USER_READ')")
    public String getUsername(final Long userId) {
        return binder.getUserTO(userId).getUsername();
    }

    @PreAuthorize("hasRole('USER_READ')")
    public Long getUserId(final String username) {
        return binder.getUserTO(username).getId();
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public int count() {
        return userDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public int searchCount(final SearchCond searchCondition) {
        return searchDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public List<UserTO> list(final int page, final int size, final List<OrderByClause> orderBy) {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size, orderBy);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(binder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public UserTO readSelf() {
        return binder.getAuthenticatedUserTO();
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true)
    public UserTO read(final Long userId) {
        return binder.getUserTO(userId);
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true)
    public List<UserTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy) {

        final List<SyncopeUser> matchingUsers = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, page, size, orderBy,
                AttributableUtil.getInstance(AttributableType.USER));

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(binder.getUserTO(user));
        }

        return result;
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    public UserTO createSelf(final UserTO userTO) {
        if (!isSelfRegistrationAllowed()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unauthorized);
            sce.getElements().add("SelfRegistration forbidden by configuration");
        }

        return doCreate(userTO);
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    public UserTO create(final UserTO userTO) {
        Set<Long> requestRoleIds = new HashSet<Long>(userTO.getMemberships().size());
        for (MembershipTO membership : userTO.getMemberships()) {
            requestRoleIds.add(membership.getRoleId());
        }
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        requestRoleIds.removeAll(adminRoleIds);
        if (!requestRoleIds.isEmpty()) {
            throw new UnauthorizedRoleException(requestRoleIds);
        }

        return doCreate(userTO);
    }

    protected UserTO doCreate(final UserTO userTO) {
        // Attributable transformation (if configured)
        UserTO actual = attrTransformer.transform(userTO);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>>
                created = provisioningManager.create(actual);

        final UserTO savedTO = binder.getUserTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    public UserTO updateSelf(final UserMod userMod) {
        UserTO userTO = binder.getAuthenticatedUserTO();

        if (userTO.getId() != userMod.getId()) {
            throw new AccessControlException("Not allowed for user id " + userMod.getId());
        }

        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    public UserTO update(final UserMod userMod) {
        // AttributableMod transformation (if configured)
        UserMod actual = attrTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(actual);

        final UserTO updatedTO = binder.getUserTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    protected Map.Entry<Long, List<PropagationStatus>> setStatusOnWfAdapter(final SyncopeUser user, final StatusMod statusMod) {
        Map.Entry<Long, List<PropagationStatus>> updated;

        switch (statusMod.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(user, statusMod);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(user, statusMod);
                break;

            case ACTIVATE:
            default: 
                updated = provisioningManager.activate(user, statusMod);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public UserTO status(final StatusMod statusMod) {
        SyncopeUser user = binder.getUserFromId(statusMod.getId());

        Map.Entry<Long, List<PropagationStatus>> 
            updated = setStatusOnWfAdapter(user, statusMod);

        final UserTO savedTO = binder.getUserTO(updated.getKey());
        savedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return savedTO;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    public UserTO deleteSelf() {
        UserTO userTO = binder.getAuthenticatedUserTO();

        return delete(userTO.getId());
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    public UserTO delete(final Long userId) {
        
        List<SyncopeRole> ownedRoles = roleDAO.findOwnedByUser(userId);
        if (!ownedRoles.isEmpty()) {
            List<String> owned = new ArrayList<String>(ownedRoles.size());
            for (SyncopeRole role : ownedRoles) {
                owned.add(role.getId() + " " + role.getName());
            }

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RoleOwnership);
            sce.getElements().addAll(owned);
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(userId);

        final UserTO deletedTO;
        SyncopeUser deleted = userDAO.find(userId);
        if (deleted == null) {
            deletedTO = new UserTO();
            deletedTO.setId(userId);
        } else {
            deletedTO = binder.getUserTO(userId);
        }
        deletedTO.getPropagationStatusTOs().addAll(statuses);

        return deletedTO;
    }

    @PreAuthorize("(hasRole('USER_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE) or "
            + "(hasRole('USER_UPDATE') and "
            + "(#bulkAction.operation == #bulkAction.operation.REACTIVATE or "
            + "#bulkAction.operation == #bulkAction.operation.SUSPEND))")
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult res = new BulkActionResult();

        switch (bulkAction.getOperation()) {
            case DELETE:
                for (String userId : bulkAction.getTargets()) {
                    try {
                        res.add(delete(Long.valueOf(userId)).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;

            case SUSPEND:
                for (String userId : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setId(Long.valueOf(userId));
                    statusMod.setType(StatusMod.ModType.SUSPEND);
                    try {
                        res.add(status(statusMod).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing suspend for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;

            case REACTIVATE:
                for (String userId : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setId(Long.valueOf(userId));
                    statusMod.setType(StatusMod.ModType.REACTIVATE);
                    try {
                        res.add(status(statusMod).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing reactivate for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;

            default:
        }

        return res;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO unlink(final Long userId, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToRemove().addAll(resources);
        Long updatedId = provisioningManager.unlink(userMod);

        return binder.getUserTO(updatedId);
    }


    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO link(final Long userId, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToAdd().addAll(resources);
        return binder.getUserTO(provisioningManager.link(userMod));
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO unassign(final Long userId, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToRemove().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO assign(
            final Long userId,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToAdd().addAll(resources);

        if (changepwd) {
            StatusMod statusMod = new StatusMod();
            statusMod.setOnSyncope(false);
            statusMod.getResourceNames().addAll(resources);
            userMod.setPwdPropRequest(statusMod);
            userMod.setPassword(password);
        }

        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO deprovision(final Long userId, final Collection<String> resources) {
        final SyncopeUser user = binder.getUserFromId(userId);        
        
        List<PropagationStatus> statuses = provisioningManager.deprovision(userId, resources);

        final UserTO updatedUserTO = binder.getUserTO(user);
        updatedUserTO.getPropagationStatusTOs().addAll(statuses);
        return updatedUserTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(readOnly = true)
    @Override
    public UserTO provision(
            final Long userId,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        final UserTO original = binder.getUserTO(userId);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(userId, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Object id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof String) {
                    id = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    id = ((UserTO) args[i]).getId();
                } else if (args[i] instanceof UserMod) {
                    id = ((UserMod) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return id instanceof Long ? binder.getUserTO((Long) id) : binder.getUserTO((String) id);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
    
    public PrintStream getDefinition() throws FileNotFoundException{
        String result = "";
        if(provisioningManager instanceof CamelUserProvisioningManager){
            List l = ((CamelUserProvisioningManager)provisioningManager).getRoutes();
            Iterator<CamelRoute> it = l.iterator();
            
            while(it.hasNext()){
                result += it.next().getRouteContent();
            }                        
        }
        return new PrintStream(result);
            
    }
}
