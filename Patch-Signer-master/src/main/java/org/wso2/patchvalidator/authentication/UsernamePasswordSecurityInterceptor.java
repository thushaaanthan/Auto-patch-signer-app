package org.wso2.patchvalidator.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.msf4j.security.basic.AbstractBasicAuthSecurityInterceptor;

import java.io.IOException;
import java.util.Properties;

/**
 * <h1>Username and Password Validation</h1>
 * Before reply to the request from the outside requester, validate the
 * given username and password. This is a basic Auth.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class UsernamePasswordSecurityInterceptor extends AbstractBasicAuthSecurityInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(UsernamePasswordSecurityInterceptor.class);
    private Properties prop = new Properties();

    @Override
    protected boolean authenticate(String username, String password) {

        try {
            prop.load(UsernamePasswordSecurityInterceptor.class.getClassLoader().getResourceAsStream(
                    "application.properties"));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        String validUsername = prop.getProperty("backend_service_username");
        String validPassword = prop.getProperty("backend_service_password");

        if (username.equals(validUsername) && password.equals(validPassword)) {
            return true;
        }
        return false;
    }
}
