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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.patchvalidator.validators;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
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
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.tmatesoft.svn.core.SVNURL.parseURIDecoded;
import static org.tmatesoft.svn.core.SVNURL.parseURIEncoded;

/**
 * <h1>Patch Zip Validator</h1>
 * Validate each file in zip file of the patch and return error messages
 * or successful message.
 *
 * @author Kosala Herath,Senthan Prasanth
 * @version 1.3
 * @since 2017-12-14
 */
public class PatchZipValidator implements CommonValidator {

    private static final Properties prop = new Properties();
    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOG = LoggerFactory.getLogger(CommonValidator.class);
    PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
    private String username = "patchsigner@wso2.com";  //TODO : use this from application properties
    private String password = "xcbh8=cfj0mfgsOekDbh";
    private boolean securityPatch = true;
    private boolean isPatchEmpty = false;

    private boolean isResourcesFileEmpty = false;

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private static boolean isDirEmpty(final File directory) throws IOException {

        DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath());
        return !dirStream.iterator().hasNext();
    }

    private static String svnConnection(String svnURL, String svnUser, String svnPass) {

        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        SVNRepository repository;
        try {

            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnURL));
            LOG.info("ESTABLISH CONNECTION TO : " + svnURL);
            ISVNAuthenticationManager authManager = new BasicAuthenticationManager(new SVNAuthentication[]{
                    new SVNPasswordAuthentication(svnUser, svnPass, false, parseURIEncoded(svnURL),
                            false)});
            repository.setAuthenticationManager(authManager);
            repository.testConnection();
            LOG.info("SVN CONECTION SUCCESSFUL...");
            return Constants.CONNECTION_SUCCESSFUL;

        } catch (SVNException e) {
            LOG.error("SVN CONNECTION FAILED...");
            return Constants.SVN_CONNECTION_FAIL_STATE;
        }

    }

    public static String commitKeys(String patchUrl, String patchDestination) throws IOException {

        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String signingScriptPath = prop.getProperty("signingScriptPath");

        //copy signing script to the patch location
        File source = new File(signingScriptPath);
        File dest = new File(patchDestination);
        FileUtils.copyFileToDirectory(source, dest);
        String resultSVNCommit = "";

        try {
            Runtime.getRuntime().exec("chmod a+rwx " + patchDestination + "signing-script.sh");
            Process executor = Runtime.getRuntime().exec("bash " + patchDestination + "signing-script.sh " +
                    patchDestination);
            executor.waitFor();
            resultSVNCommit = commitToSVN(patchUrl, patchDestination);
            return resultSVNCommit;

        } catch (InterruptedException e) {
            resultSVNCommit = e.getMessage();
            LOG.error(resultSVNCommit);
            return resultSVNCommit;
        }
    }

    private static String commitToSVN(String patchUrl, String patchDestination) throws IOException {

        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String username = prop.getProperty("username");
        final String password = prop.getProperty("password");
        try {
            setupLibrary();
            SVNURL svnUrl = SVNURL.parseURIDecoded("https://svn.wso2.com/wso2/custom/projects/projects/carbon/" +
                    patchUrl);
            SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
            ISVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);

            ISVNAuthenticationManager myAuthManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(myAuthManager);

            SVNClientManager clientManager = SVNClientManager.newInstance(myOptions, myAuthManager);
            SVNCommitClient commitClient = clientManager.getCommitClient();
            SVNWCClient wcClient = clientManager.getWCClient();

            //select each files for commit to svn
            File[] files = new File(patchDestination).listFiles();
            List<String> commitFilesList = new ArrayList<>();
            List<String> lockFilesList = new ArrayList<>();

            for (File file : files) {
                if (file.isFile() && (!FilenameUtils.getExtension(file.getName()).equals("sh")) &&
                        (!FilenameUtils.getExtension(file.getName()).equals("zip"))) {
                    commitFilesList.add(file.getName());
                }
                if (file.isFile() && (!FilenameUtils.getExtension(file.getName()).equals("sh"))) {
                    lockFilesList.add(file.getName());
                }
            }
            LOG.info("Keys commit to : " + svnUrl);

            for (String commitFile : commitFilesList) {
                File fileToCheckIn = new File(patchDestination + commitFile);
                SVNCommitInfo importInfo = commitClient.doImport(fileToCheckIn, SVNURL.parseURIDecoded
                        (svnUrl + "/" + commitFile), "sign and keys generate", true);
                LOG.info("File committed. New Revision number for SVN : " + Long.toString(importInfo.getNewRevision()));
            }

            SVNURL[] lockFilesArray = new SVNURL[1];
            for (String lockFile : lockFilesList) {
                SVNURL fileToLock = SVNURL.parseURIDecoded(svnUrl + "/" + lockFile);
                lockFilesArray[0] = fileToLock;
                wcClient.doLock(lockFilesArray, true, "Patches are not allowed to modify after signed.");
                LOG.info("File Locked : " + lockFile);
            }

            return (Constants.SUCCESSFULLY_KEY_COMMITTED);

        } catch (SVNException e) {
            SVNErrorMessage err = e.getErrorMessage();
            while (err != null) {
                LOG.error(err.getErrorCode().getCode() + " : " + err.getMessage());
                err = err.getChildErrorMessage();
            }
            return (Constants.COMMIT_KEYS_FAILURE);
        }
    }

    private static void setupLibrary() {

        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }

    @Override
    public String checkReadMe(String filePath, String patchId, String[] productNameArray)
            throws IOException, SQLException {

        StringBuilder errorMessage = new StringBuilder();
        Boolean jar = false;
        Boolean war = false;
        Boolean jag = false;
        //todo: complete .jar .war and jag
        File dir = new File(filePath + "/resources");
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith((".jar"))) {
                    jar = true;
                }
                if (file.getName().endsWith((".war"))) {
                    war = true;
                }
                if (file.getName().endsWith((".jag"))) {
                    jag = true;

                }
            }
        }

        String filepath = filePath + "README.txt";
        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant README.txt does not exist\n";
        }

        List<String> lines = FileUtils.readLines(file, "UTF-8");

        String[] line = lines.get(0).split("-");
        if (!Objects.equals(patchId, line[4]) || Objects.equals(lines.get(0), "Patch ID         : patchId")) {
            errorMessage = new StringBuilder("'Patch ID' line in the README.txt has an error\n");
        }

        line = lines.get(1).split(": ");
        boolean isSameProducts = false;
        if (line.length > 1) {
            String[] productNameArrayReadMe = line[1].split(",");
            for (int i = 0; i < productNameArrayReadMe.length; i++) {
                productNameArrayReadMe[i] = productNameArrayReadMe[i].trim();
            }

            isSameProducts = (new HashSet<>(Arrays.asList(productNameArray)).equals(new HashSet<>(Arrays.asList(
                    productNameArrayReadMe))));
        }
//        if (line.length < 2 || !isSameProducts) {
//            errorMessage.append("'Applies To' line in the README.txt has an error\n");
//        }

        line = lines.get(2).split(":");
        if (line.length == 2 && Objects.equals(line[1], "publicJIRA")) {
            errorMessage.append("'Associated JIRA' line in the README.txt has an error\n");
        } else if (line.length == 1 && securityPatch) {
            LOG.info("This is identified as a Security patch");
        }
//edited
/*

        line = lines.get(1).split(":");
        String productData[] = line[2].split(",");
        for (int tt = 0; tt < productData.length; tt++) {
            String product = productData[tt];
            int productTypeDetails = patchRequestDatabaseHandler.getProductType(product);
            if (productTypeDetails  == 2) {
                errorMessage.append("There are some Update only files in the Readme File\n");
                break;
            }

        }
*/
//

        for (int i = 3; i < lines.size(); i++) {
            if (lines.get(i).startsWith("DESCRIPTION")) {
                if (lines.get(i + 1).startsWith("Patch description goes here") || lines.get(i).isEmpty()) {
                    errorMessage.append("DESCRIPTION section in the README.txt is not in the correct format\n");
                }
                i++;
            }
            if (lines.get(i).startsWith("INSTALLATION INSTRUCTIONS")) {
                boolean jaggeryInstruction = false;
                for (int j = i + 1; j < lines.size(); j++) {
                    if (jag &&
                            lines.get(j).contains(
                                    " Merge and Replace resource/store to " +
                                            "<CARBON_SERVER>/repository/deployment/server/jaggeryapps/store") &&
                            lines.get(j + 1).contains(
                                    " Merge and Replace resource/publisher to " +
                                            "<CARBON_SERVER>/repository/deployment/server/jaggeryapps/publisher")) {
                        jaggeryInstruction = true;

                    }
                    if (lines.get(j).contains("Copy the patchNumber to")) {
                        errorMessage.append("INSTALLATION INSTRUCTIONS section " + "in the README.txt is not in the " +
                                "correct format: Check patchNumber\n");
                    } else if (lines.get(j).contains("Copy the patch")) {
                        if (!lines.get(j).contains("Copy the patch" + patchId + " to")) {
                            errorMessage.append("INSTALLATION INSTRUCTIONS section " + "in the README.txt is not in " +
                                    "the correct format: Check patchNumber\n");
                        }
                    }
//edit
                    /*
                    if (lines.get(j).contains("Shutdown the server")) {

                        if (!lines.get(j).contains("if you have already started")) {
                            errorMessage.append("INSTALLATION INSTRUCTIONS section " + "in the README.txt is not in " +
                                    "the correct format: Check Shutdown\n");
                        }
                    }

                    if (!lines.get(j).contains("Restart the server with")) {
                        if (!lines.get(j + 1).isEmpty()) {
                            errorMessage.append("INSTALLATION INSTRUCTIONS section " + "in the README.txt is not in " +
                                    "the correct format: Check Restart\n");
                        }
                    }
                    */
 //end edit
                    i++;
                }
                if (jag && !jaggeryInstruction) {
                    errorMessage.append("Jaggery instructions are not in the correct format");
                }
            }
        }
        return errorMessage.toString();
    }

    @Override
    public String checkLicense(String filepath) throws IOException {

        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String license = prop.getProperty("license");
        LOG.info(filepath);

        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant LICENSE.txt  does not exist\n";
        }
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if (Objects.equals(md5, license)) {
            return "";
        }
        return "LICENSE.txt is not in the correct format";
    }

    @Override
    public String checkNotAContribution(String filepath) throws IOException {

        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String notAContribution = prop.getProperty("notAContribution");

        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant NOT_A_CONTRIBUTION.txt does not exist\n";
        }
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if (Objects.equals(md5, notAContribution)) {
            return "";
        }
        return "NOT_A_CONTRIBUTION.txt is not in the correct format";
    }

    @Override
    public boolean checkPatch(String filepath, String patchId) {

        Boolean jar = true;

        StringBuilder errorMessage = new StringBuilder();

        File dir = new File(filepath);
        if (!dir.exists() && dir.isDirectory()) {
            isPatchEmpty = false;
            LOG.error("patch" + patchId + " is empty!!");
            errorMessage.append("Patch folder is empty!");
        }

        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith((".jar"))) {
                    jar = true;
                } else {
                    jar = false;
                    errorMessage.append("Inappropriate " + file.getName() + "found");
                }

            }
        }
        return jar;

    }

    @Override
    public void unZip(File zipFilePath, String destFilePath) throws IOException {

        if (!zipFilePath.exists()) {
            return;
        }
        File destDir = new File(destFilePath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            String filePath = destDir + File.separator + zipEntry.getName();
            if (!zipEntry.isDirectory()) {
                new File(filePath).getParentFile().mkdirs();
                extractFile(zipInputStream, filePath);
                LOG.info("Extracting " + filePath);

            } else {
                File dir = new File(filePath);
                LOG.info("Extracting " + filePath);
                dir.mkdirs();
            }
            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    @Override
    public String checkContent(String filePath, String patchId) throws IOException {

        StringBuilder errorMessage = new StringBuilder();

        File destDir = new File(filePath);
        if (!destDir.exists()) {
            return "patch" + patchId + " content does not exist\n";
        } else {
            boolean check = new File(filePath + "LICENSE.txt").exists();
            if (!check) {
                errorMessage.append("LICENSE.txt does not exist\n");
            }

            check = new File(filePath + "README.txt").exists();
            if (!check) {
                errorMessage.append("README.txt does not exist\n");
            }

            check = new File(filePath + "NOT_A_CONTRIBUTION.txt").exists();
            if (!check) {
                errorMessage.append("NOT_A_CONTRIBUTION.txt does not exist\n");
            }

            check = new File(filePath + "patch" + patchId).exists();
            if (!check) {
                errorMessage.append("patch folder does not exist\n");
            }

            check = new File(filePath + "wso2carbon-version.txt").exists();
            if (check) {
                errorMessage.append("Unexpected file found: wso2carbon-version.txt\n");
            }

            String[] extensions = new String[]{"tmp", "swp", "DS_Dstore", "_MAX_OS"};
            List<File> files = (List<File>) FileUtils.listFiles(destDir, extensions, true);

            if (files.size() > 0) {
                errorMessage.append("Unexpected file found: check for temporary, hidden, etc.\n");
            }

            File[] hiddenFiles = destDir.listFiles((FileFilter) HiddenFileFilter.HIDDEN);
            assert hiddenFiles != null;
            for (File hiddenFile : hiddenFiles) {
                errorMessage.append("hidden file: ").append(hiddenFile.getName()).append("\n");

            }
            for (File file : destDir.listFiles()) {
                if (file.getName().endsWith(("~"))) {
                    errorMessage.append("Unexpected file found").append(file.getName()).append("\n");
                }
            }

            check = new File(filePath + "resources").exists();
            if (check) {
                File resourcesFile = new File(filePath + "resources");
                isResourcesFileEmpty = isDirEmpty(resourcesFile);
                /*check = new File(filePath + "resources/store").exists();
                if(!check) errorMessage = errorMessage + "inside the resources, store folder does not exist\n";

                check = new File(filePath + "resources/publisher").exists();
                if(!check) errorMessage = errorMessage + "inside the resources, publisher folder does not exist\n";*/
            }
            if (isResourcesFileEmpty && isPatchEmpty) {
                errorMessage.append("Both resources and patch").append(patchId).append(" folders are empty\n");
            }
            return errorMessage.toString();
        }
    }

    @Override
    public String downloadZipFile(String url, String version, String patchId, String destFilePath) {

        File destinationDirectory = new File(destFilePath);
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        String checkConnection = svnConnection(url, username, password);
        if (checkConnection.equals(Constants.CONNECTION_SUCCESSFUL)) {
            final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            try {
                final SvnCheckout checkout = svnOperationFactory.createCheckout();
                checkout.setSource(SvnTarget.fromURL(parseURIDecoded(url)));
                checkout.setSingleTarget(SvnTarget.fromFile(destinationDirectory));
                checkout.run();
            } catch (SVNException e) {
                LOG.info(String.valueOf(e));
                LOG.error("Requested url not found");
                return "Requested url not found: " + url;
            }
        }
        return "";
    }

}
