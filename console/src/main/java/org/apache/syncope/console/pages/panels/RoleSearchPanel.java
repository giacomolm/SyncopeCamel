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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.search.SyncopeFiqlSearchConditionBuilder;
import org.apache.syncope.common.types.AttributableType;
import org.apache.wicket.model.LoadableDetachableModel;

public class RoleSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 5757183539269316263L;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 6308997285778809579L;

        private String id;

        private String fiql = null;

        public Builder(final String id) {
            this.id = id;
        }

        public RoleSearchPanel.Builder fiql(final String fiql) {
            this.fiql = fiql;
            return this;
        }

        public RoleSearchPanel build() {
            return new RoleSearchPanel(this);
        }
    }

    private RoleSearchPanel(final Builder builder) {
        super(builder.id, AttributableType.ROLE, builder.fiql, true);
    }

    @Override
    protected void populate() {
        super.populate();

        this.types = new LoadableDetachableModel<List<SearchClause.Type>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<SearchClause.Type> load() {
                final List<SearchClause.Type> result = new ArrayList<SearchClause.Type>();
                result.add(SearchClause.Type.ATTRIBUTE);
                result.add(SearchClause.Type.ENTITLEMENT);
                result.add(SearchClause.Type.RESOURCE);
                return result;
            }
        };

        this.roleNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return Collections.<String>emptyList();
            }
        };
    }

    @Override
    protected SyncopeFiqlSearchConditionBuilder getSearchConditionBuilder() {
        return SyncopeClient.getRoleSearchConditionBuilder();
    }

}
