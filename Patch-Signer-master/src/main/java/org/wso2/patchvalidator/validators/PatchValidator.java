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

package org.wso2.patchvalidator.validators;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonValidator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Properties;

/**
 * <h1>Patch Validator</h1>
 * Validate patches considering all the file structure and content.
 *
 * @author Kosala Herath,Senthan Prasanth
 * @version 1.2
 * @since 2017-12-14
 */
public class PatchValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PatchValidator.class);
    public String patchUrl = "null";
    public String patchDestination = "null";
    public String patchName = "null";
    private Properties prop = new Properties();
    private Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public static PatchValidateFactory getPatchValidateFactory(String filepath) {

        if (filepath.endsWith(".zip")) {
            return new PatchValidateFactory();
        }
        return null;
    }

    public String zipPatchValidate(String patchId, String version, int state, int type, String product,
                                   String developedBy, String[] productNameArray) throws IOException {

        LOG.info("Patch Validation Service running...");
        prop.load(PatchValidator.class.getClassLoader().getResourceAsStream("application.properties"));

        String typeof = null;
        if (type == 1 || type == 3) {
            typeof = "patch";
        }

        ZipDownloadPath zipDownloadPath = new ZipDownloadPath(typeof, version, patchId);
        String filepath = zipDownloadPath.getFilepath();
        patchUrl = zipDownloadPath.getUrl();
        patchDestination = zipDownloadPath.getZipDownloadDestination();
        String destFilePath = zipDownloadPath.getDestFilePath();
        String unzippedFolderPath = zipDownloadPath.getUnzippedFolderPath();
        patchName = prop.getProperty("orgPatch") + version + "-" + patchId;

        String errorMessage = "";
        version = prop.getProperty(version);
        if (version == null) {
            return "Incorrect directory";
        }

        PatchValidateFactory patchValidateFactory = PatchValidator.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        String result = commonValidator.downloadZipFile(patchUrl, version, patchId, patchDestination);
        if (!Objects.equals(result, "")) {
            LOG.info(result);
            return result + "\n" + errorMessage;
        }

        File fl = new File(patchDestination);
        for (File file : fl.listFiles()) {
            if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
                    || file.getName().endsWith((".sha1"))) {
            /*
            todo: sendRequest()
            ("WSO2-CARBON-PATCH-4.0.0-0591","ReleasedNotInPublicSVN",true,"Promote");
            */
                errorMessage = "patch" + patchId + " is already signed\n";
                FileUtils.deleteDirectory(new File(destFilePath));
                LOG.info(errorMessage + "\n");
                return errorMessage + "\n";
            }
        }


//edited
      /*
        File f2 = new File(destFilePath);
        int fileCount = f2.list().length;
        if (fileCount >= 2) {
            LOG.info("There are some other files within this Zip file" + "\n");
            return "There are some other files within this Zip" + "\n";
        }
*/
//end

        try {
            LOG.info("Downloaded destination: " + patchDestination);
            LOG.info("Downloaded patch zip file: " + filepath);
            LOG.info("Unzipped destination: " + unzippedFolderPath);

            //unzip the patch in the temp directory
            commonValidator.unZip(new File(filepath), patchDestination);

            //validate patch from checking standards
            errorMessage = commonValidator.checkContent(unzippedFolderPath, patchId);
            errorMessage = errorMessage + commonValidator.checkLicense(unzippedFolderPath + "LICENSE.txt");
            errorMessage = errorMessage + commonValidator.checkNotAContribution(unzippedFolderPath +
                    "NOT_A_CONTRIBUTION.txt");
            if (!commonValidator.checkPatch(unzippedFolderPath +
                    "patch" + patchId + "/", patchId)) {
                errorMessage = errorMessage + "Patch directory is not available";
            }
            errorMessage = errorMessage + commonValidator.checkReadMe(unzippedFolderPath, patchId, productNameArray);

        } catch (IOException | SQLException ex) {
            LOG.error("unzipping failed", ex);
            errorMessage = errorMessage + "File unzipping failed\n";
        }


        if (Objects.equals(errorMessage, "")) {
            return Constants.SUCCESSFULLY_VALIDATED;
        } else {
            FileUtils.deleteDirectory(new File(destFilePath));  //delete downloaded files
            LOG.info(errorMessage + "\n");
            return errorMessage + "\n";
        }

    }

}

