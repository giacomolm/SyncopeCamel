/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.GenerationType;
import javax.persistence.Column;


@Entity
public class CamelRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="anId")
    private Long anId;

    private String name;

    @Lob
    private String routeContent;

    public Long getId() {
        return anId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getRouteContent() {
        return routeContent;
    }

    public void setRouteContent(String routeContent) {
        this.routeContent = routeContent;
    }

}
