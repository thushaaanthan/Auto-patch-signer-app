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

package org.wso2.patchvalidator.interfaces;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * TODO: Class level comment.
 */
public interface CommonValidator {

    String checkReadMe(String filepath, String patchId, String[] productNameArray) throws IOException, SQLException;

    String checkLicense(String filepath) throws IOException;

    String checkNotAContribution(String filepath) throws IOException;

    boolean checkPatch(String filepath, String patchId);

    void unZip(File zipFilepath, String destFilePath) throws IOException;

    String checkContent(String filePath, String patchId) throws IOException;

    String downloadZipFile(String url, String version, String patchId, String destFilePath);
}
