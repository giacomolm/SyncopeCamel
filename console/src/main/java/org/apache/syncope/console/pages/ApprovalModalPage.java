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

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.MapChoiceRenderer;
import org.apache.syncope.console.markup.html.list.AltListView;
import org.apache.syncope.console.rest.ApprovalRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ApprovalModalPage extends BaseModalPage {

    private static final long serialVersionUID = -8847854414429745216L;

    private final static int USER_WIN_HEIGHT = 550;

    private final static int USER_WIN_WIDTH = 800;

    @SpringBean
    private ApprovalRestClient restClient;

    private final ModalWindow editUserWin;

    public ApprovalModalPage(final PageReference pageRef, final ModalWindow window, final WorkflowFormTO formTO) {
        super();

        IModel<List<WorkflowFormPropertyTO>> formProps = new LoadableDetachableModel<List<WorkflowFormPropertyTO>>() {

            private static final long serialVersionUID = 3169142472626817508L;

            @Override
            protected List<WorkflowFormPropertyTO> load() {
                return formTO.getProperties();
            }
        };

        final ListView<WorkflowFormPropertyTO> propView =
                new AltListView<WorkflowFormPropertyTO>("propView", formProps) {

                    private static final long serialVersionUID = 9101744072914090143L;

                    @Override
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    protected void populateItem(final ListItem<WorkflowFormPropertyTO> item) {
                        final WorkflowFormPropertyTO prop = item.getModelObject();

                        Label label = new Label("key", prop.getName() == null
                                ? prop.getId()
                                : prop.getName());
                        item.add(label);

                        FieldPanel field;
                        switch (prop.getType()) {
                            case Boolean:
                                field = new AjaxDropDownChoicePanel("value", label.getDefaultModelObjectAsString(),
                                        new Model<Boolean>(Boolean.valueOf(prop.getValue()))).setChoices(Arrays.asList(
                                                new String[] { "Yes", "No" }));
                                break;

                            case Date:
                                SimpleDateFormat df = StringUtils.isNotBlank(prop.getDatePattern())
                                ? new SimpleDateFormat(prop.getDatePattern())
                                : new SimpleDateFormat();
                                Date parsedDate = null;
                                if (StringUtils.isNotBlank(prop.getValue())) {
                                    try {
                                        parsedDate = df.parse(prop.getValue());
                                    } catch (ParseException e) {
                                        LOG.error("Unparsable date: {}", prop.getValue(), e);
                                    }
                                }

                                field = new DateTimeFieldPanel("value", label.getDefaultModelObjectAsString(),
                                        new Model<Date>(parsedDate), df.toLocalizedPattern());
                                break;

                            case Enum:
                                MapChoiceRenderer<String, String> enumCR =
                                new MapChoiceRenderer<String, String>(prop.getEnumValues());

                                field = new AjaxDropDownChoicePanel("value", label.getDefaultModelObjectAsString(),
                                        new Model(prop.getValue())).setChoiceRenderer(enumCR).setChoices(new Model() {

                                    private static final long serialVersionUID = -858521070366432018L;

                                    @Override
                                    public Serializable getObject() {
                                        return new ArrayList<String>(prop.getEnumValues().keySet());
                                    }
                                });
                                break;

                            case Long:
                                field = new SpinnerFieldPanel<Long>("value", label.getDefaultModelObjectAsString(),
                                        Long.class, new Model<Long>(NumberUtils.toLong(prop.getValue())),
                                        null, null, false);
                                break;

                            case String:
                            default:
                                field = new AjaxTextFieldPanel("value", PARENT_PATH,
                                        new Model<String>(prop.getValue()));
                                break;
                        }

                        field.setReadOnly(!prop.isWritable());
                        if (prop.isRequired()) {
                            field.addRequiredLabel();
                        }

                        item.add(field);
                    }
                };

        final AjaxButton userDetails = new IndicatingAjaxButton("userDetails",
                new Model<String>(getString("userDetails"))) {

                    private static final long serialVersionUID = -4804368561204623354L;

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        editUserWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ViewUserModalPage(ApprovalModalPage.this.getPageReference(), editUserWin,
                                        userRestClient.read(formTO.getUserId())) {

                                    private static final long serialVersionUID = -2819994749866481607L;

                                    @Override
                                    protected void closeAction(final AjaxRequestTarget target, final Form form) {
                                        setResponsePage(ApprovalModalPage.this);
                                    }
                                };
                            }
                        });

                        editUserWin.show(target);
                    }
                };
        MetaDataRoleAuthorizationStrategy.authorize(userDetails, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Users", "read"));

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new Model<String>(getString(SUBMIT))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                Map<String, WorkflowFormPropertyTO> props = formTO.getPropertyMap();

                for (int i = 0; i < propView.size(); i++) {
                    @SuppressWarnings("unchecked")
                    ListItem<WorkflowFormPropertyTO> item = (ListItem<WorkflowFormPropertyTO>) propView.get(i);
                    String input = ((FieldPanel) item.get("value")).getField().getInput();

                    if (!props.containsKey(item.getModelObject().getId())) {
                        props.put(item.getModelObject().getId(), new WorkflowFormPropertyTO());
                    }

                    if (item.getModelObject().isWritable()) {
                        switch (item.getModelObject().getType()) {
                            case Boolean:
                                props.get(item.getModelObject().getId()).setValue(String.valueOf("0".equals(input)));
                                break;

                            case Date:
                            case Enum:
                            case String:
                            case Long:
                            default:
                                props.get(item.getModelObject().getId()).setValue(input);
                                break;
                        }
                    }
                }

                formTO.setProperties(props.values());
                try {
                    restClient.submitForm(formTO);

                    ((Todo) pageRef.getPage()).setModalResult(true);
                    window.close(target);
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    LOG.error("While submitting form {}", formTO, e);
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form form) {
                // nothing
            }
        };

        cancel.setDefaultFormProcessing(false);

        Form form = new Form(FORM);
        form.add(propView);
        form.add(userDetails);
        form.add(submit);
        form.add(cancel);

        MetaDataRoleAuthorizationStrategy.authorize(form, ENABLE, xmlRolesReader.getAllAllowedRoles("Approval",
                SUBMIT));

        editUserWin = new ModalWindow("editUserWin");
        editUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserWin.setInitialHeight(USER_WIN_HEIGHT);
        editUserWin.setInitialWidth(USER_WIN_WIDTH);
        editUserWin.setCookieName("edit-user-modal");
        add(editUserWin);

        add(form);
    }
}
