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
package org.apache.syncope.core.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.util.LoggerEventUtils;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UVirAttr;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.rest.data.SearchCondConverter;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.velocity.VelocityEngineUtils;

/**
 * Create notification tasks that will be executed by NotificationJob.
 *
 * @see NotificationTask
 */
@Transactional(rollbackFor = { Throwable.class })
public class NotificationManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * Notification DAO.
     */
    @Autowired
    private NotificationDAO notificationDAO;

    /**
     * Configuration DAO.
     */
    @Autowired
    private ConfDAO confDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Role DAO.
     */
    @Autowired
    private RoleDAO roleDAO;

    /**
     * User data binder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * User Search DAO.
     */
    @Autowired
    private AttributableSearchDAO searchDAO;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    /**
     * Velocity template engine.
     */
    @Autowired
    private VelocityEngine velocityEngine;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private ConnObjectUtil connObjectUtil;

    /**
     * Create a notification task.
     *
     * @param notification notification to take as model
     * @param attributable the user this task is about
     * @param model Velocity model
     * @return notification task, fully populated
     */
    private NotificationTask getNotificationTask(
            final Notification notification,
            final AbstractAttributable attributable,
            final Map<String, Object> model) {

        if (attributable != null) {
            connObjectUtil.retrieveVirAttrValues(attributable, AttributableUtil.getInstance(AttributableType.USER));
        }

        final List<SyncopeUser> recipients = new ArrayList<SyncopeUser>();

        if (notification.getRecipients() != null) {
            recipients.addAll(searchDAO.<SyncopeUser>search(EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                    SearchCondConverter.convert(notification.getRecipients()),
                    Collections.<OrderByClause>emptyList(), AttributableUtil.getInstance(AttributableType.USER)));
        }

        if (notification.isSelfAsRecipient() && attributable instanceof SyncopeUser) {
            recipients.add((SyncopeUser) attributable);
        }

        final Set<String> recipientEmails = new HashSet<String>();
        final List<UserTO> recipientTOs = new ArrayList<UserTO>(recipients.size());
        for (SyncopeUser recipient : recipients) {
            connObjectUtil.retrieveVirAttrValues(recipient, AttributableUtil.getInstance(AttributableType.USER));

            String email = getRecipientEmail(notification.getRecipientAttrType(),
                    notification.getRecipientAttrName(), recipient);
            if (email == null) {
                LOG.warn("{} cannot be notified: {} not found", recipient, notification.getRecipientAttrName());
            } else {
                recipientEmails.add(email);
                recipientTOs.add(userDataBinder.getUserTO(recipient));
            }
        }

        model.put("recipients", recipientTOs);
        model.put("syncopeConf", this.findAllSyncopeConfs());
        model.put("events", notification.getEvents());

        NotificationTask task = new NotificationTask();
        task.setTraceLevel(notification.getTraceLevel());
        task.setRecipients(recipientEmails);
        task.setSender(notification.getSender());
        task.setSubject(notification.getSubject());

        String htmlBody;
        String textBody;
        try {
            htmlBody = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "mailTemplates/"
                    + notification.getTemplate() + ".html.vm", SyncopeConstants.DEFAULT_ENCODING, model);
            textBody = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "mailTemplates/"
                    + notification.getTemplate() + ".txt.vm", SyncopeConstants.DEFAULT_ENCODING, model);
        } catch (VelocityException e) {
            LOG.error("Could not get mail body", e);

            htmlBody = "";
            textBody = "";
        }
        task.setTextBody(textBody);
        task.setHtmlBody(htmlBody);

        return task;
    }

    /**
     * Create notification tasks for each notification matching the given user id and (some of) tasks performed.
     */
    public void createTasks(
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final String event,
            final Result condition,
            final Object before,
            final Object output,
            final Object... input) {

        AttributableType attributableType = null;
        AbstractAttributable attributable = null;

        if (before instanceof UserTO) {
            attributableType = AttributableType.USER;
            attributable = userDAO.find(((UserTO) before).getId());
        } else if (output instanceof UserTO) {
            attributableType = AttributableType.USER;
            attributable = userDAO.find(((UserTO) output).getId());
        } else if (before instanceof RoleTO) {
            attributableType = AttributableType.ROLE;
            attributable = roleDAO.find(((RoleTO) before).getId());
        } else if (output instanceof RoleTO) {
            attributableType = AttributableType.ROLE;
            attributable = roleDAO.find(((RoleTO) output).getId());
        }

        LOG.debug("Search notification for [{}]{}", attributableType, attributable);

        for (Notification notification : notificationDAO.findAll()) {
            LOG.debug("Notification available about {}", notification.getAbout());

            final Set<String> events = new HashSet<String>(notification.getEvents());
            events.retainAll(Collections.<String>singleton(LoggerEventUtils.buildEvent(
                    type, category, subcategory, event, condition)));

            if (events.isEmpty()) {
                LOG.debug("No events found about {}", attributable);
            } else if (attributableType == null || attributable == null || notification.getAbout() == null
                    || searchDAO.matches(attributable,
                            SearchCondConverter.convert(notification.getAbout()),
                            AttributableUtil.getInstance(attributableType))) {

                LOG.debug("Creating notification task for events {} about {}", events, attributable);

                final Map<String, Object> model = new HashMap<String, Object>();
                model.put("type", type);
                model.put("category", category);
                model.put("subcategory", subcategory);
                model.put("event", event);
                model.put("condition", condition);
                model.put("before", before);
                model.put("output", output);
                model.put("input", input);

                if (attributable instanceof SyncopeUser) {
                    model.put("user", userDataBinder.getUserTO((SyncopeUser) attributable));
                }

                taskDAO.save(getNotificationTask(notification, attributable, model));
            }
        }
    }

    private String getRecipientEmail(
            final IntMappingType recipientAttrType, final String recipientAttrName, final SyncopeUser user) {

        String email = null;

        switch (recipientAttrType) {
            case Username:
                email = user.getUsername();
                break;

            case UserSchema:
                UAttr attr = user.getAttr(recipientAttrName);
                if (attr != null && !attr.getValuesAsStrings().isEmpty()) {
                    email = attr.getValuesAsStrings().get(0);
                }
                break;

            case UserVirtualSchema:
                UVirAttr virAttr = user.getVirAttr(recipientAttrName);
                if (virAttr != null && !virAttr.getValues().isEmpty()) {
                    email = virAttr.getValues().get(0);
                }
                break;

            case UserDerivedSchema:
                UDerAttr derAttr = user.getDerAttr(recipientAttrName);
                if (derAttr != null) {
                    email = derAttr.getValue(user.getAttrs());
                }
                break;

            default:
        }

        return email;
    }

    /**
     * Store execution of a NotificationTask.
     *
     * @param execution task execution.
     * @return merged task execution.
     */
    public TaskExec storeExec(final TaskExec execution) {
        NotificationTask task = taskDAO.find(execution.getTask().getId());
        task.addExec(execution);
        task.setExecuted(true);
        taskDAO.save(task);
        // this flush call is needed to generate a value for the execution id
        taskDAO.flush();
        return execution;
    }

    /**
     * Set execution state of NotificationTask with provided id.
     *
     * @param taskId task to be updated
     * @param executed execution state
     */
    public void setTaskExecuted(final Long taskId, final boolean executed) {
        NotificationTask task = taskDAO.find(taskId);
        task.setExecuted(executed);
        taskDAO.save(task);
    }

    /**
     * Count the number of task executions of a given task with a given status.
     *
     * @param taskId task id
     * @param status status
     * @return number of task executions
     */
    public long countExecutionsWithStatus(final Long taskId, final String status) {
        NotificationTask task = taskDAO.find(taskId);
        long count = 0;
        for (TaskExec taskExec : task.getExecs()) {
            if (status == null) {
                if (taskExec.getStatus() == null) {
                    count++;
                }
            } else if (status.equals(taskExec.getStatus())) {
                count++;
            }
        }
        return count;
    }

    protected Map<String, String> findAllSyncopeConfs() {
        Map<String, String> syncopeConfMap = new HashMap<String, String>();
        for (SyncopeConf conf : confDAO.findAll()) {
            syncopeConfMap.put(conf.getKey(), conf.getValue());
        }
        return syncopeConfMap;
    }
}
