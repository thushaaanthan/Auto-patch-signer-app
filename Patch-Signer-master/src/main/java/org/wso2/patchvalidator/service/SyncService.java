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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.commiter.KeysCommitter;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.productmapper.ProductSeparator;
import org.wso2.patchvalidator.revertor.PatchRevertor;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;
import org.wso2.patchvalidator.store.StagingDatabaseHandler;
import org.wso2.patchvalidator.store.UatDatabaseHandler;
import org.wso2.patchvalidator.validators.EmailSender;
import org.wso2.patchvalidator.validators.PatchValidator;
import org.wso2.patchvalidator.validators.UpdateValidator;
import org.wso2.patchvalidator.validators.WumUcResponse;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <h1>SyncServices</h1>
 * All the endpoints in the microservice is defined here.
 *
 * @author Kosala Herath,Senthan Praanth
 * @version 1.2
 * @since 2017-12-14
 */
@Path("/request")
public class SyncService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);

    private boolean patchValidationStatus = true;
    private boolean updateValidationStatus = true;
    private Properties prop = new Properties();
    private WumUcResponse wumUcResponse = WumUcResponse.getInstance();

    @POST
    @Path("/service")
    @Produces(MediaType.TEXT_PLAIN)
    public Response postRequest(@FormParam("patchId") String patchId,
                                @FormParam("version") String version,
                                @FormParam("state") Integer state,
                                @FormParam("product") String productNameList,
                                @FormParam("productType") String productPackType,
                                @FormParam("developedBy") String developedBy) throws IOException,
            InterruptedException, SQLException {

        LOG.info("******************************************************************");
        LOG.info("                         NEW REQUEST                              ");
        LOG.info("******************************************************************");
        LOG.info("PATCH OR UPDATE SIGN REQUEST RECEIVED... >>> PATCH ID : " + patchId + ": PRODUCT LIST : " +
                productNameList);

        String patchValidateStatus = "N/A";
        String updateValidateStatus = "N/A";
        String status = "";

        ProductSeparator productSeparator = new ProductSeparator();
        String productList = productSeparator.splitProduct(productNameList);
        String[] productNameArray = productList.split(",");

        PatchValidator objPatchValidator = new PatchValidator();
        UpdateValidator objUpdateValidator = new UpdateValidator();

        LOG.info("VALIDATION PROCESS STARTED FOR EACH PRODUCT...");
        prop.load(PatchValidator.class.getClassLoader().getResourceAsStream("application.properties"));

        for (String product : productNameArray) {
            LOG.info("- - - - - - - - - - - - - - - - - - " + product + " - - - - - - - - - - - - - - - - - " +
                    "- - -");

            PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
            int productType = patchRequestDatabaseHandler.getProductType(product);

            //insert requests to register
            patchRequestDatabaseHandler.insertDataToTrackDatabase(patchId, version, state, productType, product,
                    developedBy, status);

            //check product type ( 1 = patch / 2 = update / 3 = patch and update )
            if (productType == 1) {   //patch validation
                LOG.info("THIS IS A ONLY PATCH VALIDATION AND PROCESS STARTED...");
                patchValidateStatus = objPatchValidator.zipPatchValidate(patchId, version, state, productType,
                        product, developedBy, productNameArray);
                LOG.info("PATCH VALIDATION FINISHED...");
                LOG.info("PATCH VALIDATION STATUS >>> " + patchValidateStatus);

                version = getString(version);
                boolean isSuccess = false;
                if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_VALIDATED))) {
                    LOG.info("PATCH VALIDATION SUCCESSFUL");
                    isSuccess = true;
                    patchValidationStatus = true;

                } else {
                    patchValidationStatus = false;
                }
                changeValidateStatus(patchId, product, patchRequestDatabaseHandler, isSuccess);
                version = prop.getProperty(version);
            } else if (productType == 2) {    //update validation
                LOG.info("THIS IS A ONLY UPDATE VALIDATION AND PROCESS STARTED...");
                updateValidateStatus = objUpdateValidator.zipUpdateValidate(patchId, version, productType,
                        product);
                LOG.info("UPDATE VALIDATION FINISHED...");
//                LOG.info("UPDATE VALIDATION STATUS >>> " + updateValidateStatus);
                LOG.info("UPDATE VALIDATION ExitCode >>> " + wumUcResponse.getExitCode());
                LOG.info("UPDATE VALIDATION STATUS >>> " + wumUcResponse.getResponse());

                version = getString(version);


                String statusOfUpdateValidation = "Validating update ...'" + prop.getProperty("orgUpdate") +
                        version + "-" + patchId + "' " + Constants.UPDATE_VALIDATED;
                int exitCode = wumUcResponse.getExitCode();
//                if (updateValidateStatus.equals(statusOfUpdateValidation)) {
//                    LOG.info("UPDATE VALIDATION SUCCESSFUL");
//                    updateValidationStatus = true;
//
//                } else {
//                    updateValidateStatus = updateValidateStatus + "-" + product;
//                    updateValidationStatus = false;
//                    version = prop.getProperty(version);
//                    changeValidateStatus(patchId, product, patchRequestDatabaseHandler, false);
//                    break;
//                }

                if (exitCode == 0) {
                    LOG.info("UPDATE VALIDATION SUCCESSFUL");
                    updateValidationStatus = true;

                } else {
//                    updateValidateStatus = updateValidateStatus + "-" + product;
                    updateValidateStatus = wumUcResponse.getResponse() + "-" + product;
                    updateValidationStatus = false;
                    version = prop.getProperty(version);
                    changeValidateStatus(patchId, product, patchRequestDatabaseHandler, false);
                    break;
                }
                changeValidateStatus(patchId, product, patchRequestDatabaseHandler, true);
                version = prop.getProperty(version);
            } else if (productType == 3) {  //patch and update validation
                LOG.info("THIS IS A PATCH AND UPDATE VALIDATION AND PROCESS STARTED...");

                LOG.info("PATCH VALIDATION STARTED...");
                patchValidateStatus = objPatchValidator.zipPatchValidate(patchId, version, state, productType, product,
                        developedBy, productNameArray);
                LOG.info("PATCH VALIDATION FINISHED...");

                LOG.info("UPDATE VALIDATION STARTED...");
                updateValidateStatus = objUpdateValidator.zipUpdateValidate(patchId, version, productType,
                        product);
                LOG.info("UPDATE VALIDATION FINISHED...");

                version = getString(version);
                LOG.info("PATCH VALIDATION STATUS >>> " + patchValidateStatus);
                LOG.info("UPDATE VALIDATION ExitCode >>> " + wumUcResponse.getExitCode());
                LOG.info("UPDATE VALIDATION STATUS >>> " + wumUcResponse.getResponse());
               // LOG.info("UPDATE VALIDATION STATUS >>> " + updateValidateStatus);


                String statusOfUpdateValidation = "Validating update ...'" + prop.getProperty("orgUpdate") +
                        version + "-" + patchId + "' " + Constants.UPDATE_VALIDATED;


                boolean isSuccessPatch = false;
                if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_VALIDATED))) {
                    LOG.info("PATCH VALIDATION SUCCESSFUL IN P&U TYPE");
                    patchValidationStatus = true;
                    isSuccessPatch = true;

                } else {
                    patchValidationStatus = false;  //insert break statement
                }
//                if (updateValidateStatus.equals(statusOfUpdateValidation)) {
//                    LOG.info("UPDATE VALIDATION SUCCESSFUL IN P&U TYPE");
//                    updateValidationStatus = true;
//                } else {
//                    updateValidateStatus = updateValidateStatus + "-" + product;
//                    updateValidationStatus = false;
//                    version = prop.getProperty(version);
//                    changeValidateStatus(patchId, product, patchRequestDatabaseHandler, false);
//                    version = prop.getProperty(version);
//                    break;
//                }
                int exitCode = wumUcResponse.getExitCode();
                if (exitCode == 0) {
                    LOG.info("UPDATE VALIDATION SUCCESSFUL IN P&U TYPE");
                    updateValidationStatus = true;
                } else {
//                    updateValidateStatus = updateValidateStatus + "-" + product;
                    updateValidateStatus = wumUcResponse.getResponse() + "-" + product;
                    updateValidationStatus = false;
                    version = prop.getProperty(version);
                    changeValidateStatus(patchId, product, patchRequestDatabaseHandler, false);
                    version = prop.getProperty(version);
                    break;
                }

                changeValidateStatus(patchId, product, patchRequestDatabaseHandler, isSuccessPatch);
                version = prop.getProperty(version);
            } else {
                patchValidateStatus = product + " : Product Type Error. Please update database for this product";
                updateValidateStatus = product + " : Product Type Error. Please update database for this product";
                patchValidationStatus = false;
                updateValidationStatus = false;
                version = prop.getProperty(version);

                changeValidateStatus(patchId, product, patchRequestDatabaseHandler, false);
                break;
            }
        }

        LOG.info("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");
        LOG.info("  PATCH VALIDATION SUCCESS : " + patchValidationStatus);
        LOG.info(" UPDATE VALIDATION SUCCESS : " + updateValidationStatus);
        LOG.info("<><><><><><><><><><><><><><><><><><><><><><><><><><><><><>");

        String[] validationResult = getAction(objPatchValidator, objUpdateValidator, patchValidationStatus,
                updateValidationStatus, patchValidateStatus, updateValidateStatus, patchId, version, developedBy);

        patchValidateStatus = validationResult[0];
        updateValidateStatus = validationResult[1];

        return Response.ok("------ Validation Process Finished ------#Patch Validate Status : #" +
                patchValidateStatus + "##updateValidateStatus : #" + updateValidateStatus, MediaType.TEXT_PLAIN)
                .header("Access-Control-Allow-Credentials", true)
                .build();

    }

    @POST
    @Path("/revert")
    @Produces(MediaType.TEXT_PLAIN)
    public Response revertPatch(@FormParam("patchId") String patchId,
                                @FormParam("version") String version,
                                @FormParam("onlySVNRevert") boolean onlySVNRevert) {

        LOG.info("PATCH OR UPDATE REVERTING REQUEST RECEIVED >>> PATCH ID : " + patchId);

        String patchUrl;
        String updateUrl;
        String resUatDatabase;
        String resStagingDatabase;
        String errorMessage = null;
        int patchIdInt = 0;

        try {
            if (version == null || patchId == null) {
                throw new IOException("PARAMETERS ARE NOT RECEIVED...");
            }
            patchUrl = version + "/patches/patch";
            updateUrl = version + "/updates/update";

            patchUrl = patchUrl + patchId + "/";
            updateUrl = updateUrl + patchId + "/";

            patchIdInt = Integer.parseInt(patchId);

            PatchRevertor patchRevertor = new PatchRevertor();
            String resSvnPatch = patchRevertor.unlockAndDeleteSvnRepository(patchUrl, patchId);
            String resSvnUpdate = patchRevertor.unlockAndDeleteSvnRepository(updateUrl, patchId);
            if (!resSvnPatch.equals("REVERTED") && !resSvnUpdate.equals("REVERTED")) {
                errorMessage = "\nNo Repository in SVN patch or update.";
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
            errorMessage = e.getMessage();
        }

        //delete from UAT database
        if (errorMessage == null && !onlySVNRevert) {
            //delete from UAT database
            UatDatabaseHandler uatDatabaseHandler = new UatDatabaseHandler();
            resUatDatabase = uatDatabaseHandler.deletePatch(patchIdInt);

            //delete from Staging database
            StagingDatabaseHandler stagingDatabaseHandler = new StagingDatabaseHandler();
            resStagingDatabase = stagingDatabaseHandler.deletePatch(patchIdInt);

            if (resStagingDatabase == null || resUatDatabase == null) {
                errorMessage = "\nError at database data deleting process.";
            }
        } else if (errorMessage != null) {
            errorMessage = errorMessage + "\nAborted from database deleting due to SVN error.";
        }

        if (errorMessage == null) {
            LOG.info("REVERTING PROCESS FINISHED SUCCESSFULLY...");
            return Response.ok("Reverting Process Finished Successfully\n", MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } else {
            LOG.error("REVERTING PROCESS FAILED. PROCESS END... " + errorMessage);
            return Response.ok("Reverting Process Failed. \n" + errorMessage, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    @POST
    @Path("/fetchData")
    @Produces(MediaType.TEXT_PLAIN)
    public Response fetchProductNames() {

        LOG.info("PRODUCT LIST REQUESTED FROM APP...");

        JsonArray productList;

        PatchRequestDatabaseHandler pmtData = new PatchRequestDatabaseHandler();
        productList = pmtData.getProductList();
        JsonObject productListFirstObject = (productList.get(0)).getAsJsonObject();

        if (productListFirstObject.get("value").toString().equals("No Data")) {
            LOG.error("PRODUCT LIST RETRIEVING PROCESS HAS OCCURRED A PROBLEM...");
            return Response.ok(productList, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } else {
            LOG.error("PRODUCT LIST RETRIEVING PROCESS FINISHED SUCCESSFULLY...");
            return Response.ok(productList, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    //add new product request
    @POST
    @Path("/addProduct")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addProduct(@FormParam("product") String productName,
                               @FormParam("productVersion") String productVersion,
                               @FormParam("carbonVersion") String carbonVersion,
                               @FormParam("kernelVersion") String kernelVersion,
                               @FormParam("productAbbreviation") String productAbbreviation,
                               @FormParam("WUMSupported") Integer wumSupported,
                               @FormParam("type") Integer type) {

        LOG.info("ADDING PRODUCT REQUESTED FROM APP...");
        try {
            PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
            //insert requests to database
            patchRequestDatabaseHandler.insertProductToTrackDatabase(productName, productVersion, carbonVersion,
                    kernelVersion, productAbbreviation, wumSupported, type);
            LOG.error("PRODUCT SUCCESSFULLY ADDED TO DATABASE...");
            return Response.ok("Adding product to DB Finished Successfully\n", MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return Response.ok("Exception occurred when adding product\n", MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    private void setCCList(@FormParam("developedBy") String developedBy, ArrayList<String> toList,
                           ArrayList<String> ccList) {

        toList.add(developedBy);
        ccList.add(prop.getProperty("ccList1"));
        ccList.add(prop.getProperty("ccList2"));
    }

    private String getString(@FormParam("version") String version) {

        switch (version) {
            case "wilkes":
                version = "4.4.0";
                break;
            case "hamming":
                version = "5.0.0";
                break;
            case "turing":
                version = "4.2.0";
                break;
            default:
                LOG.error("Error in version of the product");
        }
        return version;
    }

    private void changeValidateStatus(@FormParam("patchId") String patchId, String product,
                                      PatchRequestDatabaseHandler insertRequestParametersToDB, boolean isSuccess)
            throws SQLException {

        if (isSuccess) {
            LOG.info("Process Successful and status updated..!");
            insertRequestParametersToDB.updatePostRequestStatus(product, patchId, Constants.SUCCESS_STATE);
        } else {
            LOG.info("Process finished.");
            insertRequestParametersToDB.updatePostRequestStatus(product, patchId, Constants.VALIDATION_FAIL_STATE);
        }
    }

    private String[] getAction(PatchValidator objPatchValidator, UpdateValidator objUpdateValidator,
                               boolean patchValidationStatus, boolean updateValidationStatus,
                               String patchValidateStatus, String updateValidateStatus, String patchId, String version,
                               String developedBy)
            throws IOException {

        EmailSender checkValidity = new EmailSender();
        ArrayList<String> toList = new ArrayList<>();
        ArrayList<String> ccList = new ArrayList<>();

        switch (version) {
            case "wilkes":
                version = "4.4.0";
                break;
            case "hamming":
                version = "5.0.0";
                break;
            case "turing":
                version = "4.2.0";
                break;
        }

        if (patchValidationStatus && updateValidateStatus.equals("N/A")) {
            patchValidateStatus = patchValidateStatus + " and " + KeysCommitter.validateKeysCommit(
                    (objPatchValidator.patchUrl.split("carbon/")[1]), objPatchValidator.patchDestination);
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMail(toList, ccList, patchId, version, patchValidateStatus, "patch");
        } else if (!patchValidationStatus && updateValidateStatus.equals("N/A")) {
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMail(toList, ccList, patchId, version, patchValidateStatus, "patch");
        } else if (updateValidationStatus && patchValidateStatus.equals("N/A")) {
            updateValidateStatus = updateValidateStatus + " " + KeysCommitter.validateKeysCommit(
                    (objUpdateValidator.updateUrl.split("carbon/")[1]), objUpdateValidator.updateDestination);
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMail(toList, ccList, patchId, version, updateValidateStatus, "update");
        } else if (!updateValidationStatus && patchValidateStatus.equals("N/A")) {
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMail(toList, ccList, patchId, version, updateValidateStatus, "update");
        } else if (patchValidationStatus && updateValidationStatus) {
            patchValidateStatus = patchValidateStatus + " and " + KeysCommitter.validateKeysCommit(
                    (objPatchValidator.patchUrl.split("carbon/")[1]), objPatchValidator.patchDestination);
            updateValidateStatus = updateValidateStatus + " " + KeysCommitter.validateKeysCommit(
                    (objUpdateValidator.updateUrl.split("carbon/")[1]), objUpdateValidator.updateDestination);
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMailPatchAndUpdate(toList, ccList, patchId, version, patchValidateStatus,
                    updateValidateStatus);
        } else if (!patchValidationStatus && updateValidationStatus) {
            updateValidateStatus = updateValidateStatus + " " + KeysCommitter.validateKeysCommit(
                    (objUpdateValidator.updateUrl.split("carbon/")[1]), objUpdateValidator.updateDestination);
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMailPatchAndUpdate(toList, ccList, patchId, version, patchValidateStatus,
                    updateValidateStatus);
        } else if (patchValidationStatus && !updateValidationStatus) {
            patchValidateStatus = patchValidateStatus + " and " + KeysCommitter.validateKeysCommit(
                    (objPatchValidator.patchUrl.split("carbon/")[1]), objPatchValidator.patchDestination);
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMailPatchAndUpdate(toList, ccList, patchId, version, patchValidateStatus,
                    updateValidateStatus);
        } else {
            setCCList(developedBy, toList, ccList);
            checkValidity.executeSendMailPatchAndUpdate(toList, ccList, patchId, version, patchValidateStatus,
                    updateValidateStatus);
        }
        String[] result = {patchValidateStatus, updateValidateStatus};

        //delete downloaded files
        String destFilePath = prop.getProperty("destFilePath");
        FileUtils.deleteDirectory(new File(destFilePath));

        return result;
    }
}





