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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.util.StringUtils;

public class DateFieldPanel extends FieldPanel<Date> {

    private static final long serialVersionUID = -428975732068281726L;

    protected final String datePattern;

    protected DateFieldPanel(final String id, final String name, final IModel<Date> model, final String datePattern) {
        super(id, name, model);
        this.datePattern = datePattern;
    }

    @Override
    public FieldPanel<Date> setNewModel(final List<Serializable> list) {
        final SimpleDateFormat formatter = datePattern == null
                ? new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN, Locale.getDefault())
                : new SimpleDateFormat(datePattern, Locale.getDefault());

        setNewModel(new Model<Date>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public Date getObject() {
                Date date = null;

                if (list != null && !list.isEmpty() && StringUtils.hasText(list.get(0).toString())) {
                    try {
                        // Parse string using datePattern
                        date = formatter.parse(list.get(0).toString());
                    } catch (ParseException e) {
                        LOG.error("invalid parse exception", e);
                    }
                }

                return date;
            }

            @Override
            public void setObject(final Date object) {
                list.clear();
                if (object != null) {
                    list.add(formatter.format(object));
                }
            }
        });

        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FieldPanel<Date> setNewModel(final ListItem item) {
        final SimpleDateFormat formatter = datePattern == null
                ? new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN, Locale.getDefault())
                : new SimpleDateFormat(datePattern, Locale.getDefault());

        IModel<Date> model = new Model<Date>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Date getObject() {
                Date date = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        // Parse string using datePattern
                        try {
                            date = formatter.parse(obj.toString());
                        } catch (ParseException e) {
                            LOG.error("While parsing date", e);
                        }
                    } else if (obj instanceof Date) {
                        // Don't parse anything
                        date = (Date) obj;
                    } else {
                        // consider Long
                        date = new Date((Long) obj);
                    }
                }

                return date;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final Date object) {
                item.setModelObject(formatter.format(object));
            }
        };

        field.setModel(model);
        return this;
    }
}
