/* 

Copyright 2015 Novartis Institutes for Biomedical Research

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.novartis.pcs.ontology.service.notify;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.novartis.pcs.ontology.dao.CuratorDAOLocal;
import com.novartis.pcs.ontology.entity.Curator;

@Stateless
@Local(EmailSenderLocal.class)
public class EmailSender implements EmailSenderLocal{
		
	private static Logger logger = Logger.getLogger(
			EmailSender.class.getName());

	@Resource
	private javax.mail.Session session;
	
	@Resource(lookup="java:global/ontobrowser/url")
	private URL url;

	@EJB
	protected CuratorDAOLocal curatorDAO;
	
	public EmailSender() {
	}

	@Override
	public void send(String subject, String body) {
		send(subject, body, null);
	}
	
	@Override
	public void send(String subject, String body, CuratorCriteria criteria) {
		try {		
			Address[] to = createCuratorEmailAddresses(criteria == null ?
					new DefaultCuratorCriteria() : criteria);
			if(to.length > 0) {
				// Create message
				MimeMessage message = new MimeMessage(session);
				message.setFrom();
				message.setRecipients(Message.RecipientType.TO, to);
				message.setSubject(subject, "UTF-8");
				message.setSentDate(new Date());
				
				body = "You are receiving this email because you are registered as\n"
					+ "an ontology curator at " + url + "\n\n" + body;
				
				message.setText(body, "UTF-8");
	
				// Send message
				logger.log(Level.INFO, "Sending notification email with subject \""
						+ subject + "\" to: " + Arrays.toString(to));
				Transport.send(message);
			}
		} catch (Exception e) {
			String msg = "Failed to send email notifying curators of: " 
				+ subject + "\n" + body;
			logger.log(Level.SEVERE, msg, e);
			throw new RuntimeException(msg, e);
		}
	}
	
	private Address[] createCuratorEmailAddresses(CuratorCriteria criteria) throws AddressException {
		List<Curator> curators = curatorDAO.loadAll();
		List<Address> addresses = new ArrayList<Address>();
		
		for(Curator curator : curators) {
			if(curator.isActive()
					&& curator.getEmailAddress() != null
					&& criteria.verify(curator)) {
				addresses.add(new InternetAddress(curator.getEmailAddress(), true));
			}
		}
		
		return addresses.toArray(new Address[addresses.size()]);
	}
}

