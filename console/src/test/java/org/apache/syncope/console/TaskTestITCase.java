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
package org.apache.syncope.console;

import org.junit.Test;

public class TaskTestITCase extends AbstractTest {

    @Test
    public void execute() {
        selenium.click("css=img[alt=\"Tasks\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[4]/a");
        selenium.click("//*[div=1]/../td[10]/div/span[6]/a");

        selenium.waitForCondition("selenium.isTextPresent(" + "\"Operation executed successfully\");", "30000");

        selenium.click("//*[div=1]/../td[10]/div/span[12]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[2]/div/div/span/div/div/div[2]/span/input\");", "30000");

        assertTrue(selenium.isElementPresent("//form/div[2]/div[2]/span/table/tbody/tr[2]/td"));

        selenium.click("css=a.w_close");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Tasks\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//table/tbody/tr/td[8]/div/span[14]/a");

        assertTrue(selenium.getConfirmation().equals("Do you really want to delete the selected item(s)?"));

        selenium.waitForCondition("selenium.isTextPresent(\"Operation executed successfully\");", "30000");
    }

    @Test
    public void issueSYNCOPE148() {
        selenium.click("css=img[alt=\"Tasks\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//a[contains(text(),'Create new task')]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[2]/form/div[2]/div/div/span/div/div[2]/div/label\");", "30000");

        selenium.click("//div[2]/form/div[3]/input[2]");

        selenium.waitForCondition("selenium.isTextPresent(\"Id\");", "30000");
    }
}
