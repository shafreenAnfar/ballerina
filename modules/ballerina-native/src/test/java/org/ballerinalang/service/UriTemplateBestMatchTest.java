/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.service;

import org.ballerinalang.model.BLangProgram;
import org.ballerinalang.model.values.BJSON;
import org.ballerinalang.testutils.EnvironmentInitializer;
import org.ballerinalang.testutils.MessageUtils;
import org.ballerinalang.testutils.Services;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * Test class for Uri Template based resource dispatchers.
 * Ex: /products/{productId}?regID={regID}
 */
public class UriTemplateBestMatchTest {

    private BLangProgram application;

    @BeforeClass()
    public void setup() {
        application = EnvironmentInitializer.setup("lang/service/uritemplate/uri-template-matching.bal");
    }

    @Test(description = "Test dispatching with URL. /hello/world/echo2?regid=abc")
    public void testMostSpecificMatchWithQueryParam() {
        String path = "/hello/world/echo2?regid=abc";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("echo1").asText(), "echo1"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/world/echo2/bar")
    public void testMostSpecificMatchWithWildCard() {
        String path = "/hello/world/echo2/bar";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("echo2").asText(), "echo2"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/world/echo2/foo/bar")
    public void testMostSpecificMatch() {
        String path = "/hello/world/echo2/foo/bar";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("echo3").asText(), "echo3"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/echo2?regid=abc")
    public void testMostSpecificServiceDispatch() {
        String path = "/hello/echo2?regid=abc";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("echo5").asText(), "echo5"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/echo2?regid=abc")
    public void testSubPathEndsWithPathParam() {
        String path = "/hello/echo2/shafreen";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("echo3").asText(), "shafreen"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/echo2/shafreen-anfar & /hello/echo2/shafreen+anfar")
    public void testMostSpecificWithPathParam() {
        String path = "/hello/echo2/shafreen-anfar";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("first").asText(), "shafreen"
                , "Resource dispatched to wrong template");

        Assert.assertEquals(bJson.value().get("second").asText(), "anfar"
                , "Resource dispatched to wrong template");

        path = "/hello/echo2/shafreen+anfar";
        cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        response = Services.invoke(cMsg);
        Assert.assertNotNull(response, "Response message not found");
        bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("first").asText(), "anfar"
                , "Resource dispatched to wrong template");

        Assert.assertEquals(bJson.value().get("second").asText(), "shafreen"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/echo2/shafreen+anfar/bar")
    public void testSubPathEndsWithBar() {
        String path = "/hello/echo2/shafreen+anfar/bar";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("first").asText(), "shafreen"
                , "Resource dispatched to wrong template");

        Assert.assertEquals(bJson.value().get("second").asText(), "anfar"
                , "Resource dispatched to wrong template");

        Assert.assertEquals(bJson.value().get("echo4").asText(), "echo4"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/echo2/shafreen+anfar/foo")
    public void testSubPathEndsWithFoo() {
        String path = "/hello/echo2/shafreen+anfar/foo";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("first").asText(), "shafreen"
                , "Resource dispatched to wrong template");

        Assert.assertEquals(bJson.value().get("second").asText(), "anfar"
                , "Resource dispatched to wrong template");

        Assert.assertEquals(bJson.value().get("echo4").asText(), "foo"
                , "Resource dispatched to wrong template");
    }

    @Test(description = "Test dispatching with URL. /hello/echo2/shafreen+anfar/foo/bar")
    public void testLeastSpecificURITemplate() {
        String path = "/hello/echo2/shafreen+anfar/foo/bar";
        CarbonMessage cMsg = MessageUtils.generateHTTPMessage(path, "GET");
        CarbonMessage response = Services.invoke(cMsg);

        Assert.assertNotNull(response, "Response message not found");
        BJSON bJson = ((BJSON) response.getMessageDataSource());

        Assert.assertEquals(bJson.value().get("echo5").asText(), "any"
                , "Resource dispatched to wrong template");
    }

    @AfterClass
    public void tearDown() {
        EnvironmentInitializer.cleanup(application);
    }
}
