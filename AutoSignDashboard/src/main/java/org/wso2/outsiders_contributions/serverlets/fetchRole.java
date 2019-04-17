package org.wso2.outsiders_contributions.serverlets;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.nimbusds.jwt.SignedJWT;
import org.apache.log4j.Logger;
import org.wso2.outsiders_contributions.msf4jhttp.HttpHandler;
import org.wso2.outsiders_contributions.msf4jhttp.PropertyReader;

@WebServlet(
        name = "fetchRole",
        urlPatterns = "/fetchRole"
)
public class fetchRole extends HttpServlet {
    private static final Logger logger = Logger.getLogger(fetchData.class);
    private static final PropertyReader propertyReader = new PropertyReader();

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        try {
            logger.info("Request backend to fetch roles");
            String roles = String.valueOf(httpServletRequest.getSession().getAttribute("roles"));
            List<String> listOfRoles = Arrays.asList(roles.split(","));
            //check admin role is assigned
            int response = 0;
            if (listOfRoles.contains(propertyReader.getAllowedAdminRole())) { //change this
                logger.error("User has admin role permissions.");
                response = 1;
            }else{
                logger.error("User does not have admin role permissions.");
            }
            logger.info("Got:  " + response);
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            ServletOutputStream out = httpServletResponse.getOutputStream();
            out.print(response);
        } catch (IOException e) {
            logger.error("The response output stream failed ");
        }
    }
}
