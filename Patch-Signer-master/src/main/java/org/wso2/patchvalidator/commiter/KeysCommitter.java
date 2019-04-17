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
package org.wso2.patchvalidator.commiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.updator.PMTUpdater;
import org.wso2.patchvalidator.validators.PatchValidator;
import org.wso2.patchvalidator.validators.PatchZipValidator;

import java.io.IOException;

/**
 * <h1>Keys Committer</h1>
 * After validation the patch ot update generate and commit keys for
 * update and patch to SVN repository. Also send request to the PMT
 * with release state.
 *
 * @author Kosala Herath, Senthan Prasanth
 * @version 1.3
 * @since 2017-12-14
 */
public class KeysCommitter {

    private static final Logger LOG = LoggerFactory.getLogger(PatchValidator.class);

    public static String validateKeysCommit(String patchUrl, String patchDestination) throws IOException {

        return PatchZipValidator.commitKeys(patchUrl, patchDestination);
    }

    public static void updatePMT(String patchName, int state) throws IOException {

        String patchStatus;
        if (state == 1) {
            patchStatus = "ReleasedNotInPublicSVN";
        } else if (state == 2) {
            patchStatus = "ReleasedNotAutomated";
        } else if (state == 3) {
            patchStatus = "Promote";
        } else {
            patchStatus = "Error in patch status";
        }
        LOG.info("patchStatus = " + patchStatus);
        PMTUpdater.sendRequest(patchName, state, true, patchStatus);
    }
}


