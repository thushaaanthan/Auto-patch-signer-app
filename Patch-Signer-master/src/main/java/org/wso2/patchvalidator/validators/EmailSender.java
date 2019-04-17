/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.service.SyncService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * <h1>Email Sender</h1>
 * Send email to developer and user with the results of validation.
 *
 * @author Kosala Herath,Senthan Prasanth
 * @version 1.3
 * @since 2017-12-14
 */
public class EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailSender.class);

    private static void sendEmail(String fromAddress, ArrayList<String> toList, ArrayList<String> ccList,
                                  String subject, String body) throws IOException {

        Properties prop = new Properties();
        prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));

        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "tygra.wso2.com");
        prop.put("mail.smtp.port", "25");

        javax.mail.Session session = javax.mail.Session.getDefaultInstance(prop, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(prop.getProperty("user"), prop.getProperty("emailPassword"));
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));

            for (String aToList : toList) {
                message.addRecipient(Message.RecipientType.TO,
                        new InternetAddress(aToList));
            }
            for (String aCcList : ccList) {
                message.addRecipient(Message.RecipientType.CC,
                        new InternetAddress(aCcList));
            }

            message.setReplyTo(new InternetAddress[]
                    {new InternetAddress("maheshika@wso2.com")});  //added now

            message.setSubject(subject);
            message.setContent(body, "text/html");

            Transport transport = session.getTransport(prop.getProperty("protocol"));
            transport.connect(prop.getProperty("host"), prop.getProperty("user"), prop.getProperty("emailPassword"));
            Transport.send(message);
            LOG.info("Email sent successfully");

        } catch (MessagingException mex) {
            LOG.error("Email sending failed", mex);
        }
    }

    public void executeSendMail(ArrayList<String> toList, ArrayList<String> ccList, String patchId, String version,
                                String patchValidateStatus, String type) throws IOException {

        Properties prop = new Properties();
        prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));

        String subTitle = "";
        String subject;
        if (type.equals("patch")) {
            subTitle = "Patch validate status";
        } else if (type.equals("update")) {
            subTitle = "Update validate status";
        }
        {
            String validationReturner = "<html><body><table style=\"width:100%\"border=\"1px\"><tr " +
                    "style=\"font-size: 12\">" +
                    "<th bgcolor='black'><font color=\"white\">" +
                    subTitle + "</font></th><tr>" +
                    "<td align=\"center\">" + patchValidateStatus + "</td></tr></table>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";
//            if (patchValidateStatus.equals(Constants.SUCCESSFULLY_SIGNED) ||
// patchValidateStatus.contains(Constants.UPDATE_VALIDATED)) {
//                subject = "Patch-" + patchId + " successfully signed";
//            } else {
//                subject = "Patch-" + patchId + " validation failed";
//            }

            subject = "[SIGN REQUEST] Sign the patch WSO2-CARBON-PATCH-" + version + "-" + patchId;

            EmailSender.sendEmail(prop.getProperty("mailFrom"), toList, ccList, subject,
                    validationReturner);
        }
    }

    public void executeSendMailPatchAndUpdate(ArrayList<String> toList, ArrayList<String> ccList, String patchId,
                                              String version, String patchValidateStatus, String updateValidateStatus)
            throws IOException {

        Properties prop = new Properties();
        prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
        String subject;

        {
            String validationReturner = "<html><body><table style=\"width:100%\"border=\"1px\">" +
                    "<tr style=\"font-size: 12\">" +
                    "<th bgcolor='black'><font color=\"white\">Update validate status</font></th><th bgcolor='black'>" +
                    "<font color=\"white\">" +
                    "Patch validate status</font></th><tr>" +
                    "<td align=\"center\">" + updateValidateStatus + "</td><td align=\"center\">" +
                    patchValidateStatus + "</td></tr></table>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";

//            if (patchValidateStatus.equals(Constants.SUCCESSFULLY_SIGNED) &&
// updateValidateStatus.contains(Constants.UPDATE_VALIDATED)) {
//                subject = "Patch-" + patchId + " successfully signed";
//            } else {
//                subject = "Patch-" + patchId + " validation failed";
//            }

            subject = "[SIGN REQUEST] Sign the patch WSO2-CARBON-PATCH-" + version + "-" + patchId;

            EmailSender.sendEmail(prop.getProperty("mailFrom"), toList, ccList, subject,
                    validationReturner);
        }
    }
}
