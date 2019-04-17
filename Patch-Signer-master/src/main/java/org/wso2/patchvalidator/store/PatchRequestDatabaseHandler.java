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
package org.wso2.patchvalidator.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonDatabaseHandler;
import org.wso2.patchvalidator.service.SyncService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * <h1>PMT Database Access</h1>
 * Read data from PMT database about product details.
 *
 * @author Kosala Herath,Senthan Prasanth
 * @version 1.2
 * @since 2017-12-14
 */
public class PatchRequestDatabaseHandler implements CommonDatabaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PatchRequestDatabaseHandler.class);
    private Properties prop = new Properties();
    private Connection connectDB;

    {
        try {
            prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
            String dbURL = prop.getProperty("dbURL");
            String dbUser = prop.getProperty("dbUser");
            String dbPassword = prop.getProperty("dbPassword");
            connectDB = DriverManager.getConnection(dbURL, dbUser, dbPassword);
        } catch (SQLException | IOException e) {
            LOG.error("Database connection failure.");
        }
    }

    @Override
    public int getProductType(String product) throws SQLException {

        Statement create = connectDB.createStatement();
        String productTypeChooser = "SELECT TYPE FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS WHERE " +
                "PRODUCT_ABBREVIATION='" + product + "'";
        ResultSet result = create.executeQuery(productTypeChooser);

        int type = 0;
        while (result.next()) {
            type = result.getInt("TYPE");
        }
        return type;
    }

    @Override
    public String getProductAbbreviation(String productName, String productVersion) throws SQLException {

        Statement create = connectDB.createStatement();

        String productChooser = "SELECT PRODUCT_ABBREVIATION FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS " +
                "WHERE PRODUCT_NAME='" + productName
                + "' AND PRODUCT_VERSION='" + productVersion + "'";
        ResultSet result = create.executeQuery(productChooser);

        while (result.next()) {
            productName = result.getString("PRODUCT_ABBREVIATION");
        }
        return productName;
    }

    @Override
    public JsonArray getProductList() {

        JsonArray productList = new JsonArray();
        try {
            Statement create = connectDB.createStatement();
            String productTypeChooser = "SELECT PRODUCT_NAME , PRODUCT_VERSION FROM " +
                    "WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS ORDER BY PRODUCT_NAME ASC";
            ResultSet result = create.executeQuery(productTypeChooser);
            String productName;
            String productVersion;
            String fullProductName;

            while (result.next()) {
                productName = result.getString("PRODUCT_NAME");
                productVersion = result.getString("PRODUCT_VERSION");

                fullProductName = productName + " " + productVersion;
                JsonObject productDetail = new JsonObject();
                productDetail.addProperty("value", fullProductName);
                productDetail.addProperty("label", fullProductName);
                productList.add(productDetail);
            }
        } catch (SQLException e) {
            LOG.error(e.getSQLState());
            JsonObject productDetail = new JsonObject();
            productDetail.addProperty("value", "No Data");
            productDetail.addProperty("label", "No Data");
            productList.add(productDetail);
            return productList;
        } catch (NullPointerException e) {
            LOG.error("SQL connection failed");
            JsonObject productDetail = new JsonObject();
            productDetail.addProperty("value", "No Data");
            productDetail.addProperty("label", "No Data");
            productList.add(productDetail);
            return productList;
        }
        return productList;
    }

    public String getProductURL(String productAbbreviation) throws SQLException {

        Statement create = connectDB.createStatement();
        String productUrl = null;
        String productChooser = "SELECT PRODUCT_URL FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS " +
                "WHERE PRODUCT_ABBREVIATION='" + productAbbreviation + "'";
        ResultSet result = create.executeQuery(productChooser);

        while (result.next()) {
            productUrl = result.getString("PRODUCT_URL");
        }
        return productUrl;
    }

    @Override
    public void insertDataToTrackDatabase(String patchId, String version, int state, int type, String product,
                                          String developedBy,
                                          String status) throws SQLException {

        Statement create = connectDB.createStatement();
        switch (version) {
            case "wilkes":
                version = "4.4.0";
                break;
            case "hamming":
                version = "5.2.0";
                break;
            case "turing":
                version = "4.2.0";
                break;
        }

        String patchType = getPatchType(type);

        String processStatus = "SELECT * FROM WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS " +
                "WHERE STATUS='" + Constants.PROCESSING + "'";
        ResultSet inProcess = create.executeQuery(processStatus);

        if (inProcess.next()) {
            String postParametersInserter = "INSERT INTO WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS " +
                    "(PATCH_ID,VERSION,STATE,TYPE," +
                    "PRODUCT,DEVELOPED_BY,STATUS) VALUES ('" + patchId + "','" + version + "','" + state + "','" +
                    patchType + "','" + product + "','" + developedBy + "','" + status + "')";
            PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                    Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
            updatePostRequestStatus(product, patchId, Constants.QUEUE);
        } else {
            String postParametersInserter = "INSERT INTO " +
                    "WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS(PATCH_ID,VERSION,STATE,TYPE," +
                    "PRODUCT,DEVELOPED_BY,STATUS) VALUES ('" + patchId + "','" + version + "','" + state + "','" +
                    patchType + "','" + product
                    + "','" + developedBy + "','" + status + "')";
            PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                    Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
            updatePostRequestStatus(product, patchId, Constants.PROCESSING);
        }
    }

    public void updatePostRequestStatus(String product, String patchId, String status) throws SQLException {

        String changeStatus = "UPDATE WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS SET status='" +
                status + "' WHERE PRODUCT='" + product
                + "' && PATCH_ID='" + patchId + "'";
        PreparedStatement proceed = connectDB.prepareStatement(changeStatus, Statement.RETURN_GENERATED_KEYS);
        proceed.executeUpdate();

    }

    //add product to track database
    @Override
    public void insertProductToTrackDatabase(String productName, String productVersion, String carbonVersion,
                                             String kernelVersion, String productAbbreviation,
                                             int wumSupported, int type) throws SQLException {

        carbonVersion = getCarbonVersion(carbonVersion);
        //String patchType = getPatchType(type);
        String postParametersInserter = "INSERT INTO " +
                "WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS(PRODUCT_NAME,PRODUCT_VERSION,CARBON_VERSION," +
                "KERNEL_VERSION,PRODUCT_ABBREVIATION,WUM_SUPPORTED,TYPE) VALUES ('" + productName + "','" +
                productVersion + "','" + carbonVersion + "','" + kernelVersion + "','" + productAbbreviation
                + "','" + wumSupported + "','" + type + "')";
        PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                Statement.RETURN_GENERATED_KEYS);
        proceed.executeUpdate();
    }

    String getCarbonVersion(String carbonVersion) {

        LOG.info(carbonVersion);
        switch (carbonVersion) {
            case "wilkes":
                carbonVersion = "4.4.0";
                break;
            case "hamming":
                carbonVersion = "5.2.0";
                break;
            case "turing":
                carbonVersion = "4.2.0";
                break;
            default:
                LOG.info("Error in carbon version: " + carbonVersion);
                break;
        }
        return carbonVersion;
    }

    String getPatchType(int type) {
        String patchType = null;
        switch (type) {
            case 1:
                patchType = "patch";
                break;
            case 2:
                patchType = "update";
                break;
            case 3:
                patchType = "PatchAndUpdate";
                break;
            default:
                LOG.info("Error in patch type" + type);
        }
        return patchType;
    }
}


