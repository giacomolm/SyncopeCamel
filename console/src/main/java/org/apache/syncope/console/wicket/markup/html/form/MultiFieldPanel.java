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
package org.apache.syncope.console.wicket.markup.html.form;

import java.util.List;
import org.apache.syncope.console.commons.Constants;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

public class MultiFieldPanel<E> extends AbstractFieldPanel<List<E>> {

    private static final long serialVersionUID = -6322397761456513324L;

    private ListView<E> view;

    private WebMarkupContainer container;

    public MultiFieldPanel(final String id, final IModel<List<E>> model, final FieldPanel<E> panelTemplate) {
        this(id, model, panelTemplate, false);
    }

    public MultiFieldPanel(final String id, final IModel<List<E>> model, final FieldPanel<E> panelTemplate,
            final boolean eventTemplate) {

        super(id, model);

        // -----------------------
        // Object container definition
        // -----------------------
        container = new WebMarkupContainer("multiValueContainer");
        container.setOutputMarkupId(true);
        add(container);
        // -----------------------

        view = new ListView<E>("view", model) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<E> item) {
                final FieldPanel<E> fieldPanel = panelTemplate.clone();

                if (eventTemplate) {
                    fieldPanel.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = -1107858522700306810L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            send(getPage(), Broadcast.BREADTH, new MultiValueSelectorEvent(target));
                        }
                    });
                }

                fieldPanel.setNewModel(item);
                item.add(fieldPanel);

                AjaxLink<Void> minus = new IndicatingAjaxLink<Void>("drop") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        //Drop current component
                        model.getObject().remove(item.getModelObject());
                        fieldPanel.getField().clearInput();
                        target.add(container);

                        if (eventTemplate) {
                            send(getPage(), Broadcast.BREADTH, new MultiValueSelectorEvent(target));
                        }
                    }
                };

                item.add(minus);

                if (model.getObject().size() <= 1) {
                    minus.setVisible(false);
                    minus.setEnabled(false);
                } else {
                    minus.setVisible(true);
                    minus.setEnabled(true);
                }

                final Fragment fragment;
                if (item.getIndex() == model.getObject().size() - 1) {
                    final AjaxLink<Void> plus = new IndicatingAjaxLink<Void>("add") {

                        private static final long serialVersionUID = -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            //Add current component
                            model.getObject().add(null);
                            target.add(container);
                        }
                    };

                    fragment = new Fragment("panelPlus", "fragmentPlus", container);

                    fragment.add(plus);
                } else {
                    fragment = new Fragment("panelPlus", "emptyFragment", container);
                }
                item.add(fragment);
            }
        };

        container.add(view.setOutputMarkupId(true));
        setOutputMarkupId(true);
    }

    public ListView<E> getView() {
        return view;
    }

    public WebMarkupContainer getContainer() {
        return container;
    }

    @Override
    public MultiFieldPanel<E> setModelObject(final List<E> object) {
        view.setModelObject(object);
        return this;
    }

    public static class MultiValueSelectorEvent {

        final AjaxRequestTarget target;

        public MultiValueSelectorEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
