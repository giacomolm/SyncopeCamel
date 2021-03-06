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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.rest.ApprovalRestClient;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Todo extends BasePage {

    private static final long serialVersionUID = -7122136682275797903L;

    @SpringBean
    private ApprovalRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow window;

    private static final int WIN_HEIGHT = 400;

    private static final int WIN_WIDTH = 600;

    private WebMarkupContainer container;

    private int paginatorRows;

    public Todo(final PageParameters parameters) {
        super(parameters);

        add(window = new ModalWindow("editApprovalWin"));

        container = new WebMarkupContainer("approvalContainer");

        MetaDataRoleAuthorizationStrategy.authorize(container, RENDER,
                xmlRolesReader.getAllAllowedRoles("Approval", "list"));

        paginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_TODO_PAGINATOR_ROWS);

        List<IColumn<WorkflowFormTO, String>> columns = new ArrayList<IColumn<WorkflowFormTO, String>>();
        columns.add(new PropertyColumn<WorkflowFormTO, String>(
                new ResourceModel("taskId"), "taskId", "taskId"));
        columns.add(new PropertyColumn<WorkflowFormTO, String>(
                new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<WorkflowFormTO, String>(
                new ResourceModel("description"), "description", "description"));
        columns.add(new DatePropertyColumn<WorkflowFormTO>(
                new ResourceModel("createTime"), "createTime", "createTime"));
        columns.add(new DatePropertyColumn<WorkflowFormTO>(
                new ResourceModel("dueDate"), "dueDate", "dueDate"));
        columns.add(new PropertyColumn<WorkflowFormTO, String>(new ResourceModel("owner"), "owner", "owner"));
        columns.add(new AbstractColumn<WorkflowFormTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<WorkflowFormTO>> cellItem, final String componentId,
                    final IModel<WorkflowFormTO> model) {

                final WorkflowFormTO formTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.claimForm(formTO.getTaskId());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException scee) {
                            error(getString(Constants.ERROR) + ": " + scee.getMessage());
                        }
                        target.add(feedbackPanel);
                        target.add(container);
                    }
                }, ActionLink.ActionType.CLAIM, "Approval");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ApprovalModalPage(Todo.this.getPageReference(), window, formTO);
                            }
                        });

                        window.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Approval",
                        SyncopeSession.get().getUsername().equals(formTO.getOwner()));

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<WorkflowFormTO, String> approvalTable =
                new AjaxFallbackDefaultDataTable<WorkflowFormTO, String>(
                        "approvalTable", columns, new ApprovalProvider(), paginatorRows);
        container.add(approvalTable);

        container.setOutputMarkupId(true);
        add(container);

        @SuppressWarnings("rawtypes")
        Form approvalPaginatorForm = new Form("paginatorForm");

        MetaDataRoleAuthorizationStrategy.authorize(approvalPaginatorForm, RENDER,
                xmlRolesReader.getAllAllowedRoles("Approval", "list"));

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_TODO_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                approvalTable.setItemsPerPage(paginatorRows);

                target.add(container);
            }
        });

        approvalPaginatorForm.add(rowsChooser);
        add(approvalPaginatorForm);

        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setCookieName("edit-approval-modal");

        setWindowClosedCallback(window, container);
    }

    private class ApprovalProvider extends SortableDataProvider<WorkflowFormTO, String> {

        private static final long serialVersionUID = -2311716167583335852L;

        private final SortableDataProviderComparator<WorkflowFormTO> comparator;

        public ApprovalProvider() {
            super();
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<WorkflowFormTO>(this);
        }

        @Override
        public Iterator<WorkflowFormTO> iterator(final long first, final long count) {

            final List<WorkflowFormTO> list = restClient.getForms();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getForms().size();
        }

        @Override
        public IModel<WorkflowFormTO> model(final WorkflowFormTO configuration) {

            return new AbstractReadOnlyModel<WorkflowFormTO>() {

                private static final long serialVersionUID = -2566070996511906708L;

                @Override
                public WorkflowFormTO getObject() {
                    return configuration;
                }
            };
        }
    }

}
