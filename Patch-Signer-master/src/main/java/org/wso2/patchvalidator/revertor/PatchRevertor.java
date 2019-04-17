package org.wso2.patchvalidator.revertor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.wso2.patchvalidator.validators.PatchValidator;

import java.io.IOException;
import java.util.Properties;

/**
 * <h1>Patch Revertor</h1>
 * Signed patches or Updates deletes from SVN repository and
 * delete data entries in UAT and Staging Databases.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class PatchRevertor {

    private static final Logger LOG = LoggerFactory.getLogger(PatchValidator.class);
    private Properties prop = new Properties();

    //patch url : wilkes/patches/patch2345/
    private SVNClientManager buildClientManager(String patchUrl) throws IOException {

        prop.load(PatchRevertor.class.getClassLoader().getResourceAsStream("application.properties"));
        final String username = prop.getProperty("testUsername");
        final String password = prop.getProperty("testPassword");
        final String svnBaseUrl = prop.getProperty("staticURL");

        try {
            DAVRepositoryFactory.setup();
            SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();

            SVNURL svnUrl = SVNURL.parseURIDecoded(svnBaseUrl + patchUrl);
            SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
            ISVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);

            ISVNAuthenticationManager myAuthManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(myAuthManager);

            SVNClientManager clientManager = SVNClientManager.newInstance(myOptions, myAuthManager);
            return clientManager;
        } catch (SVNException e) {
            SVNErrorMessage err = e.getErrorMessage();
            while (err != null) {
                LOG.error(err.getErrorCode().getCode() + " : " + err.getMessage());
                err = err.getChildErrorMessage();
            }
            return null;
        }
    }

    public String unlockAndDeleteSvnRepository(String patchUrl, String patchId) throws IOException {

        prop.load(PatchRevertor.class.getClassLoader().getResourceAsStream("application.properties"));
        final String svnBaseUrl = prop.getProperty("staticURL");
        String[] svnRepositoryFiles = prop.getProperty("svnRepositoryFiles").split(",");
        String patchIdReplaceTerm = prop.getProperty("patchNoReplaceTerm");

        SVNClientManager clientManager = buildClientManager(patchUrl);
        if (clientManager == null) {
            return "Error in building SVN Client Manager";
        }
        SVNWCClient wcClient = clientManager.getWCClient();
        SVNCommitClient commitClient = clientManager.getCommitClient();
        for (String file : svnRepositoryFiles) {
            file = file.replaceAll(patchIdReplaceTerm, patchId);
            try {
                SVNURL[] unlockFilesArray = new SVNURL[1];
                unlockFilesArray[0] = SVNURL.parseURIDecoded(svnBaseUrl + patchUrl + file);
                //wcClient.doLock(unlockFilesArray,true,"test lock");             //TODO : delete this line
                wcClient.doUnlock(unlockFilesArray, true);
                LOG.info("Unlocked file: " + file);
                if (!file.endsWith("zip")) {
                    commitClient.doDelete(unlockFilesArray, "Delete for revert the patch");
                    LOG.info("Deleted file: " + file);
                }
            } catch (SVNException e) {
                SVNErrorMessage err = e.getErrorMessage();
                while (err != null) {
                    LOG.error(err.getErrorCode().getCode() + " : " + err.getMessage());
                    err = err.getChildErrorMessage();
                }
                return "Error in SVN unlock process";
            }
        }
        return "REVERTED";
    }
}
