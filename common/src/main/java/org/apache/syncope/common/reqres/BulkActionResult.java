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
package org.apache.syncope.common.reqres;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement(name = "bulkActionResult")
@XmlType
public class BulkActionResult extends AbstractBaseBean {

    private static final long serialVersionUID = 2868894178821778133L;

    @XmlEnum
    @XmlType(name = "bulkActionStatus")
    public enum Status {

        // general bulk action result statuses
        SUCCESS,
        FAILURE,
        // specific propagation task execution statuses
        CREATED,
        SUBMITTED,
        UNSUBMITTED;

    }

    private final List<Result> results = new ArrayList<Result>();

    @XmlElementWrapper(name = "result")
    @XmlElement(name = "item")
    @JsonProperty("result")
    public List<Result> getResult() {
        return results;
    }

    @JsonIgnore
    public void add(final Object id, final Status status) {
        if (id != null) {
            results.add(new Result(id.toString(), status));
        }
    }

    @JsonIgnore
    public void add(final Object id, final String status) {
        if (id != null) {
            results.add(new Result(id.toString(), Status.valueOf(status.toUpperCase())));
        }
    }

    @JsonIgnore
    public Map<String, Status> getResultMap() {
        final Map<String, Status> res = new HashMap<String, Status>();

        for (Result result : results) {
            res.put(result.getKey(), result.getValue());
        }

        return res;
    }

    @JsonIgnore
    public List<String> getResultByStatus(final Status status) {
        final List<String> res = new ArrayList<String>();

        for (Result result : results) {
            if (result.getValue() == status) {
                res.add(result.getKey());
            }
        }

        return res;
    }

    public static class Result extends AbstractBaseBean {

        private static final long serialVersionUID = -1149681964161193232L;

        private String key;

        private Status value;

        public Result() {
            super();
        }

        public Result(final String key, final Status value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Status getValue() {
            return value;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public void setValue(final Status value) {
            this.value = value;
        }
    }
}
