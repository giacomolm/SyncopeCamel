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
package org.apache.syncope.services.proxy;

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.client.mod.StatusMod;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.to.WorkflowFormTO;
import org.apache.syncope.services.UserService;

public class UserServiceProxy extends SpringServiceProxy implements UserService {

    public UserServiceProxy(String baseUrl, SpringRestTemplate callback) {
        super(baseUrl, callback);
    }

    @Override
    public Boolean verifyPassword(String username, String password) {
        return getRestTemplate().getForObject(
                baseUrl + "user/verifyPassword/{username}.json?password={password}", Boolean.class,
                username, password);
    }

    @Override
    public int count() {
        return getRestTemplate().getForObject(baseUrl + "user/count.json", Integer.class);
    }

    @Override
    public List<UserTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "user/list.json", UserTO[].class));
    }

    @Override
    public List<UserTO> list(int page, int size) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "user/list/{page}/{size}.json",
                UserTO[].class, page, size));
    }

    @Override
    public UserTO read(Long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/read/{userId}.json", UserTO.class, userId);
    }

    @Override
    public UserTO read(String username) {
        return getRestTemplate().getForObject(baseUrl + "user/readByUsername/{username}.json", UserTO.class,
                username);
    }

    @Override
    public UserTO create(UserTO userTO) {
        return getRestTemplate().postForObject(baseUrl + "user/create", userTO, UserTO.class);
    }

    @Override
    public UserTO update(Long userId, UserMod userMod) {
        return getRestTemplate().postForObject(baseUrl + "user/update", userMod, UserTO.class);
    }

    @Override
    public UserTO delete(Long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/delete/{userId}", UserTO.class, userId);
    }

    @Override
    public UserTO executeWorkflow(String taskId, UserTO userTO) {
        return null;
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "user/workflow/form/list",
                WorkflowFormTO[].class));
    }

    @Override
    public WorkflowFormTO getFormForUser(Long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/workflow/form/{userId}", WorkflowFormTO.class,
                userId);
    }

    @Override
    public WorkflowFormTO claimForm(String taskId) {
        return getRestTemplate().getForObject(baseUrl + "user/workflow/form/claim/{taskId}",
                WorkflowFormTO.class, taskId);
    }

    @Override
    public UserTO submitForm(WorkflowFormTO form) {
        return getRestTemplate().postForObject(baseUrl + "user/workflow/form/submit", form, UserTO.class);
    }

    @Override
    public UserTO activate(long userId, String token) {
        return getRestTemplate().getForObject(baseUrl + "user/activate/{userId}?token=" + token, UserTO.class,
                userId);
    }

    @Override
    public UserTO activateByUsername(String username, String token) {
        return getRestTemplate().getForObject(baseUrl + "user/activateByUsername/{username}.json?token=" + token,
                UserTO.class, username);
    }

    @Override
    public UserTO suspend(long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/suspend/{userId}", UserTO.class, userId);
    }

    @Override
    public UserTO reactivate(long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/reactivate/{userId}", UserTO.class, userId);
    }

    @Override
    public UserTO reactivate(long userId, String query) {
        return getRestTemplate().getForObject(baseUrl + "user/reactivate/" + userId + query, UserTO.class);
    }

    @Override
    public UserTO suspendByUsername(String username) {
        return getRestTemplate().getForObject(baseUrl + "user/suspendByUsername/{username}.json", UserTO.class,
                username);
    }

    @Override
    public UserTO reactivateByUsername(String username) {
        return getRestTemplate().getForObject(baseUrl + "user/reactivateByUsername/{username}.json",
                UserTO.class, username);
    }

    @Override
    public UserTO suspend(long userId, String query) {
        return getRestTemplate().getForObject(baseUrl + "user/suspend/" + userId + query, UserTO.class);
    }

    @Override
    public UserTO readSelf() {
        return getRestTemplate().getForObject(baseUrl + "user/read/self", UserTO.class);
    }

    @Override
    public List<UserTO> search(NodeCond searchCondition) {
        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "user/search", searchCondition,
                UserTO[].class));
    }

    @Override
    public List<UserTO> search(NodeCond searchCondition, int page, int size) {
        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "user/search/{page}/{size}",
                searchCondition, UserTO[].class, page, size));
    }

    @Override
    public int searchCount(NodeCond searchCondition) {
        return getRestTemplate()
                .postForObject(baseUrl + "user/search/count.json", searchCondition, Integer.class);
    }

    @Override
    public UserTO setStatus(Long userId, StatusMod statusUpdate) {
        return null; // Not used in old REST API
    }

}