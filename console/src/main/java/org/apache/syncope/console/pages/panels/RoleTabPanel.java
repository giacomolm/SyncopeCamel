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
package org.apache.syncope.console.pages.panels;

import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.ResultStatusModalPage;
import org.apache.syncope.console.pages.RoleModalPage;
import org.apache.syncope.console.pages.Roles;
import org.apache.syncope.console.pages.StatusModalPage;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RoleTabPanel extends Panel {

    private static final long serialVersionUID = 859236186975983959L;

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private UserRestClient userRestClient;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RoleTabPanel(final String id, final RoleTO selectedNode, final ModalWindow window,
            final PageReference pageRef) {

        super(id);

        this.add(new Label("displayName", selectedNode.getDisplayName()));

        final ActionLinksPanel links = new ActionLinksPanel("actionLinks", new Model(), pageRef);
        links.setOutputMarkupId(true);
        this.add(links);
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        RoleTO roleTO = new RoleTO();
                        roleTO.setParent(selectedNode.getId());
                        RoleModalPage form = new RoleModalPage(pageRef, window, roleTO);
                        return form;
                    }
                });

                window.show(target);
            }
        }, ActionLink.ActionType.CREATE, xmlRolesReader.getAllAllowedRoles("Roles", "create"));
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new StatusModalPage<RoleTO>(pageRef, window, roleRestClient.read(selectedNode.getId()));
                    }
                });

                window.show(target);
            }
        }, ActionLink.ActionType.MANAGE_RESOURCES, xmlRolesReader.getAllAllowedRoles("Roles", "update"));
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        RoleTO roleTO = roleRestClient.read(selectedNode.getId());
                        RoleModalPage form = new RoleModalPage(pageRef, window, roleTO);
                        return form;
                    }
                });

                window.show(target);
            }
        }, ActionLink.ActionType.EDIT, xmlRolesReader.getAllAllowedRoles("Roles", "update"));
        links.addWithRoles(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    final RoleTO roleTO = roleRestClient.delete(selectedNode.getId());

                    ((Roles) pageRef.getPage()).setModalResult(true);

                    window.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            return new ResultStatusModalPage.Builder(window, roleTO).build();
                        }
                    });

                    window.show(target);
                } catch (SyncopeClientException e) {
                    error(getString(Constants.OPERATION_ERROR) + ": " + e.getMessage());
                    target.add(((Roles) pageRef.getPage()).getFeedbackPanel());
                }
            }
        }, ActionLink.ActionType.DELETE, xmlRolesReader.getAllAllowedRoles("Roles", "delete"));

        final Form form = new Form("roleForm");
        form.setModel(new CompoundPropertyModel(selectedNode));
        form.setOutputMarkupId(true);

        final RolePanel rolePanel = new RolePanel.Builder("rolePanel").form(form).roleTO(selectedNode).
                roleModalPageMode(RoleModalPage.Mode.ADMIN).build();
        rolePanel.setEnabled(false);
        form.add(rolePanel);

        final WebMarkupContainer userListContainer = new WebMarkupContainer("userListContainer");

        userListContainer.setOutputMarkupId(true);
        userListContainer.setEnabled(true);
        userListContainer.add(new UserSearchResultPanel("userList", true, null, pageRef, userRestClient));
        userListContainer.add(new ClearIndicatingAjaxButton("search", new ResourceModel("search"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                userListContainer.replace(new UserSearchResultPanel("userList",
                        true,
                        SyncopeClient.getUserSearchConditionBuilder().hasRoles(selectedNode.getId()).query(),
                        pageRef,
                        userRestClient));

                target.add(userListContainer);
            }
        });

        form.add(userListContainer);
        add(form);
    }
}
