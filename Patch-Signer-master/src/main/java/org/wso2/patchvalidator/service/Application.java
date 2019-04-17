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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.patchvalidator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.msf4j.MicroservicesRunner;
import org.wso2.patchvalidator.authentication.UsernamePasswordSecurityInterceptor;

/**
 * <h1>Auto Patch and Update Signer Microservice</h1>
 * The Auto Patch and update Signer implements an micro service that
 * validate,sign and revert wso2 patches and updates automatically.
 * <p>
 * This microservice listen requests from Auto Patch Signer
 * Application on wso2 internal app store.
 * Finally generate commit and lock keys for each patch and
 * update and send email to developer.
 *
 * @author Kosala Herath,Senthan Praanth
 * @version 1.2
 * @since 2017-12-14
 */
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        MicroservicesRunner microservicesRunner = new MicroservicesRunner();
        LOG.info("MICROSERVICE DEPLOYING...");
        microservicesRunner
                .addGlobalRequestInterceptor(new UsernamePasswordSecurityInterceptor())
                .deploy(new SyncService())
                .start();
    }
}



