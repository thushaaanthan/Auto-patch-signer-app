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
package org.wso2.patchvalidator.constants;

/**
 * <h1>Constants</h1>
 * Constants for microservice.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public final class Constants {

    public static final String SUCCESS_STATE = "SUCCESS";
    public static final String SVN_CONNECTION_FAIL_STATE = "SVN_CONNECTION_FAILURE";
    public static final String VALIDATION_FAIL_STATE = "VALIDATION_FAILURE";
    public static final String COMMIT_KEYS_FAILURE = "Failure in committing keys to SVN";
    public static final String SUCCESSFULLY_SIGNED = "Patch successfully signed";
    public static final String SUCCESSFULLY_KEY_COMMITTED = "Keys successfully generated,committed and locked";
    public static final String SUCCESSFULLY_VALIDATED = "Patch validation successful";
    public static final String UPDATE_VALIDATED = "validation successfully finished.";
    public static final String PROCESSING = "IN_PROCESS";
    public static final String QUEUE = "IN_QUEUE";
    public static final String CONNECTION_SUCCESSFUL = "Connection Successful";

    private Constants() {
        // restrict instantiation
    }

}

