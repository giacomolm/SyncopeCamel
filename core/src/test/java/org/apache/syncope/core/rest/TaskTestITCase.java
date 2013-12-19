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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.wrap.JobClass;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.wrap.SyncActionClass;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.core.sync.TestSyncActions;
import org.apache.syncope.core.sync.TestSyncRule;
import org.apache.syncope.core.sync.impl.SyncJob;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class TaskTestITCase extends AbstractTest {

    private static final Long SCHED_TASK_ID = 5L;

    private static final Long SYNC_TASK_ID = 4L;

    /**
     * Remove initial and synchronized users to make test re-runnable.
     */
    public void removeTestUsers() {
        for (int i = 0; i < 10; i++) {
            String cUserName = "test" + i;
            try {
                UserTO cUserTO = readUser(cUserName);
                userService.delete(cUserTO.getId());
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void getJobClasses() {
        List<JobClass> jobClasses = taskService.getJobClasses();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void getSyncActionsClasses() {
        List<SyncActionClass> actions = taskService.getSyncActionsClasses();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test create Sync");
        task.setResource(RESOURCE_NAME_WS2);

        UserTO userTemplate = new UserTO();
        userTemplate.getResources().add(RESOURCE_NAME_WS2);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTemplate.getMemberships().add(membershipTO);
        task.setUserTemplate(userTemplate);

        RoleTO roleTemplate = new RoleTO();
        roleTemplate.getResources().add(RESOURCE_NAME_LDAP);
        task.setRoleTemplate(roleTemplate);

        Response response = taskService.create(task);
        SyncTaskTO actual = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(actual.getId());
        assertNotNull(task);
        assertEquals(actual.getId(), task.getId());
        assertEquals(actual.getJobClassName(), task.getJobClassName());
        assertEquals(userTemplate, task.getUserTemplate());
        assertEquals(roleTemplate, task.getRoleTemplate());
    }

    @Test
    public void update() {
        SchedTaskTO task = taskService.read(SCHED_TASK_ID);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setId(5);
        taskMod.setCronExpression(null);

        taskService.update(taskMod.getId(), taskMod);
        SchedTaskTO actual = taskService.read(taskMod.getId());
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void list() {
        PagedResult<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }
    }

    @Test
    public void paginatedList() {
        PagedResult<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION, 1, 2);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());
        assertEquals(2, tasks.getResult().size());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(TaskType.PROPAGATION, 2, 2);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(TaskType.PROPAGATION, 1000, 2);

        assertNotNull(tasks);
        assertTrue(tasks.getResult().isEmpty());
    }

    @Test
    public void read() {
        PropagationTaskTO taskTO = taskService.read(3L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        TaskExecTO taskTO = taskService.readExecution(6L);
        assertNotNull(taskTO);
    }

    @Test
    // Currently test is not re-runnable.
    // To successfully run test second time it is necessary to restart cargo.
    public void deal() {
        try {
            taskService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        TaskExecTO exec = taskService.execute(1L, false);
        assertEquals(PropagationTaskExecStatus.SUBMITTED.name(), exec.getStatus());

        ReportExecTO report = new ReportExecTO();
        report.setStatus(PropagationTaskExecStatus.SUCCESS.name());
        report.setMessage("OK");
        taskService.report(exec.getId(), report);
        exec = taskService.readExecution(exec.getId());
        assertEquals(PropagationTaskExecStatus.SUCCESS.name(), exec.getStatus());
        assertEquals("OK", exec.getMessage());

        taskService.delete(1L);
        try {
            taskService.readExecution(exec.getId());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void sync() {
        removeTestUsers();

        // -----------------------------
        // Create a new user ... it should be updated applying sync policy
        // -----------------------------
        UserTO inUserTO = new UserTO();
        inUserTO.setPassword("password123");
        String userName = "test9";
        inUserTO.setUsername(userName);
        inUserTO.getAttrs().add(attributeTO("firstname", "nome9"));
        inUserTO.getAttrs().add(attributeTO("surname", "cognome"));
        inUserTO.getAttrs().add(attributeTO("type", "a type"));
        inUserTO.getAttrs().add(attributeTO("fullname", "nome cognome"));
        inUserTO.getAttrs().add(attributeTO("userId", "puccini@syncope.apache.org"));
        inUserTO.getAttrs().add(attributeTO("email", "puccini@syncope.apache.org"));
        inUserTO.getDerAttrs().add(attributeTO("csvuserid", null));

        inUserTO = createUser(inUserTO);
        assertNotNull(inUserTO);

        // -----------------------------
        try {
            int usersPre = userService.list(1, 1).getTotalCount();
            assertNotNull(usersPre);

            // Update sync task
            SyncTaskTO task = taskService.read(SYNC_TASK_ID);
            assertNotNull(task);

            // add custom SyncJob actions
            task.setActionsClassName(TestSyncActions.class.getName());

            // add user template
            UserTO template = new UserTO();
            template.getAttrs().add(attributeTO("type",
                    "email == 'test8@syncope.apache.org'? 'TYPE_8': 'TYPE_OTHER'"));
            template.getDerAttrs().add(attributeTO("cn", null));
            template.getResources().add(RESOURCE_NAME_TESTDB);

            MembershipTO membershipTO = new MembershipTO();
            membershipTO.setRoleId(8L);
            membershipTO.getAttrs().add(attributeTO("subscriptionDate", "'2009-08-18T16:33:12.203+0200'"));
            template.getMemberships().add(membershipTO);

            task.setUserTemplate(template);

            taskService.update(task.getId(), task);
            SyncTaskTO actual = taskService.read(task.getId());
            assertNotNull(actual);
            assertEquals(task.getId(), actual.getId());
            assertEquals(TestSyncActions.class.getName(), actual.getActionsClassName());

            execSyncTask(SYNC_TASK_ID, 50, false);

            // after execution of the sync task the user data should be synced from
            // csv datasource and processed by user template
            UserTO userTO = userService.read(inUserTO.getId());
            assertNotNull(userTO);
            assertEquals("test9", userTO.getUsername());
            assertEquals(ActivitiDetector.isActivitiEnabledForUsers() ? "active" : "created", userTO.getStatus());
            assertEquals("test9@syncope.apache.org", userTO.getAttrMap().get("email").getValues().get(0));
            assertEquals("test9@syncope.apache.org", userTO.getAttrMap().get("userId").getValues().get(0));
            assertTrue(Integer.valueOf(userTO.getAttrMap().get("fullname").getValues().get(0)) <= 10);

            // check for user template
            userTO = readUser("test7");
            assertNotNull(userTO);
            assertEquals("TYPE_OTHER", userTO.getAttrMap().get("type").getValues().get(0));
            assertEquals(2, userTO.getResources().size());
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));
            assertEquals(1, userTO.getMemberships().size());
            assertTrue(userTO.getMemberships().get(0).getAttrMap().containsKey("subscriptionDate"));

            userTO = readUser("test8");
            assertNotNull(userTO);
            assertEquals("TYPE_8", userTO.getAttrMap().get("type").getValues().get(0));

            // check for sync results
            int usersPost = userService.list(1, 1).getTotalCount();
            assertNotNull(usersPost);
            assertEquals(usersPre + 9, usersPost);

            // Check for issue 215:
            // * expected disabled user test1
            // * expected enabled user test2
            userTO = readUser("test1");
            assertNotNull(userTO);
            assertEquals("suspended", userTO.getStatus());

            userTO = readUser("test3");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());

            // SYNCOPE-317
            execSyncTask(SYNC_TASK_ID, 50, false);
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void reconcileFromDB() {
        // update sync task
        SyncTaskTO task = taskService.read(7L);
        assertNotNull(task);

        // add user template
        UserTO template = new UserTO();
        template.getAttrs().add(attributeTO("type", "'type a'"));
        template.getAttrs().add(attributeTO("userId", "'reconciled@syncope.apache.org'"));
        template.getAttrs().add(attributeTO("fullname", "'reconciled fullname'"));
        template.getAttrs().add(attributeTO("surname", "'surname'"));

        task.setUserTemplate(template);

        taskService.update(task.getId(), task);
        SyncTaskTO actual = taskService.read(task.getId());
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(template, actual.getUserTemplate());
        assertEquals(new RoleTO(), actual.getRoleTemplate());

        TaskExecTO execution = execSyncTask(actual.getId(), 20, false);
        assertNotNull(execution.getStatus());
        assertTrue(PropagationTaskExecStatus.valueOf(execution.getStatus()).isSuccessful());

        UserTO userTO = readUser("testuser1");
        assertNotNull(userTO);
        assertEquals("reconciled@syncope.apache.org", userTO.getAttrMap().get("userId").getValues().get(0));
        assertEquals("suspended", userTO.getStatus());

        // enable user on external resource
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("UPDATE TEST SET STATUS=TRUE");

        // re-execute the same SyncTask: now user must be active
        execution = execSyncTask(actual.getId(), 20, false);
        assertNotNull(execution.getStatus());
        assertTrue(PropagationTaskExecStatus.valueOf(execution.getStatus()).isSuccessful());

        userTO = readUser("testuser1");
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void reconcileFromLDAP() {
        // Update sync task
        SyncTaskTO task = taskService.read(11L);
        assertNotNull(task);

        //  add user template
        final UserTO userTemplate = task.getUserTemplate();
        userTemplate.getResources().add(RESOURCE_NAME_LDAP);
        userTemplate.getVirAttrs().add(attributeTO("virtualReadOnly", ""));

        task.setUserTemplate(userTemplate);

        //  add role template
        RoleTO roleTemplate = new RoleTO();
        roleTemplate.setParent(8L);
        roleTemplate.getRAttrTemplates().add("show");
        roleTemplate.getAttrs().add(attributeTO("show", "'true'"));

        task.setRoleTemplate(roleTemplate);

        taskService.update(task.getId(), task);
        SyncTaskTO actual = taskService.read(task.getId());
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(roleTemplate, actual.getRoleTemplate());
        assertEquals(userTemplate, actual.getUserTemplate());

        TaskExecTO execution = execSyncTask(actual.getId(), 20, false);

        // 1. verify execution status
        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        // 2. verify that synchronized role is found, with expected attributes
        final PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query());
        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.getResult().size());

        final PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("syncFromLDAP").query());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());

        // Check for SYNCOPE-436
        assertEquals("syncFromLDAP", matchingUsers.getResult().get(0).getVirAttrMap().
                get("virtualReadOnly").getValues().get(0));

        final RoleTO roleTO = matchingRoles.getResult().iterator().next();
        assertNotNull(roleTO);
        assertEquals("testLDAPGroup", roleTO.getName());
        assertEquals(8L, roleTO.getParent());
        assertEquals("true", roleTO.getAttrMap().get("show").getValues().get(0));
        assertEquals(matchingUsers.getResult().iterator().next().getId(), (long) roleTO.getUserOwner());
        assertNull(roleTO.getRoleOwner());

        // 3. verify that LDAP group membership is propagated as Syncope role membership
        final PagedResult<UserTO> members = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().hasRoles(roleTO.getId()).query());
        assertNotNull(members);
        assertEquals(1, members.getResult().size());
    }

    @Test
    public void issue196() {
        TaskExecTO exec = taskService.execute(6L, false);
        assertNotNull(exec);
        assertEquals(0, exec.getId());
        assertNotNull(exec.getTask());
    }

    @Test
    public void dryRun() {
        TaskExecTO execution = execSyncTask(SYNC_TASK_ID, 50, true);
        assertEquals("Execution of task " + execution.getTask() + " failed with message " + execution.getMessage(),
                "SUCCESS", execution.getStatus());
    }

    @Test
    public void issueSYNCOPE81() {
        String sender = "syncope81@syncope.apache.org";
        createNotificationTask(sender);
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);

        assertTrue(taskTO.getExecutions().isEmpty());

        // generate an execution in order to verify the deletion of a notification task with one or more executions
        TaskExecTO execution = taskService.execute(taskTO.getId(), false);
        assertEquals("NOT_SENT", execution.getStatus());

        int i = 0;
        int maxit = 50;
        int executions = 0;

        // wait for task exec completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = taskService.read(taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (executions == taskTO.getExecutions().size() && i < maxit);

        assertFalse(taskTO.getExecutions().isEmpty());

        taskService.delete(taskTO.getId());
    }

    @Test
    public void issueSYNCOPE86() {
        // 1. create notification task
        String sender = "syncope86@syncope.apache.org";
        createNotificationTask(sender);

        // 2. get NotificationTaskTO for user just created
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());

        try {
            // 3. execute the generated NotificationTask
            TaskExecTO execution = taskService.execute(taskTO.getId(), false);
            assertNotNull(execution);

            // 4. verify
            taskTO = taskService.read(taskTO.getId());
            assertNotNull(taskTO);
            assertEquals(1, taskTO.getExecutions().size());
        } finally {
            // Remove execution to make test re-runnable
            taskService.deleteExecution(taskTO.getExecutions().get(0).getId());
        }
    }

    private NotificationTaskTO findNotificationTaskBySender(final String sender) {
        PagedResult<NotificationTaskTO> tasks = taskService.list(TaskType.NOTIFICATION);
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());
        NotificationTaskTO taskTO = null;
        for (NotificationTaskTO task : tasks.getResult()) {
            if (sender.equals(task.getSender())) {
                taskTO = task;
            }
        }
        return taskTO;
    }

    private void createNotificationTask(final String sender) {
        // 1. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.FAILURES);
        notification.getEvents().add("[REST]:[UserController]:[]:[create]:[SUCCESS]");

        notification.setAbout(SyncopeClient.getUserSearchConditionBuilder().hasRoles(7L).query());

        notification.setRecipients(SyncopeClient.getUserSearchConditionBuilder().hasRoles(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserSchema);

        notification.setSender(sender);
        String subject = "Test notification";
        notification.setSubject(subject);
        notification.setTemplate("optin");

        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        // 2. create user
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE68() {
        //-----------------------------
        // Create a new user ... it should be updated applying sync policy
        //-----------------------------
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername("testuser2");

        userTO.getAttrs().add(attributeTO("firstname", "testuser2"));
        userTO.getAttrs().add(attributeTO("surname", "testuser2"));
        userTO.getAttrs().add(attributeTO("type", "a type"));
        userTO.getAttrs().add(attributeTO("fullname", "a type"));
        userTO.getAttrs().add(attributeTO("userId", "testuser2@syncope.apache.org"));
        userTO.getAttrs().add(attributeTO("email", "testuser2@syncope.apache.org"));

        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION2);
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION4);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals("testuser2", userTO.getUsername());
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(3, userTO.getResources().size());
        //-----------------------------

        try {
            //-----------------------------
            //  add user template
            //-----------------------------
            UserTO template = new UserTO();

            membershipTO = new MembershipTO();
            membershipTO.setRoleId(10L);

            template.getMemberships().add(membershipTO);

            template.getResources().add(RESOURCE_NAME_NOPROPAGATION4);
            //-----------------------------

            // Update sync task
            SyncTaskTO task = taskService.read(9L);
            assertNotNull(task);

            task.setUserTemplate(template);

            taskService.update(task.getId(), task);
            SyncTaskTO actual = taskService.read(task.getId());
            assertNotNull(actual);
            assertEquals(task.getId(), actual.getId());
            assertFalse(actual.getUserTemplate().getResources().isEmpty());
            assertFalse(actual.getUserTemplate().getMemberships().isEmpty());

            TaskExecTO execution = execSyncTask(actual.getId(), 50, false);
            final String status = execution.getStatus();
            assertNotNull(status);
            assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

            userTO = readUser("testuser2");
            assertNotNull(userTO);
            assertEquals("testuser2@syncope.apache.org", userTO.getAttrMap().get("userId").getValues().get(0));
            assertEquals(2, userTO.getMemberships().size());
            assertEquals(4, userTO.getResources().size());
        } finally {
            UserTO dUserTO = deleteUser(userTO.getId());
            assertNotNull(dUserTO);
        }
    }

    @Test
    public void issueSYNCOPE144() {
        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobClassName(SyncJob.class.getName());

        Response response = taskService.create(task);
        SchedTaskTO actual = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        task = taskService.read(actual.getId());
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        response = taskService.create(task);
        actual = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }

    @Test
    public void issueSYNCOPE230() {
        // 1. read SyncTask for resource-db-sync (table TESTSYNC on external H2)
        execSyncTask(10L, 20, false);

        // 3. read e-mail address for user created by the SyncTask first execution
        UserTO userTO = readUser("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getAttrMap().get("email").getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTSYNC on external H2 by changing e-mail address
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("UPDATE TESTSYNC SET email='updatedSYNCOPE230@syncope.apache.org'");

        // 5. re-execute the SyncTask
        execSyncTask(10L, 20, false);

        // 6. verify that the e-mail was updated
        userTO = readUser("issuesyncope230");
        assertNotNull(userTO);
        email = userTO.getAttrMap().get("email").getValues().iterator().next();
        assertNotNull(email);
        assertEquals("updatedSYNCOPE230@syncope.apache.org", email);
    }

    private TaskExecTO execSyncTask(final Long taskId, final int maxWaitSeconds,
            final boolean dryRun) {

        AbstractTaskTO taskTO = taskService.read(taskId);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        int preSyncSize = taskTO.getExecutions().size();
        TaskExecTO execution = taskService.execute(taskTO.getId(), dryRun);
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = maxWaitSeconds;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            taskTO = taskService.read(taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (preSyncSize == taskTO.getExecutions().size() && i < maxit);
        if (i == maxit) {
            fail("Timeout when executing task " + taskId);
        }
        return taskTO.getExecutions().get(0);
    }

    @Test
    public void issueSYNCOPE272() {
        removeTestUsers();

        // create user with testdb resource
        UserTO userTO = UserTestITCase.getUniqueSampleTO("syncope272@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO);
        try {
            assertNotNull(userTO);
            assertEquals(1, userTO.getPropagationStatusTOs().size());
            assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());

            // update sync task
            SyncTaskTO task = taskService.read(SYNC_TASK_ID);
            assertNotNull(task);

            // add user template
            AttributeTO newAttrTO = new AttributeTO();
            newAttrTO.setSchema("firstname");
            newAttrTO.getValues().add("");

            UserTO template = new UserTO();
            template.getAttrs().add(newAttrTO);
            template.getAttrs().add(attributeTO("userId", "'test'"));
            template.getAttrs().add(attributeTO("fullname", "'test'"));
            template.getAttrs().add(attributeTO("surname", "'test'"));
            template.getResources().add(RESOURCE_NAME_TESTDB);

            task.setUserTemplate(template);

            taskService.update(task.getId(), task);
            SyncTaskTO actual = taskService.read(task.getId());
            assertNotNull(actual);
            assertEquals(task.getId(), actual.getId());

            TaskExecTO taskExecTO = execSyncTask(SYNC_TASK_ID, 50, false);
            assertNotNull(actual);
            assertEquals(task.getId(), actual.getId());

            assertNotNull(taskExecTO.getStatus());
            assertTrue(PropagationTaskExecStatus.valueOf(taskExecTO.getStatus()).isSuccessful());

            userTO = userService.read(userTO.getId());
            assertNotNull(userTO);
            assertNotNull(userTO.getAttrMap().get("firstname").getValues().get(0));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void issueSYNCOPE258() {
        // -----------------------------
        // Add a custom correlation rule
        // -----------------------------
        SyncPolicyTO policyTO = policyService.read(9L);
        policyTO.getSpecification().setUserJavaRule(TestSyncRule.class.getName());

        policyService.update(policyTO.getId(), policyTO);
        // -----------------------------

        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test Sync Rule");
        task.setResource(RESOURCE_NAME_WS2);
        task.setFullReconciliation(true);
        task.setPerformCreate(true);
        task.setPerformDelete(true);
        task.setPerformUpdate(true);

        Response response = taskService.create(task);
        SyncTaskTO actual = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        UserTO userTO = UserTestITCase.getUniqueSampleTO("s258_1@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        createUser(userTO);

        userTO = UserTestITCase.getUniqueSampleTO("s258_2@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO);

        // change email in order to unmatch the second user
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getAttrsToRemove().add("email");
        userMod.getAttrsToUpdate().add(attributeMod("email", "s258@apache.org"));

        userService.update(userMod.getId(), userMod);

        execSyncTask(actual.getId(), 50, false);

        SyncTaskTO executed = taskService.read(actual.getId());
        assertEquals(1, executed.getExecutions().size());

        // asser for just one match
        assertTrue(executed.getExecutions().get(0).getMessage().substring(0, 55) + "...",
                executed.getExecutions().get(0).getMessage().contains("[updated/failures]: 1/0"));
    }

    @Test
    public void issueSYNCOPE307() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("s307@apache.org");

        AttributeTO csvuserid = new AttributeTO();
        csvuserid.setSchema("csvuserid");
        userTO.getDerAttrs().add(csvuserid);

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);
        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        userTO = userService.read(userTO.getId());
        assertEquals("virtualvalue", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // Update sync task
        SyncTaskTO task = taskService.read(12L);
        assertNotNull(task);

        //  add user template
        UserTO template = new UserTO();
        template.getResources().add(RESOURCE_NAME_DBVIRATTR);

        AttributeTO userId = attributeTO("userId", "'s307@apache.org'");
        template.getAttrs().add(userId);

        AttributeTO email = attributeTO("email", "'s307@apache.org'");
        template.getAttrs().add(email);

        task.setUserTemplate(template);

        taskService.update(task.getId(), task);
        execSyncTask(task.getId(), 50, false);

        // check for sync policy
        userTO = userService.read(userTO.getId());
        assertEquals("virtualvalue", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        try {
            final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

            String value = jdbcTemplate.queryForObject(
                    "SELECT USERNAME FROM testsync WHERE ID=?", String.class, userTO.getId());
            assertEquals("virtualvalue", value);
        } catch (EmptyResultDataAccessException e) {
            assertTrue(false);
        }
    }

    @Test
    public void bulkAction() {
        final PagedResult<PropagationTaskTO> before = taskService.list(TaskType.PROPAGATION);

        // create user with testdb resource
        final UserTO userTO = UserTestITCase.getUniqueSampleTO("taskBulk@apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(userTO);

        final List<PropagationTaskTO> after = new ArrayList<PropagationTaskTO>(
                taskService.<PropagationTaskTO>list(TaskType.PROPAGATION).getResult());

        after.removeAll(before.getResult());

        assertFalse(after.isEmpty());

        final BulkAction bulkAction = new BulkAction();
        bulkAction.setOperation(BulkAction.Type.DELETE);

        for (AbstractTaskTO taskTO : after) {
            bulkAction.getTargets().add(String.valueOf(taskTO.getId()));
        }

        taskService.bulk(bulkAction);

        assertFalse(taskService.list(TaskType.PROPAGATION).getResult().containsAll(after));
    }
}
