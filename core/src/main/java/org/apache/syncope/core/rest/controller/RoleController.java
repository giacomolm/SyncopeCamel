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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.core.provisioning.ProvisioningManager;
import org.apache.syncope.core.provisioning.RoleProvisioningManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class RoleController extends AbstractResourceAssociator<RoleTO> {

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected AttributableSearchDAO searchDAO;

    @Autowired
    protected RoleDataBinder binder;

    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;
    
    @Resource(name = "defaultRoleProvisioningManager")
    protected RoleProvisioningManager provisioningManager;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @PreAuthorize("hasAnyRole('ROLE_READ', T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional(readOnly = true)
    public RoleTO read(final Long roleId) {
        SyncopeRole role;
        // bypass role entitlements check
        if (anonymousUser.equals(EntitlementUtil.getAuthenticatedUsername())) {
            role = roleDAO.find(roleId);
        } else {
            role = binder.getRoleFromId(roleId);
        }

        if (role == null) {
            throw new NotFoundException("Role " + roleId);
        }

        return binder.getRoleTO(role);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public RoleTO readSelf(final Long roleId) {
        // Explicit search instead of using binder.getRoleFromId() in order to bypass auth checks - will do here
        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + roleId);
        }

        Set<Long> ownedRoleIds;
        SyncopeUser authUser = userDAO.find(SecurityContextHolder.getContext().getAuthentication().getName());
        if (authUser == null) {
            ownedRoleIds = Collections.<Long>emptySet();
        } else {
            ownedRoleIds = authUser.getRoleIds();
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        allowedRoleIds.addAll(ownedRoleIds);
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        return binder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public RoleTO parent(final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (role.getParent() != null && !allowedRoleIds.contains(role.getParent().getId())) {
            throw new UnauthorizedRoleException(role.getParent().getId());
        }

        RoleTO result = role.getParent() == null
                ? null
                : binder.getRoleTO(role.getParent());

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public List<RoleTO> children(final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeRole> children = roleDAO.findChildren(role);
        List<RoleTO> childrenTOs = new ArrayList<RoleTO>(children.size());
        for (SyncopeRole child : children) {
            if (allowedRoleIds.contains(child.getId())) {
                childrenTOs.add(binder.getRoleTO(child));
            }
        }

        return childrenTOs;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public int count() {
        return roleDAO.count();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<RoleTO> list(final int page, final int size, final List<OrderByClause> orderBy) {
        List<SyncopeRole> roles = roleDAO.findAll(page, size, orderBy);

        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(binder.getRoleTO(role));
        }

        return roleTOs;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public int searchCount(final SearchCond searchCondition) {
        final Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        return searchDAO.count(adminRoleIds, searchCondition, AttributableUtil.getInstance(AttributableType.ROLE));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public List<RoleTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy) {

        final List<SyncopeRole> matchingRoles = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, page, size, orderBy, AttributableUtil.getInstance(AttributableType.ROLE));

        final List<RoleTO> result = new ArrayList<RoleTO>(matchingRoles.size());
        for (SyncopeRole role : matchingRoles) {
            result.add(binder.getRoleTO(role));
        }

        return result;
    }

    @PreAuthorize("hasRole('ROLE_CREATE')")
    public RoleTO create(final RoleTO roleTO) {
        // Check that this operation is allowed to be performed by caller
        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (roleTO.getParent() != 0 && !allowedRoleIds.contains(roleTO.getParent())) {
            throw new UnauthorizedRoleException(roleTO.getParent());
        }

        // Attributable transformation (if configured)
        RoleTO actual = attrTransformer.transform(roleTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(roleTO);

        final RoleTO savedTO = binder.getRoleTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    public RoleTO update(final RoleMod roleMod) {
        // Check that this operation is allowed to be performed by caller
        binder.getRoleFromId(roleMod.getId());

        // Attribute value transformation (if configured)
        RoleMod actual = attrTransformer.transform(roleMod);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(roleMod);
        
        final RoleTO updatedTO = binder.getRoleTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    public RoleTO delete(final Long roleId) {
        List<SyncopeRole> ownedRoles = roleDAO.findOwnedByRole(roleId);
        if (!ownedRoles.isEmpty()) {
            List<String> owned = new ArrayList<String>(ownedRoles.size());
            for (SyncopeRole role : ownedRoles) {
                owned.add(role.getId() + " " + role.getName());
            }

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RoleOwnership);
            sce.getElements().addAll(owned);
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(roleId);
        
        RoleTO roleTO = new RoleTO();
        roleTO.setId(roleId);

        roleTO.getPropagationStatusTOs().addAll(statuses);

        return roleTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO unlink(final Long roleId, final Collection<String> resources) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleId);
        roleMod.getResourcesToRemove().addAll(resources);
        final Long updatedResult = provisioningManager.unlink(roleMod);

        return binder.getRoleTO(updatedResult);
    }
    
    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO link(final Long roleId, final Collection<String> resources) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleId);
        roleMod.getResourcesToAdd().addAll(resources);
        return binder.getRoleTO(provisioningManager.link(roleMod));
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO unassign(final Long roleId, final Collection<String> resources) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleId);
        roleMod.getResourcesToRemove().addAll(resources);
        return update(roleMod);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO assign(
            final Long roleId, final Collection<String> resources, final boolean changePwd, final String password) {
        final RoleMod userMod = new RoleMod();
        userMod.setId(roleId);
        userMod.getResourcesToAdd().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO deprovision(final Long roleId, final Collection<String> resources) {
        final SyncopeRole role = binder.getRoleFromId(roleId);

        List<PropagationStatus> statuses = provisioningManager.deprovision(roleId, resources);

        final RoleTO updatedTO = binder.getRoleTO(role);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO provision(
            final Long roleId, final Collection<String> resources, final boolean changePwd, final String password) {
        final RoleTO original = binder.getRoleTO(roleId);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(roleId, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RoleTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof RoleTO) {
                    id = ((RoleTO) args[i]).getId();
                } else if (args[i] instanceof RoleMod) {
                    id = ((RoleMod) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getRoleTO(id);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
