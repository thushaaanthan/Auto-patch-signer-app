package org.wso2.patchvalidator.validators;


/**
 * <h1>Update Validator</h1>
 *handle the response comes from WUM-UC
 *
 * @author Thushanthan Amalanathan
 * @version 1.2
 * @since 2019-01-21
 */
public class WumUcResponse {
    String response;
    int exitCode;
    private static WumUcResponse wumUcResponse;

    private WumUcResponse() {
    }

    public static synchronized WumUcResponse getInstance() {
        if (wumUcResponse == null) {
            wumUcResponse = new WumUcResponse();
            return wumUcResponse;
        } else {
            return wumUcResponse;
        }
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
