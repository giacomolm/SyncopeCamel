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

import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class FieldPanel<T> extends AbstractFieldPanel<T> implements Cloneable {

    private static final long serialVersionUID = -198988924922541273L;

    protected FormComponent<T> field = null;

    protected final String id;

    protected final String name;

    protected String title = null;

    protected boolean isRequiredLabelAdded = false;

    public FieldPanel(final String id, final String name, final IModel<T> model) {
        super(id, model);

        this.id = id;
        this.name = name;

        final Fragment fragment = new Fragment("required", "notRequiredFragment", this);
        add(fragment);

        setOutputMarkupId(true);
    }

    public FormComponent<T> getField() {
        return field;
    }

    public FieldPanel<T> setTitle(final String title) {
        this.title = title;
        field.add(AttributeModifier.replace("title", title != null
                ? title
                : ""));

        return this;
    }

    public FieldPanel<T> setStyleSheet(final String classes) {
        field.add(AttributeModifier.replace("class", classes != null
                ? classes
                : ""));

        return this;
    }

    public FieldPanel<T> setRequired(boolean required) {
        field.setRequired(required);

        return this;
    }

    public FieldPanel<T> setReadOnly(boolean readOnly) {
        field.setEnabled(!readOnly);

        return this;
    }

    public boolean isRequired() {
        return field.isRequired();
    }

    public boolean isReadOnly() {
        return !field.isEnabled();
    }

    public FieldPanel<T> addRequiredLabel() {
        if (!isRequired()) {
            setRequired(true);
        }

        final Fragment fragment = new Fragment("required", "requiredFragment", this);

        fragment.add(new Label("requiredLabel", "*"));

        replace(fragment);

        this.isRequiredLabelAdded = true;

        return this;
    }

    public FieldPanel<T> removeRequiredLabel() {
        if (isRequired()) {
            setRequired(false);
        }

        final Fragment fragment = new Fragment("required", "notRequiredFragment", this);

        replace(fragment);

        this.isRequiredLabelAdded = false;

        return this;
    }

    @Override
    public FieldPanel<T> setModelObject(T object) {
        field.setModelObject(object);
        return this;
    }

    public T getModelObject() {
        return (T) field.getModelObject();
    }

    public FieldPanel<T> setNewModel(final IModel<T> model) {
        field.setModel(model);
        return this;
    }

    /**
     * Used by MultiValueSelectorPanel to attach items.
     *
     * @param item item to attach.
     * @return updated FieldPanel object.
     */
    public FieldPanel<T> setNewModel(final ListItem<T> item) {
        setNewModel(new IModel<T>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public T getObject() {
                return item.getModelObject();
            }

            @Override
            public void setObject(final T object) {
                if (object != null && !object.toString().isEmpty()) {
                    item.setModelObject(object);
                }
            }

            @Override
            public void detach() {
                // no detach
            }
        });
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public FieldPanel<T> setNewModel(final List<Serializable> list) {
        setNewModel(new Model() {

            private static final long serialVersionUID = 1088212074765051906L;

            @Override
            public Serializable getObject() {
                return list == null || list.isEmpty()
                        ? null
                        : list.get(0);
            }

            @Override
            public void setObject(final Serializable object) {
                list.clear();

                if (object != null) {
                    list.add(object);
                }
            }
        });

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FieldPanel<T> clone() {
        final FieldPanel<T> panel = SerializationUtils.clone(this);
        panel.setModelObject(null);
        return panel;
    }
}
