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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TaskDAOImpl extends AbstractDAOImpl implements TaskDAO {

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task> T find(final Long id) {
        return (T) entityManager.find(Task.class, id);
    }

    private <T extends Task> StringBuilder buildfindAllQuery(final Class<T> reference) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(reference.getSimpleName()).append(" e ");
        if (SchedTask.class.equals(reference)) {
            queryString.append("WHERE e.id NOT IN (SELECT e.id FROM ").
                    append(SyncTask.class.getSimpleName()).append(" e) ");
        }

        return queryString;
    }

    @Override
    public <T extends Task> List<T> findToExec(final Class<T> reference) {
        StringBuilder queryString = buildfindAllQuery(reference);

        if (SchedTask.class.equals(reference)) {
            queryString.append("AND ");
        } else {
            queryString.append("WHERE ");
        }

        if (reference.equals(NotificationTask.class)) {
            queryString.append("e.executed = 0 ");
        } else {
            queryString.append("e.executions IS EMPTY ");
        }
        queryString.append("ORDER BY e.id DESC");

        final TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        return query.getResultList();
    }

    @Override
    public <T extends Task> List<T> findAll(final ExternalResource resource, final Class<T> reference) {
        StringBuilder queryString = buildfindAllQuery(reference);

        if (SchedTask.class.equals(reference)) {
            queryString.append("AND ");
        } else {
            queryString.append("WHERE ");
        }

        queryString.append("e.resource=:resource ");
        queryString.append("ORDER BY e.id DESC");

        final TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public <T extends Task> List<T> findAll(final Class<T> reference) {
        return findAll(-1, -1, Collections.<OrderByClause>emptyList(), reference);
    }

    @Override
    public <T extends Task> List<T> findAll(final int page, final int itemsPerPage,
            final List<OrderByClause> orderByClauses, final Class<T> reference) {

        StringBuilder queryString = buildfindAllQuery(reference);
        queryString.append(orderByClauses.isEmpty()
                ? "ORDER BY e.id DESC"
                : toOrderByStatement(reference, "e", orderByClauses));

        final TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);

        query.setFirstResult(itemsPerPage * (page <= 0
                ? 0
                : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public <T extends Task> int count(final Class<T> reference) {
        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(id) FROM Task WHERE DTYPE=?1");
        countQuery.setParameter(1, reference.getSimpleName());

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task> T save(final T task) {
        return entityManager.merge(task);
    }

    @Override
    public <T extends Task> void delete(final Long id) {
        T task = find(id);
        if (task == null) {
            return;
        }

        delete(task);
    }

    @Override
    public <T extends Task> void delete(final T task) {
        entityManager.remove(task);
    }

    @Override
    public <T extends Task> void deleteAll(final ExternalResource resource, final Class<T> reference) {

        List<T> tasks = findAll(resource, reference);
        if (tasks != null) {
            List<Long> taskIds = new ArrayList<Long>(tasks.size());
            for (T task : tasks) {
                taskIds.add(task.getId());
            }
            for (Long taskId : taskIds) {
                delete(taskId);
            }
        }
    }
}
