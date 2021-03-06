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
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.types.LoggerType;

@Entity
public class SyncopeLogger extends AbstractBaseBean {

    private static final long serialVersionUID = 943012777014416027L;

    @Id
    @Column(name = "logName")
    private String name;

    @Column(name = "logLevel", nullable = false)
    @Enumerated(EnumType.STRING)
    private LoggerLevel level;

    @Column(name = "logType", nullable = false)
    @Enumerated(EnumType.STRING)
    private LoggerType type;

    public LoggerLevel getLevel() {
        return level;
    }

    public void setLevel(final LoggerLevel level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public LoggerType getType() {
        return type;
    }

    public void setType(final LoggerType type) {
        this.type = type;
    }
}
