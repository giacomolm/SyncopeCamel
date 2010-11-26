/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.beans.role.RDerAttr;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;

@Repository
public class SyncopeRoleDAOImpl extends AbstractDAOImpl
        implements SyncopeRoleDAO {

    @Override
    public SyncopeRole find(final String name, final Long parentId) {
        Query query;
        if (parentId != null) {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "name=:name AND parent.id=:parentId");
            query.setParameter("parentId", parentId);
        } else {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "name=:name AND parent IS NULL");
        }
        query.setParameter("name", name);

        List<SyncopeRole> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public SyncopeRole find(final Long id) {
        return entityManager.find(SyncopeRole.class, id);
    }

    @Override
    public List<SyncopeRole> findChildren(final Long roleId) {
        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + "parent.id=:roleId");
        query.setParameter("roleId", roleId);
        return query.getResultList();
    }

    private List<Long> getAncestors(final SyncopeRole role,
            final List<Long> ancestors) {

        ancestors.add(role.getId());

        if (role.getParent() != null && role.isInheritAttributes()) {
            return getAncestors(role.getParent(), ancestors);
        }

        return ancestors;
    }

    @Override
    public List<RAttr> findInheritedAttributes(final SyncopeRole role) {
        if (role.getParent() == null) {
            return Collections.EMPTY_LIST;
        }

        List<Long> ancestors = getAncestors(role.getParent(),
                new ArrayList<Long>());
        if (ancestors == null || ancestors.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        StringBuilder queryExp = new StringBuilder();
        queryExp.append("SELECT ra ").
                append("FROM " + RAttr.class.getSimpleName() + " ra ").
                append("WHERE ra.owner.id = ");
        queryExp.append(ancestors.get(0));

        if (ancestors.size() > 1) {
            for (int i = 1; i < ancestors.size(); i++) {
                queryExp.append("OR ra.owner.id = ");
                queryExp.append(ancestors.get(i));
                queryExp.append(" ");
            }
        }
        queryExp.append("ORDER BY ra.owner.id ASC");

        Query query = entityManager.createQuery(queryExp.toString());
        return query.getResultList();
    }

    @Override
    public List<RDerAttr> findInheritedDerivedAttributes(
            final SyncopeRole role) {

        if (role.getParent() == null) {
            return Collections.EMPTY_LIST;
        }

        List<Long> ancestors = getAncestors(role.getParent(),
                new ArrayList<Long>());
        if (ancestors == null || ancestors.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        StringBuilder queryExp = new StringBuilder();
        queryExp.append("SELECT rda ").
                append("FROM " + RDerAttr.class.getSimpleName() + " rda ").
                append("WHERE rda.owner.id = ");
        queryExp.append(ancestors.get(0));

        if (ancestors.size() > 1) {
            for (int i = 1; i < ancestors.size(); i++) {
                queryExp.append("OR rda.owner.id = ");
                queryExp.append(ancestors.get(i));
                queryExp.append(" ");
            }
        }
        queryExp.append("ORDER BY rda.owner.id ASC");

        Query query = entityManager.createQuery(queryExp.toString());
        return query.getResultList();
    }

    @Override
    public List<SyncopeRole> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeRole e");
        return query.getResultList();
    }

    @Override
    public SyncopeRole save(final SyncopeRole syncopeRole) {
        return entityManager.merge(syncopeRole);
    }

    @Override
    public void delete(final Long id) {
        SyncopeRole role = find(id);
        if (role == null) {
            return;
        }

        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + "parent_id=:id");
        query.setParameter("id", id);
        List<SyncopeRole> childrenRoles = query.getResultList();
        for (SyncopeRole child : childrenRoles) {
            delete(child.getId());
        }

        for (Membership membership : role.getMemberships()) {
            membership.setSyncopeRole(null);
            membership.getSyncopeUser().removeMembership(membership);
            membership.setSyncopeRole(null);

            entityManager.remove(membership);
        }
        role.setMemberships(Collections.EMPTY_LIST);

        for (Entitlement entitlement : role.getEntitlements()) {
            entitlement.removeRole(role);
        }
        role.setEntitlements(Collections.EMPTY_SET);

        role.setParent(null);
        entityManager.remove(role);
    }
}
