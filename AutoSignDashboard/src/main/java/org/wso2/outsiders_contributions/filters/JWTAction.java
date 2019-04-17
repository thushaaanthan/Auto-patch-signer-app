package org.wso2.outsiders_contributions.filters;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.log4j.Logger;
import org.wso2.outsiders_contributions.msf4jhttp.PropertyReader;


import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is for handling sso configuration.
 */
public class JWTAction implements Filter {
    private static final Logger logger = Logger.getLogger(JWTAction.class);
    private static final PropertyReader propertyReader = new PropertyReader();


    /**
     * This method is for get public key
     *
     * @return return for getting public key
     * @throws IOException              if unable to load the file
     * @throws KeyStoreException        if unable to get instance
     * @throws CertificateException     if unable to certify
     * @throws NoSuchAlgorithmException cause by other underlying exceptions(KeyStoreException)
     */

    private static PublicKey getPublicKey() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        InputStream file = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(propertyReader.getSsoKeyStoreName());
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        //loading key store with password
        keystore.load(file, propertyReader.getSsoKeyStorePassword().toCharArray());
        Certificate cert = keystore.getCertificate(propertyReader.getSsoCertAlias());
        return cert.getPublicKey();
    }

    public void init(FilterConfig filterConfig) {

    }


    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String jwt = request.getHeader("X-JWT-Assertion");
        String ssoRedirectUrl = propertyReader.getSsoRedirectUrl();

        if (jwt == null || "".equals(jwt)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Redirecting to {}");
            }
            response.sendRedirect(ssoRedirectUrl);
            return;
        }

        String username = null;
        String roles = null;

        try {

            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) getPublicKey());

            if (signedJWT.verify(verifier)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("JWT validation success for token: {}");
                }
                username = signedJWT.getJWTClaimsSet().getClaim("http://wso2.org/claims/emailaddress").toString();
                roles = signedJWT.getJWTClaimsSet().getClaim("http://wso2.org/claims/role").toString();
                if (logger.isDebugEnabled()) {
                    logger.debug("User = {" + username + "} | Roles = " + roles);
                }
                if (roles != null) {
                    List<String> listOfRoles = Arrays.asList(roles.split(","));
                    if (!listOfRoles.contains(propertyReader.getAllowedUserRole())) {
                        logger.error("User does not have a valid role permissions.");
                        response.sendError(403);
                        return;
                    }
                }
            } else {
                logger.error("JWT validation failed for token: {" + jwt + "}");
                response.sendRedirect(ssoRedirectUrl);
                return;
            }
        } catch (ParseException e) {
            logger.error("Parsing JWT token failed");
        } catch (JOSEException e) {
            logger.error("Verification of jwt failed");
        } catch (Exception e) {
            logger.error("Failed to validate the jwt {" + jwt + "}");
        }

        if (username != null && roles != null) {
            request.getSession().setAttribute("user", username);
            request.getSession().setAttribute("roles", roles);
        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (ServletException e) {
            logger.error("Failed to pass the request, response objects through filters", e);
        }
    }

    public void destroy() {

    }
}