/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.updator;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * <h1>PMT Updater</h1>
 * After successful validation update the PMT with release state.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class PMTUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(PMTUpdater.class);
    private static Properties prop = new Properties();

    public static void sendRequest(String patchName, int state, boolean isSuccess, String adminTestPromote)
            throws IOException {

        String successState;
        if (isSuccess) {
            successState = "true";
        } else {
            successState = "false";
        }

        JSONObject json = new JSONObject();
        json.put("patchName", patchName);
        json.put("state", state);
        json.put("isSuccess", successState);
        json.put("AdminTestPromote", adminTestPromote);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(prop.getProperty("httpUri"));
            LOG.info(String.valueOf(request));
            StringEntity params = new StringEntity(json.toString());
            params.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, prop.getProperty("content-type")));
            request.addHeader("Authentication", prop.getProperty("Authentication"));
            request.addHeader("Cache-Control", prop.getProperty("Cache-Control"));
            request.setEntity(params);
            httpClient.execute(request);
        } catch (Exception ex) {
            LOG.error("Error at sending Request", ex);
        }
    }
}
