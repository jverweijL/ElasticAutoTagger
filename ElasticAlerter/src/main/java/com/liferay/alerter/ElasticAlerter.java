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
import com.liferay.portal.kernel.template.ClassLoaderTemplateResource;
import com.liferay.portal.kernel.template.StringTemplateResource;
import com.liferay.portal.kernel.template.Template;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.template.TemplateException;
import com.liferay.portal.kernel.template.TemplateManagerUtil;
import com.liferay.portal.kernel.template.TemplateResource;
import com.liferay.portal.kernel.template.URLTemplateResource;
import com.liferay.portal.kernel.util.PropsUtil;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
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

	private static Log log = LogFactoryUtil.getLog(ElasticAlerter.class);
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
						log.debug("uid: " + uid);						
						
						User user = UserLocalServiceUtil.getUser(Long.parseLong(uid));
						
						this.sentMail(user, entry);
					}
				}
				
			} catch (Exception ex) {
				log.error("Error: " + ex.getMessage());
			}
		}
	}
	
	private void sentMail(User user, AssetEntry entry) {
		log.debug("ready to sent a mail to: " + user.getEmailAddress());
		log.debug("title: " + entry.getTitleCurrentValue());
	
		
		//body += entry.getTitleCurrentValue();			
		
		//TemplateResource templateResource =
		//		TemplateResourceLoaderUtil.getTemplateResource(
		//			TemplateConstants.LANG_TYPE_FTL, resourcePath);
		
		String body = "";		

		try {
			//TemplateResource templateResource = new StringTemplateResource("0", "my email ${message}");
			
			TemplateResource templateResource = new URLTemplateResource("0",this.getClass().getClassLoader().getResource("personal_alert.ftl"));
			Template template = TemplateManagerUtil.getTemplate(
				TemplateConstants.LANG_TYPE_FTL, templateResource, false);
			
			// Add the data-models
	        template.put("user", user);
			template.put("entry", entry);				
	        
	        StringWriter out = new StringWriter();
	        
	        template.processTemplate(out);
	        body = out.toString();		        
	        log.debug(body);
	        
		} catch (TemplateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

/*        //List parsing 
        List<String> mailDetails = new ArrayList<String>();
        mailDetails.add(fromAddress);
        mailDetails.add(fromName);
        mailDetails.add(toAddress);
        mailDetails.add(toName);
        mailDetails.add(subject);
        mailDetails.add(body);

        data.put("mailDetails", mailDetails);*/

		try {			
	    		MailMessage mailMessage = new MailMessage();
	    		mailMessage.setTo(new InternetAddress(user.getEmailAddress()));
	    		mailMessage.setFrom(new InternetAddress("recyclebin@liferay.com"));
	    		mailMessage.setSubject("New interesting article");
	    		mailMessage.setBody(body);
	    		mailMessage.setHTMLFormat(true);
	    		
	    		MailServiceUtil.sendEmail(mailMessage);
	    		log.debug("Send mail");
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
		log.debug("\nSending 'POST' request to URL : " + url);
		log.debug("Post parameters : " + urlParameters);
		log.debug("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		log.error("Alerter: " + response.toString());
		if (response != null && response.length() > 0) {
			return response.toString().split(",");
		} else {
			return null;
		}
	}

}