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

import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
 * Read the properties of the application
 */
public class PropertyReader {

    private final static Logger logger = Logger.getLogger(PropertyReader.class);
    private final static String CONFIG_FILE = "config.properties";
    private String backendUrl;
    private String backendServiceUrl;
    private String backendPassword;
    private String backendUsername;
    private String ssoKeyStoreName;
    private String ssoKeyStorePassword;
    private String ssoCertAlias;
    private String ssoRedirectUrl;
    private String allowedUserRole;
    private String allowedAdminRole;
    private String trustStoreFile;
    private String trustStorePassword;
    private String trustStoreFileService;
    private String trustStorePasswordService;


    public PropertyReader() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        loadConfigs(inputStream);
    }


    /**
     * Load configs from the file
     *
     * @param input - input stream of the file
     */
    private void loadConfigs(InputStream input) {
        Properties prop = new Properties();
        try {
            prop.load(input);
            this.backendUrl = prop.getProperty("backend_url");
            this.backendServiceUrl = prop.getProperty("backend_service_url");
            this.backendPassword = prop.getProperty("backend_password");
            this.backendUsername = prop.getProperty("backend_username");
            this.ssoKeyStoreName = prop.getProperty("sso_keystore_file_name");
            this.ssoKeyStorePassword = prop.getProperty("sso_keystore_password");
            this.ssoCertAlias = prop.getProperty("sso_certificate_alias");
            this.ssoRedirectUrl = prop.getProperty("sso_redirect_url");
            this.allowedUserRole = prop.getProperty("allowed_user_role");
            this.allowedAdminRole = prop.getProperty("allowed_admin_role");
            this.trustStoreFile = prop.getProperty("trust_store_file_name");
            this.trustStorePassword = prop.getProperty("trust_store_password");
            this.trustStoreFileService = prop.getProperty("trust_store_file_service_name");
            this.trustStorePasswordService = prop.getProperty("trust_store_service_password");


        } catch (FileNotFoundException e) {
            logger.error("The configuration file is not found");
        } catch (IOException e) {
            logger.error("The File cannot be read");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("The File InputStream is not closed");
                }
            }
        }

    }

    String getBackendUrl() {
        return this.backendUrl;
    }
    String getBackendServiceUrl() {
        return this.backendServiceUrl;
    }

    String getBackendUsername() {
        return this.backendUsername;
    }

    String getBackendPassword() {
        return this.backendPassword;
    }

    public String getSsoKeyStoreName() {
        return this.ssoKeyStoreName;
    }

    public String getSsoKeyStorePassword() {
        return this.ssoKeyStorePassword;
    }

    public String getSsoCertAlias() {
        return this.ssoCertAlias;
    }

    public String getSsoRedirectUrl() {
        return this.ssoRedirectUrl;
    }

    public String getTrustStoreFile() { return this.trustStoreFile; }

    public String getTrustStorePassword() { return this.trustStorePassword; }

    public String getTrustStoreFileService() { return this.trustStoreFileService; }

    public String getTrustStorePasswordService() { return this.trustStorePasswordService; }

    public String getAllowedUserRole() { return this.allowedUserRole; }

    public String getAllowedAdminRole() { return  this.allowedAdminRole; }

}
