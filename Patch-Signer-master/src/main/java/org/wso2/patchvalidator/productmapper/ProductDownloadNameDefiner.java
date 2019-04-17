package org.wso2.patchvalidator.productmapper;

/**
 * <h1>Product Download Name Definer</h1>
 * Define the path for wso2 product package in <a href="http://atuwa.private.wso2.com/WSO2-Products/">atuwa</a>
 * for download wso2 products.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class ProductDownloadNameDefiner {

    public String findProductUrl(String productAbbreviation) {

        //TODO - use a database to get this url locations for each product
        int lastOccurrence = productAbbreviation.lastIndexOf('-');
        String[] productNameArray = {productAbbreviation.substring(0, lastOccurrence),
                productAbbreviation.substring(lastOccurrence + 1)};
        String productVersion = productNameArray[1];
        String productName = productNameArray[0];
        String productUrl;

        switch (productName) {
            case "am":
                productUrl = "api-manager/";
                break;
            case "am-analytics":
                productUrl = "api-manager/";
                break;
            case "am-micro-gw":
                productUrl = "api-manager/";
                break;
            case "mb":
                productUrl = "message-broker/";
                break;
            case "cep":
                productUrl = "complex-event-processor/";
                break;
            case "dss":
                productUrl = "data-services-server/";
                break;
            case "emm":
                productUrl = "enterprise-mobility-manager/";
                break;
            case "ei":
                productUrl = "enterprise-integrator/";
                break;
            case "esb":
                productUrl = "enterprise-service-bus/";
                break;
            case "esb-analytics":
                productUrl = "enterprise-service-bus/";
                break;
            case "greg":
                productUrl = "governance-registry/";
                break;
            case "das":
                productUrl = "data-analytics-server/";
                break;
            case "is":
                productUrl = "identity-server/";
                break;
            case "is-km":
                productUrl = "identity-server/";
                break;
            case "is-analytics":
                productUrl = "identity-server/";
                break;
            case "sp":
                productUrl = "stream-processor/";
                break;
            case "iot":
                productUrl = "iot-server/";
                break;
            default:
                productUrl = "";
                break;
        }

        if (productUrl.equals("")) {
            return "";
        } else {
            productUrl = productUrl + productVersion + "/wso2" + productAbbreviation + ".zip";
            return productUrl;
        }
    }
}
