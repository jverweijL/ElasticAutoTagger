package com.liferay.tagger;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetTagLocalServiceUtil;
import com.liferay.asset.kernel.service.persistence.AssetEntryQuery;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.PropsUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
/*import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;*/
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
/*import java.net.URL;*/
import java.util.List;
import java.util.Properties;

import org.osgi.service.component.annotations.Component;



/**
 * @author jverweij
 */
@Component(
	immediate = true,
	name = "ElasticAutoTagger",
	property = {
		// TODO enter required service properties
	},
	service = ModelListener.class
)
public class ElasticAutoTagger extends BaseModelListener<AssetEntry> {

	private static Log _log = LogFactoryUtil.getLog(ElasticAutoTagger.class);
	private static String USER_AGENT = "Mozarella/5.0";
	private static Properties props = PropsUtil.getProperties();
	private static String HAS_TAG_PROPERTY = "com.liferay.tagger.has-tag";
	private static String TAGGER_SERVICE_PROPERTY = "com.liferay.tagger.service.url";

	@Override
	public void onAfterCreate(AssetEntry entry) throws ModelListenerException {
		super.onAfterCreate(entry);
		
		if (entry.getClassName().equalsIgnoreCase(JournalArticle.class.getName())) {
			try {
				
				if (mustbeTagged(entry)) {
					String[] tags = fetchTags(entry.getTitleCurrentValue());
					
					if (tags != null && tags.length > 0) {
						ServiceContext serviceContext = new ServiceContext();
						serviceContext.setCompanyId(entry.getCompanyId());
						  
						//loop over comma-separated items in case of multiple tags..
						for (String tagName: tags) {
						//String tagName = tags[0];
							_log.debug("tagname: " + tagName);
							AssetTag assetTag;
							if (AssetTagLocalServiceUtil.hasTag(entry.getGroupId(), tagName)) {
								assetTag = AssetTagLocalServiceUtil.getTag(entry.getGroupId(), tagName);
							} else {
								_log.debug("create tagname: " + tagName);
								assetTag = AssetTagLocalServiceUtil.addTag(entry.getUserId(), entry.getGroupId(), tagName, serviceContext);
							}				
							
							long[] tagIds = { assetTag.getTagId() };
							_log.debug("tag: " + assetTag.getName());
							_log.debug("entryID: " + entry.getEntryId());
						
							// connect the tag to the asset
							AssetTagLocalServiceUtil.addAssetEntryAssetTag(entry.getEntryId(), assetTag);
						}
					}
				}
			} catch (Exception ex) {
				_log.error("Foutje: " + ex.getMessage());
			}
		}
	}

	private boolean mustbeTagged(AssetEntry entry) throws PortalException {
		//only autotag if there's an autotag tag or if it's empty
		String triggerTagName = props.getProperty(HAS_TAG_PROPERTY, "");
		if (triggerTagName.isEmpty()) {
			_log.debug("No trigger tagname found");
			return true;
		} else {
			_log.debug("Checking entry for tag " + triggerTagName);
			AssetTag triggerTag = AssetTagLocalServiceUtil.getTag(entry.getGroupId(), triggerTagName);
			_log.debug("Entry has tag: " + entry.getTags().contains(triggerTag));
			return entry.getTags().contains(triggerTag);
		}
	}

	/**
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	private String[] fetchTags(String text) throws MalformedURLException, IOException, ProtocolException {
		//TODO this must be configurable
		String url = props.getProperty(TAGGER_SERVICE_PROPERTY, "http://localhost:8080/tagger-service/rest/tags");

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
		_log.error("AutoTagger: " + response.toString());
		if (response != null && response.length() > 0) {
			return response.toString().split(",");
		} else {
			return null;
		}
	}
}