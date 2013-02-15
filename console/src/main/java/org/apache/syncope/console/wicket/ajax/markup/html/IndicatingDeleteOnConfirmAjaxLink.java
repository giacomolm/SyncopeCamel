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
package org.apache.syncope.console.wicket.ajax.markup.html;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.model.IModel;

public abstract class IndicatingDeleteOnConfirmAjaxLink<T> extends ClearIndicatingAjaxLink<T> {

    private static final long serialVersionUID = 2228670850922265663L;

    public IndicatingDeleteOnConfirmAjaxLink(final String id, final PageReference pageRef) {
        super(id, pageRef);
    }

    public IndicatingDeleteOnConfirmAjaxLink(final String id, final IModel<T> model, final PageReference pageRef) {
        super(id, model, pageRef);
    }

    @Override
    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

            private static final long serialVersionUID = -7927968187160354605L;

            @Override
            public CharSequence preDecorateScript(final CharSequence script) {
                return "if (confirm('" + getString("confirmDelete") + "'))" + "{" + script + "}";
            }
        };
    }
}
