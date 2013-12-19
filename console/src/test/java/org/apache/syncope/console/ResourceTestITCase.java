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

public class ResourceTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/div/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[3]/div/span/div/div/div/label[text()='Name']\");", "30000");

        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//td[6]/div/span[12]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[3]/div/span/div/div/div/label[text()='Name']\");", "30000");

        selenium.click("//li[2]/a");

        selenium.waitForCondition("selenium.isElementPresent(" + "\"//tbody/tr\");", "30000");

        selenium.click("//tbody/tr[2]/td/input");

        assertTrue(selenium.getConfirmation().equals("Do you really want to delete the selected item(s)?"));

        selenium.click("name=apply");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//tr[3]/td[6]/div/span[14]/a");

        assertTrue(selenium.getConfirmation().equals("Do you really want to delete the selected item(s)?"));
    }

    @Test
    public void checkSecurityTab() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//td[6]/div/span[12]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[3]/div/span/div/div/div/label[text()='Name']\");", "30000");

        selenium.click("//li[4]/a");

        assertTrue(selenium.isElementPresent("//label[@for='passwordPolicy']"));

        selenium.click("//li[1]/a");
        selenium.click("//li[2]/a");
        selenium.click("//li[3]/a");

        selenium.click("css=a.w_close");
    }

    @Test
    public void checkConnection() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//*[@id=\"users-contain\"]//*[div=\"ws-target-resource-delete\"]/../td[6]/div/span[12]/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//form/div[3]/div/span/div/div/div/label[text()='Name']\");", "30000");

        selenium.click("//li[3]/a");

        selenium.waitForCondition("selenium.isElementPresent(" + "\"//span[text()='endpoint']\");", "30000");

        selenium.click("//div[2]/form/div[3]/div[4]/span/span/div[2]/a");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/ul/li/span[contains(text(), 'Successful connection')]\");", "30000");
    }
}
