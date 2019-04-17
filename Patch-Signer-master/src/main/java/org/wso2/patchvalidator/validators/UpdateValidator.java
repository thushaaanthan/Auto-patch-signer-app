/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.validators;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

/**
 * <h1>Update Validator</h1>
 * Validate updates considering all the file structure and content.
 *
 * @author Kosala Herath, Senthan Prasanth, Thushanthan Amalanathan
 * @version 1.2
 * @since 2017-12-14
 */
public class UpdateValidator {
    private WumUcResponse wumUcResponse = WumUcResponse.getInstance();
    private static final Logger LOG = LoggerFactory.getLogger(UpdateValidator.class);
    public String updateUrl = "null";
    public String updateDestination = "null";
    private Properties prop = new Properties();

    public String zipUpdateValidate(String updateId, String version, int type, String product)
            throws IOException, SQLException {

        LOG.info("Update Validation Service running");
        prop.load(UpdateValidator.class.getClassLoader().getResourceAsStream("application.properties"));


        String typeof = null;
        if (type == 2 || type == 3) {
            typeof = "update";
        }

        //define download svn url and destination directories and file names
        ZipDownloadPath zipDownloadPath = new ZipDownloadPath(typeof, version, updateId);

        String filepath = zipDownloadPath.getFilepath();
        updateUrl = zipDownloadPath.getUrl();
        updateDestination = zipDownloadPath.getZipDownloadDestination();
        String destFilePath = zipDownloadPath.getDestFilePath();

        String errorMessage = "";
        StringBuilder outMessage = new StringBuilder();
        version = prop.getProperty(version);
        if (version == null) {
            wumUcResponse.setExitCode(1);
            wumUcResponse.setResponse("Incorrect directory");
            return "Incorrect directory";
        }

        PatchValidateFactory patchValidateFactory = PatchValidator.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        //use commonValidator methods
        String result = commonValidator.downloadZipFile(updateUrl, version, updateId, updateDestination);
        if (!Objects.equals(result, "")) {
            LOG.info(result);
            wumUcResponse.setExitCode(1);
            wumUcResponse.setResponse(result + "\n" + errorMessage);
            return result + "\n" + errorMessage;
        }

        //check sign status of the update
        File fl = new File(updateDestination);
        for (File file : fl.listFiles()) {
            if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
                    || file.getName().endsWith((".sha1"))) {
                errorMessage = "update" + updateId + " is already signed\n";
                FileUtils.deleteDirectory(new File(destFilePath));
                LOG.info(errorMessage);
                wumUcResponse.setExitCode(1);
                wumUcResponse.setResponse(errorMessage);
                return errorMessage;
            }
        }

        //update validation using WUM validator
        String updateValidateScriptPath = prop.getProperty("updateValidateScriptPath");
        String productDownloadPath = prop.getProperty("productDestinationPath");


        //get the url from database
        PatchRequestDatabaseHandler productData = new PatchRequestDatabaseHandler();
        String productUrl = productData.getProductURL(product);

        boolean check = new File(productDownloadPath,
                "wso2" + product + ".zip").exists();
        LOG.info("Product name : " + "wso2" + product + ".zip");

        //download needed vanilla product packs
        if (!productUrl.equals("") && !check) {
            try {
                LOG.info("Product pack downloading...");
                Process executor = Runtime.getRuntime().exec("bash " + productDownloadPath +
                        "download-product.sh " + productUrl);
                executor.waitFor();
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
                wumUcResponse.setExitCode(1);
                wumUcResponse.setResponse(e.getMessage());
                return e.getMessage();
            }
        } else {
            LOG.error("URL of the vanilla pack not inserted into database or the pack not in the product pack ");

        }
        if (check) {
            try {

                Process executor = Runtime.getRuntime().exec(updateValidateScriptPath + "wum-uc validate " +
                        filepath + " " + productDownloadPath + "wso2" + product + ".zip");
                executor.waitFor();
                int exitStatus = executor.exitValue();
                BufferedReader validateMessage = new BufferedReader(new InputStreamReader(executor.getInputStream()));
                String validateReturn;
                while ((validateReturn = validateMessage.readLine()) != null) {
                    LOG.info(validateReturn);
                    outMessage.append(validateReturn);
                }
                //return result got from the WUM validator
              //  return outMessage.toString();
                wumUcResponse.setExitCode(exitStatus);
                wumUcResponse.setResponse(outMessage.toString());
                return String.valueOf(outMessage.toString());
            } catch (IOException | InterruptedException e) {
                LOG.error(e.getMessage());
            }
        } else {
            errorMessage = errorMessage + ("Product vanilla pack URL is incorrect or empty. Contact responsible " +
                    "person and update the database. Product: \"") + product + "\"";
            LOG.error(errorMessage);
            wumUcResponse.setExitCode(1);
            wumUcResponse.setResponse(errorMessage);
            return errorMessage;
        }

        FileUtils.deleteDirectory(new File(destFilePath)); //delete downloaded files
        wumUcResponse.setExitCode(1);
        wumUcResponse.setResponse(errorMessage);
        LOG.info(errorMessage + "\n");
        return errorMessage;



    }
}


