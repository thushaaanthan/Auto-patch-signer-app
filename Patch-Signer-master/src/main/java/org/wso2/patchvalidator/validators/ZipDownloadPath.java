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
package org.wso2.patchvalidator.validators;

import org.wso2.patchvalidator.service.SyncService;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * <h1>Zip Download Path</h1>
 * Define zip file download path and file paths
 *
 * @author Kosala Herath,Senthan Prasanth
 * @version 1.2
 * @since 2017-12-14
 */
class ZipDownloadPath {

    private String type;
    private String version;
    private String id;
    private String destFilePath;
    private String url;

    private Properties prop = new Properties();

    public ZipDownloadPath(String type, String version, String id) throws IOException {

        prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
        this.type = type;
        this.version = version;
        this.id = id;
    }

    public String getDestFilePath() {

        return prop.getProperty("destFilePath") + type + "/";
    }

    public String getUrl() {

        version = prop.getProperty(version);
        if (type.equals("patch")) {

            return url = prop.getProperty("staticURLWSO2") + version + "/" + type + "es/" + type + id + "/";
        } else {
            return url = prop.getProperty("staticURLWSO2") + version + "/" + type + "s/" + type + id + "/";
        }
    }

    public String getZipDownloadDestination() {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timeStamp = String.valueOf(timestamp.getTime());
        version = prop.getProperty(version);
        return getDestFilePath() + version + "/" + timeStamp + "/" + type + id + "/";
    }

    public String getFilepath() {

        if (type.equals("patch")) {
            return getZipDownloadDestination() + prop.getProperty("orgPatch") + version + "-" + id + ".zip";
        } else {
            return getZipDownloadDestination() + prop.getProperty("orgUpdate") + version + "-" + id + ".zip";
        }
    }

    public String getUnzippedFolderPath() {

        if (type.equals("patch")) {
            version = prop.getProperty(version);
            return getZipDownloadDestination() + prop.getProperty("orgPatch") + version + "-" + id + "/";
        } else {
            version = prop.getProperty(version);
            return getZipDownloadDestination() + prop.getProperty("orgUpdate") + version + "-" + id + "/";
        }
    }
}

