package org.wso2.patchvalidator.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.service.SyncService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * <h1>Staging Database Update</h1>
 * In reverting process delete or update Staging Database entries.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class StagingDatabaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StagingDatabaseHandler.class);
    private Properties prop = new Properties();

    private Connection connectDB;

    {
        try {
            prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
            String dbURL = prop.getProperty("stagingDbURL");
            String dbUser = prop.getProperty("stagingDbUser");
            String dbPassword = prop.getProperty("stagingDbPassword");

            connectDB = DriverManager.getConnection(dbURL, dbUser, dbPassword);
            LOG.info("Connected to Staging Database");
        } catch (SQLException | IOException e) {
            LOG.error("Database connection failure to Staging Database.");
        }
    }

    public String deletePatch(int patchId) {

        try {
            PreparedStatement st = connectDB.prepareStatement("DELETE FROM stagingUpdates WHERE update_no = ?");
            st.setInt(1, patchId);
            st.executeUpdate();
            LOG.info("Patch deleted successfully from UAT database");
            connectDB.close();
            return "Successful";
        } catch (SQLException e) {
            LOG.error("Error at deleting patch from UAT database: " + e.getSQLState());
            return null;
        } catch (NullPointerException e) {
            LOG.error("Error at deleting patch from Staging database: " + e.getMessage());
            return null;
        }
    }
}
