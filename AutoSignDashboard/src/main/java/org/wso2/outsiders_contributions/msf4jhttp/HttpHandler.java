/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.outsiders_contributions.msf4jhttp;

import com.google.common.io.BaseEncoding;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;


/*
 * Handle get post request to backend
 */
public class HttpHandler {
    private static final Logger logger = Logger.getLogger(HttpHandler.class);
    private String backendPassword;
    private String backendUsername;
    private String backendUrl;
    private String backendServiceUrl;
    private static final PropertyReader propertyReader = new PropertyReader();


    public HttpHandler() {
        PropertyReader propertyReader = new PropertyReader();
        this.backendPassword = propertyReader.getBackendPassword();
        this.backendUsername = propertyReader.getBackendUsername();
        this.backendUrl = propertyReader.getBackendUrl();
        this.backendServiceUrl = propertyReader.getBackendServiceUrl();
    }

//    public String httpsGet(String url) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
//
//        InputStream file = Thread.currentThread().getContextClassLoader()
//                .getResourceAsStream(propertyReader.getTrustStoreFile());
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        keyStore.load(file, propertyReader.getTrustStorePassword().toCharArray());
//        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(keyStore,null).build();
//        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();   //comment this
//
//        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,allowAllHosts); //remove second parameter
//        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
//
//        HttpGet request = new HttpGet(this.backendUrl + url);
//        request.addHeader("Accept", "application/json");
//        String encodedCredentials = this.encode(this.backendUsername + ":" + this.backendPassword);
//        request.addHeader("Authorization", "Basic " + encodedCredentials);
//        String responseString = null;
//
//        try {
//            HttpResponse response = httpClient.execute(request);
//            if (logger.isDebugEnabled()) {
//                logger.debug("Request successful for " + url);
//            }
//            responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
//        } catch (IllegalStateException e) {
//            logger.error("The response is empty ");
//        } catch (NullPointerException e) {
//            logger.error("Bad request to the URL ");
//        } catch (IOException e) {
//            logger.error("mke");
//        }
//        return responseString;
//    }

//    public String get(String url) {
//        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
//        HttpGet request = new HttpGet(this.backendUrl + url);
//        request.addHeader("Accept", "application/json");
//        String encodedCredentials = this.encode(this.backendUsername + ":" + this.backendPassword);
//        request.addHeader("Authorization", "Basic " + encodedCredentials);
//        String responseString = null;
//
//        try {
//            HttpResponse response = httpClient.execute(request);
//            if (logger.isDebugEnabled()) {
//                logger.debug("Request successful for " + url);
//            }
//            responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
//        } catch (IllegalStateException e) {
//            logger.error("The response is empty");
//        } catch (NullPointerException e) {
//            logger.error("Bad request to the URL");
//        } catch (IOException e) {
//            logger.error("mke");
//        }
//
//        return responseString;
//    }

    public String httpsPost(String url, String object) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException {

        InputStream file = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(propertyReader.getTrustStoreFileService());
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(file, propertyReader.getTrustStorePasswordService().toCharArray());
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(keyStore, null).build();
        //when moving to staging server
        // 1. define allowAllHosts
        // 2. add allowAllHosts as second parameter
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();   //comment this
        //remove second parameter => allowAllHosts
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();

        HttpPost request = new HttpPost(this.backendServiceUrl + url);
        //request.addHeader("Accept", "application/json");
        String encodedCredentials = this.encode(this.backendUsername + ":" + this.backendPassword);
        request.addHeader("Authorization", "Basic " + encodedCredentials);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        String responseString = null;

        try {
            if (object != null) {
                StringEntity entity = new StringEntity(object);
                request.setEntity(entity);
                HttpResponse response = httpClient.execute(request);
                if (logger.isDebugEnabled()) {
                    logger.debug("Request successful for  " + url);
                }
                responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            } else {
                HttpResponse response = httpClient.execute(request);
                responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (IllegalStateException e) {
            logger.error("The response is empty " + String.valueOf(e));
            responseString = "exception 01: "     + String.valueOf(e);
        } catch (NullPointerException e) {
            logger.error("Bad request to the URL " + String.valueOf(e));
            responseString = "exception 02: " + String.valueOf(e);
        } catch (IOException e) {
            logger.error("mke: " + String.valueOf(e));
            responseString = "exception 03: " + String.valueOf(e);
        }
        return responseString;
    }


//    public String post(String url, String object) {
//        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
//        HttpPost request = new HttpPost(this.backendServiceUrl + url);
//        //request.addHeader("Accept", "application/x-www-form-urlencoded");
//        String encodedCredentials = this.encode(this.backendUsername + ":" + this.backendPassword);
//        //request.addHeader("Authorization", "Basic " + encodedCredentials);
//        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
//        String responseString = null;
//
//        try {
//            StringEntity entity = new StringEntity(object);
//            request.setEntity(entity);
//            HttpResponse response = httpClient.execute(request);
//            if (logger.isDebugEnabled()) {
//                logger.debug("Request successful for " + url);
//            }
//            responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
//
//        } catch (IllegalStateException e) {
//            logger.error("The response is empty ");
//        } catch (NullPointerException e) {
//            logger.error("Bad request to the URL");
//        } catch (IOException e) {
//            logger.error("The request was unsuccessful with dss");
//        }
//        return responseString;
//    }


    private String encode(String text) {
        String returnString = null;
        try {
            returnString = BaseEncoding.base64().encode(text.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return returnString;
    }

}
