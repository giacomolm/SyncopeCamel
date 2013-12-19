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
package org.apache.syncope.console.pages;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.HttpResourceStream;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.pages.panels.PoliciesPanel;
import org.apache.syncope.console.rest.ConfigurationRestClient;
import org.apache.syncope.console.rest.LoggerRestClient;
import org.apache.syncope.console.rest.NotificationRestClient;
import org.apache.syncope.console.rest.WorkflowRestClient;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.console.wicket.markup.html.link.VeilPopupSettings;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Configurations WebPage.
 */
public class Configuration extends BasePage {

    private static final long serialVersionUID = -2838270869037702214L;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SpringBean
    private LoggerRestClient loggerRestClient;

    @SpringBean
    private NotificationRestClient notificationRestClient;

    @SpringBean
    private WorkflowRestClient wfRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createConfigWin;

    private final ModalWindow editConfigWin;

    private static final int CONFIG_WIN_HEIGHT = 200;

    private static final int CONFIG_WIN_WIDTH = 350;

    private final ModalWindow createNotificationWin;

    private final ModalWindow editNotificationWin;

    private static final int NOTIFICATION_WIN_HEIGHT = 500;

    private static final int NOTIFICATION_WIN_WIDTH = 1100;

    private WebMarkupContainer confContainer;

    private WebMarkupContainer notificationContainer;

    private int confPaginatorRows;

    private int notificationPaginatorRows;

    public Configuration() {
        super();

        add(createConfigWin = new ModalWindow("createConfigurationWin"));
        add(editConfigWin = new ModalWindow("editConfigurationWin"));
        setupSyncopeConf();

        add(new PoliciesPanel("passwordPoliciesPanel", getPageReference(), PolicyType.PASSWORD));
        add(new PoliciesPanel("accountPoliciesPanel", getPageReference(), PolicyType.ACCOUNT));
        add(new PoliciesPanel("syncPoliciesPanel", getPageReference(), PolicyType.SYNC));

        add(createNotificationWin = new ModalWindow("createNotificationWin"));
        add(editNotificationWin = new ModalWindow("editNotificationWin"));
        setupNotification();

        // Workflow definition stuff
        WebMarkupContainer noActivitiEnabledForUsers = new WebMarkupContainer("noActivitiEnabledForUsers");
        noActivitiEnabledForUsers.setOutputMarkupPlaceholderTag(true);
        add(noActivitiEnabledForUsers);

        WebMarkupContainer workflowDefContainer = new WebMarkupContainer("workflowDefContainer");
        workflowDefContainer.setOutputMarkupPlaceholderTag(true);

        if (wfRestClient.isActivitiEnabledForUsers()) {
            noActivitiEnabledForUsers.setVisible(false);
        } else {
            workflowDefContainer.setVisible(false);
        }

        BookmarkablePageLink<Void> activitiModeler =
                new BookmarkablePageLink<Void>("activitiModeler", ActivitiModelerPopupPage.class);
        activitiModeler.setPopupSettings(new VeilPopupSettings().setHeight(600).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(activitiModeler, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "workflowDefRead"));
        workflowDefContainer.add(activitiModeler);
        // Check if Activiti Modeler directory is found
        boolean activitiModelerEnabled = false;
        try {
            String activitiModelerDirectory = WebApplicationContextUtils.getWebApplicationContext(
                    WebApplication.get().getServletContext()).getBean("activitiModelerDirectory", String.class);
            File baseDir = new File(activitiModelerDirectory);
            activitiModelerEnabled = baseDir.exists() && baseDir.canRead() && baseDir.isDirectory();
        } catch (Exception e) {
            LOG.error("Could not check for Activiti Modeler directory", e);
        }
        activitiModeler.setEnabled(activitiModelerEnabled);

        BookmarkablePageLink<Void> xmlEditor =
                new BookmarkablePageLink<Void>("xmlEditor", XMLEditorPopupPage.class);
        xmlEditor.setPopupSettings(new VeilPopupSettings().setHeight(350).setWidth(800));
        MetaDataRoleAuthorizationStrategy.authorize(xmlEditor, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "workflowDefRead"));
        workflowDefContainer.add(xmlEditor);

        Image workflowDefDiagram = new Image("workflowDefDiagram", new Model()) {

            private static final long serialVersionUID = -8457850449086490660L;

            @Override
            protected IResource getImageResource() {
                return new DynamicImageResource() {

                    private static final long serialVersionUID = 923201517955737928L;

                    @Override
                    protected byte[] getImageData(final IResource.Attributes attributes) {
                        return wfRestClient.isActivitiEnabledForUsers()
                                ? wfRestClient.getDiagram()
                                : new byte[0];
                    }
                };
            }

        };
        workflowDefContainer.add(workflowDefDiagram);

        MetaDataRoleAuthorizationStrategy.authorize(workflowDefContainer, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Configuration", "workflowDefRead"));
        add(workflowDefContainer);

        // Logger stuff
        PropertyListView<LoggerTO> coreLoggerList =
                new LoggerPropertyList(null, "corelogger", loggerRestClient.listLogs());
        WebMarkupContainer coreLoggerContainer = new WebMarkupContainer("coreLoggerContainer");
        coreLoggerContainer.add(coreLoggerList);
        coreLoggerContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(coreLoggerContainer, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "logList"));
        add(coreLoggerContainer);

        ConsoleLoggerController consoleLoggerController = new ConsoleLoggerController();
        PropertyListView<LoggerTO> consoleLoggerList =
                new LoggerPropertyList(consoleLoggerController, "consolelogger", consoleLoggerController.getLoggers());
        WebMarkupContainer consoleLoggerContainer = new WebMarkupContainer("consoleLoggerContainer");
        consoleLoggerContainer.add(consoleLoggerList);
        consoleLoggerContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(consoleLoggerContainer, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "logList"));
        add(consoleLoggerContainer);
    }

    private void setupSyncopeConf() {
        confPaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_CONFIGURATION_PAGINATOR_ROWS);

        final List<IColumn<ConfigurationTO, String>> confColumns = new ArrayList<IColumn<ConfigurationTO, String>>();
        confColumns.add(new PropertyColumn<ConfigurationTO, String>(new ResourceModel("key"), "key", "key"));
        confColumns.add(new PropertyColumn<ConfigurationTO, String>(new ResourceModel("value"), "value", "value"));

        confColumns.add(new AbstractColumn<ConfigurationTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<ConfigurationTO>> cellItem, final String componentId,
                    final IModel<ConfigurationTO> model) {

                final ConfigurationTO configurationTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editConfigWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ConfigurationModalPage(Configuration.this.getPageReference(), editConfigWin,
                                        configurationTO, false);
                            }
                        });

                        editConfigWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Configuration");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            confRestClient.deleteConfiguration(configurationTO.getKey());
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting a conf key", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        target.add(feedbackPanel);

                        target.add(confContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Configuration");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<ConfigurationTO, String> confTable =
                new AjaxFallbackDefaultDataTable<ConfigurationTO, String>(
                        "syncopeconf", confColumns, new SyncopeConfProvider(), confPaginatorRows);

        confContainer = new WebMarkupContainer("confContainer");
        confContainer.add(confTable);
        confContainer.setOutputMarkupId(true);

        add(confContainer);

        createConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConfigWin.setInitialHeight(CONFIG_WIN_HEIGHT);
        createConfigWin.setInitialWidth(CONFIG_WIN_WIDTH);
        createConfigWin.setCookieName("create-configuration-modal");

        editConfigWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConfigWin.setInitialHeight(CONFIG_WIN_HEIGHT);
        editConfigWin.setInitialWidth(CONFIG_WIN_WIDTH);
        editConfigWin.setCookieName("edit-configuration-modal");

        setWindowClosedCallback(createConfigWin, confContainer);
        setWindowClosedCallback(editConfigWin, confContainer);

        AjaxLink createConfigurationLink = new AjaxLink("createConfigurationLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                createConfigWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new ConfigurationModalPage(Configuration.this.getPageReference(), createConfigWin,
                                new ConfigurationTO(), true);
                    }
                });

                createConfigWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createConfigurationLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "create"));
        add(createConfigurationLink);

        Link dbExportLink = new Link<Void>("dbExportLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    HttpResourceStream stream = new HttpResourceStream(confRestClient.dbExport());

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(stream.getFilename() == null ? "content.xml" : stream.getFilename());
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(dbExportLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Configuration", "read"));
        add(dbExportLink);

        Form confPaginatorForm = new Form("confPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this,
                "confPaginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_CONFIGURATION_PAGINATOR_ROWS, String.valueOf(
                        confPaginatorRows));
                confTable.setItemsPerPage(confPaginatorRows);

                target.add(confContainer);
            }
        });

        confPaginatorForm.add(rowsChooser);
        add(confPaginatorForm);
    }

    private void setupNotification() {
        notificationPaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_NOTIFICATION_PAGINATOR_ROWS);

        final List<IColumn<NotificationTO, String>> notificationCols = new ArrayList<IColumn<NotificationTO, String>>();
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("id"), "id", "id"));
        notificationCols.add(new CollectionPropertyColumn<NotificationTO>(
                new ResourceModel("events"), "events", "events"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("subject"), "subject", "subject"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("template"), "template", "template"));
        notificationCols.add(new PropertyColumn<NotificationTO, String>(
                new ResourceModel("traceLevel"), "traceLevel", "traceLevel"));

        notificationCols.add(new AbstractColumn<NotificationTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<NotificationTO>> cellItem, final String componentId,
                    final IModel<NotificationTO> model) {

                final NotificationTO notificationTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editNotificationWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new NotificationModalPage(Configuration.this.getPageReference(),
                                        editNotificationWin, notificationTO, false);
                            }
                        });

                        editNotificationWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Notification");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            notificationRestClient.deleteNotification(notificationTO.getId());
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting a notification", e);
                            error(e.getMessage());
                            return;
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        target.add(feedbackPanel);

                        target.add(notificationContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Notification");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<NotificationTO, String> notificationTable =
                new AjaxFallbackDefaultDataTable<NotificationTO, String>(
                        "notificationTable", notificationCols, new NotificationProvider(), notificationPaginatorRows);

        notificationContainer = new WebMarkupContainer("notificationContainer");
        notificationContainer.add(notificationTable);
        notificationContainer.setOutputMarkupId(true);

        add(notificationContainer);

        createNotificationWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createNotificationWin.setInitialHeight(NOTIFICATION_WIN_HEIGHT);
        createNotificationWin.setInitialWidth(NOTIFICATION_WIN_WIDTH);
        createNotificationWin.setCookieName("create-notification-modal");

        editNotificationWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editNotificationWin.setInitialHeight(NOTIFICATION_WIN_HEIGHT);
        editNotificationWin.setInitialWidth(NOTIFICATION_WIN_WIDTH);
        editNotificationWin.setCookieName("edit-notification-modal");

        setWindowClosedCallback(createNotificationWin, notificationContainer);
        setWindowClosedCallback(editNotificationWin, notificationContainer);

        AjaxLink createNotificationLink = new AjaxLink("createNotificationLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                createNotificationWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new NotificationModalPage(Configuration.this.getPageReference(), createNotificationWin,
                                new NotificationTO(), true);
                    }
                });

                createNotificationWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createNotificationLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Notification", "create"));
        add(createNotificationLink);

        Form notificationPaginatorForm = new Form("notificationPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this,
                "notificationPaginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_NOTIFICATION_PAGINATOR_ROWS, String.valueOf(
                        notificationPaginatorRows));
                notificationTable.setItemsPerPage(notificationPaginatorRows);

                target.add(notificationContainer);
            }
        });

        notificationPaginatorForm.add(rowsChooser);
        add(notificationPaginatorForm);
    }

    private class SyncopeConfProvider extends SortableDataProvider<ConfigurationTO, String> {

        private static final long serialVersionUID = -276043813563988590L;

        private SortableDataProviderComparator<ConfigurationTO> comparator;

        public SyncopeConfProvider() {
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<ConfigurationTO>(this);
        }

        @Override
        public Iterator<ConfigurationTO> iterator(final long first, final long count) {
            List<ConfigurationTO> list = confRestClient.getAllConfigurations();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return confRestClient.getAllConfigurations().size();
        }

        @Override
        public IModel<ConfigurationTO> model(final ConfigurationTO configuration) {

            return new AbstractReadOnlyModel<ConfigurationTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public ConfigurationTO getObject() {
                    return configuration;
                }
            };
        }
    }

    private class NotificationProvider extends SortableDataProvider<NotificationTO, String> {

        private static final long serialVersionUID = -276043813563988590L;

        private SortableDataProviderComparator<NotificationTO> comparator;

        public NotificationProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<NotificationTO>(this);
        }

        @Override
        public Iterator<NotificationTO> iterator(final long first, final long count) {
            List<NotificationTO> list = notificationRestClient.getAllNotifications();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return notificationRestClient.getAllNotifications().size();
        }

        @Override
        public IModel<NotificationTO> model(final NotificationTO notification) {

            return new AbstractReadOnlyModel<NotificationTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public NotificationTO getObject() {
                    return notification;
                }
            };
        }
    }

    private class LoggerPropertyList extends PropertyListView<LoggerTO> {

        private static final long serialVersionUID = 5911412425994616111L;

        private final ConsoleLoggerController consoleLoggerController;

        public LoggerPropertyList(final ConsoleLoggerController consoleLoggerController, final String id,
                final List<? extends LoggerTO> list) {

            super(id, list);
            this.consoleLoggerController = consoleLoggerController;
        }

        @Override
        protected void populateItem(final ListItem<LoggerTO> item) {
            item.add(new Label("name"));

            DropDownChoice<LoggerLevel> level = new DropDownChoice<LoggerLevel>("level");
            level.setModel(new IModel<LoggerLevel>() {

                private static final long serialVersionUID = -2350428186089596562L;

                @Override
                public LoggerLevel getObject() {
                    return item.getModelObject().getLevel();
                }

                @Override
                public void setObject(final LoggerLevel object) {
                    item.getModelObject().setLevel(object);
                }

                @Override
                public void detach() {
                }
            });
            level.setChoices(Arrays.asList(LoggerLevel.values()));
            level.setOutputMarkupId(true);
            level.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    try {
                        if (getId().equals("corelogger")) {
                            loggerRestClient.setLogLevel(item.getModelObject().getName(),
                                    item.getModelObject().getLevel());
                        } else {
                            consoleLoggerController.setLogLevel(item.getModelObject().getName(),
                                    item.getModelObject().getLevel());
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                    } catch (SyncopeClientException e) {
                        info(getString(Constants.OPERATION_ERROR));
                    }

                    target.add(feedbackPanel);
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(level, ENABLE, xmlRolesReader.getAllAllowedRoles(
                    "Configuration", "logSetLevel"));

            item.add(level);
        }
    }

    private static class ConsoleLoggerController implements Serializable {

        private static final long serialVersionUID = -1550459341476431714L;

        public List<LoggerTO> getLoggers() {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            List<LoggerTO> result = new ArrayList<LoggerTO>();
            for (LoggerConfig logger : ctx.getConfiguration().getLoggers().values()) {
                final String loggerName = LogManager.ROOT_LOGGER_NAME.equals(logger.getName())
                        ? SyncopeConstants.ROOT_LOGGER : logger.getName();
                if (logger.getLevel() != null) {
                    LoggerTO loggerTO = new LoggerTO();
                    loggerTO.setName(loggerName);
                    loggerTO.setLevel(LoggerLevel.fromLevel(logger.getLevel()));
                    result.add(loggerTO);
                }
            }

            return result;
        }

        public void setLogLevel(final String name, final LoggerLevel level) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            LoggerConfig logConf = SyncopeConstants.ROOT_LOGGER.equals(name)
                    ? ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
                    : ctx.getConfiguration().getLoggerConfig(name);
            logConf.setLevel(level.getLevel());
            ctx.updateLoggers();
        }
    }
}
