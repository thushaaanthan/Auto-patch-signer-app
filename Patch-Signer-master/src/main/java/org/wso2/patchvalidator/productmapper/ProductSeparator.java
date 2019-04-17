package org.wso2.patchvalidator.productmapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;

import java.io.IOException;
import java.sql.SQLException;

/**
 * <h1>Product Separator</h1>
 * Separate each product and get product abbreviation and return array of product names.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class ProductSeparator {

    private static final Logger LOG = LoggerFactory.getLogger(ProductSeparator.class);

    public String splitProduct(String productName) throws IOException, SQLException {

        StringBuilder productListStr = new StringBuilder();
        String[] productNameArray = productName.split("\n");
        int n = productNameArray.length - 1;
        if (n > 0) {
            String[] newProductNameArray = new String[n];
            System.arraycopy(productNameArray, 1, newProductNameArray, 0, n);

            for (String item : newProductNameArray) {
                String[] productVersion = item.split(" ");
                String splitProductName = item.split("[0-9]")[0];
                PatchRequestDatabaseHandler productData = new PatchRequestDatabaseHandler();
                String product = productData.getProductAbbreviation(splitProductName,
                        productVersion[productVersion.length - 1]);
                productListStr.append(product);
                productListStr.append(",");
            }
            String productList = productListStr.toString();
            productList = productList.substring(0, productList.length() - 1);
            return productList;
        } else {
            LOG.error("No products names are given");
            return null;
        }
    }
}
