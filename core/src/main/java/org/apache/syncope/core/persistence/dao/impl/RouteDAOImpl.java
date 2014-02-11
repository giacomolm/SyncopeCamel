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

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.persistence.dao.RouteDAO;
import static org.apache.syncope.core.persistence.dao.impl.AbstractDAOImpl.LOG;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RouteDAOImpl extends AbstractDAOImpl implements RouteDAO{

    public static int i = 0;

    @Override
    public CamelRoute find(Long id) {
        TypedQuery<CamelRoute> query = entityManager.createQuery(
                "SELECT e FROM " + CamelRoute.class.getSimpleName() + " e WHERE e.id = :id", CamelRoute.class);
        query.setParameter("id", id);          
        
        CamelRoute result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with id {}", id, e);
        }

        return result;
    }

    @Override
    public List<CamelRoute> findAll() {
        TypedQuery<CamelRoute> query = entityManager.createQuery("SELECT e FROM "+ CamelRoute.class.getSimpleName() +" e",CamelRoute.class);
        return query.getResultList();
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public CamelRoute save(CamelRoute route) throws InvalidEntityException {
                     
	return entityManager.merge(route);
    }	

    @Override
    public void delete(Long id) {
	CamelRoute route = null;
        route = find(id);
        if(route!=null) entityManager.remove(route);

    }
    
    public EntityManager getEm(){
        return entityManager;
    }
    
}
