package com.liferay.alerter;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.journal.model.JournalArticle;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailServiceUtil;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PropsUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.osgi.service.component.annotations.Component;

/**
 * @author jverweij
 */
@Component(
	immediate = true,
	property = {
		// TODO enter required service properties
	},
	service = ModelListener.class
)
public class ElasticAlerter extends BaseModelListener<AssetEntry> {

	private static Log _log = LogFactoryUtil.getLog(ElasticAlerter.class);
	private static String USER_AGENT = "Mozarella/5.0";
	private static Properties props = PropsUtil.getProperties();
	private static String ALERTER_SERVICE_PROPERTY = "com.liferay.alerter.service.url";

	@Override
	public void onAfterCreate(AssetEntry entry) throws ModelListenerException {
		super.onAfterCreate(entry);
		
		if (entry.getClassName().equalsIgnoreCase(JournalArticle.class.getName())) {
			try {
				
				//call the helper method to fetch all uids where reverse query matches
				String[] uids = fetchAlerts(entry.getTitleCurrentValue());
				
				if (uids != null && uids.length > 0) {
					ServiceContext serviceContext = new ServiceContext();
					serviceContext.setCompanyId(entry.getCompanyId());
					  
					//loop over comma-separated items in case of multiple uids..
					for (String uid: uids) {
						_log.debug("uid: " + uid);						
						
						User user = UserLocalServiceUtil.getUser(Long.parseLong(uid));
						
						this.sentMail(user, entry);
					}
				}
				
			} catch (Exception ex) {
				_log.error("Error: " + ex.getMessage());
			}
		}
	}
	
	private void sentMail(User user, AssetEntry entry) {
		_log.debug("ready to sent a mail to: " + user.getEmailAddress());
		_log.debug("title: " + entry.getTitleCurrentValue());
		
		String body = "Hi " + user.getFirstName() + "," + System.lineSeparator() + System.lineSeparator();
		body += "This article was published 56 milliseconds ago and might interest you"  + System.lineSeparator() + System.lineSeparator();
		body += entry.getTitleCurrentValue();		

		try {			
	    		MailMessage mailMessage = new MailMessage();
	    		mailMessage.setTo(new InternetAddress(user.getEmailAddress()));
	    		mailMessage.setFrom(new InternetAddress("recyclebin@liferay.com"));
	    		mailMessage.setSubject("New interesting article");
	    		mailMessage.setBody(body);
	    		
	    		MailServiceUtil.sendEmail(mailMessage);
	    		_log.debug("Send mail with Plain Text");
		} catch (AddressException e) {
		    	e.printStackTrace();
		}
	}
	
	/**
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	private String[] fetchAlerts(String text) throws MalformedURLException, IOException, ProtocolException {
		//TODO this must be configurable
		String url = props.getProperty(ALERTER_SERVICE_PROPERTY, "http://localhost:8080/tagger-service/rest/tags");

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "text=" + URLEncoder.encode(text,"utf-8");
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		_log.debug("\nSending 'POST' request to URL : " + url);
		_log.debug("Post parameters : " + urlParameters);
		_log.debug("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		_log.error("Alerter: " + response.toString());
		if (response != null && response.length() > 0) {
			return response.toString().split(",");
		} else {
			return null;
		}
	}

}