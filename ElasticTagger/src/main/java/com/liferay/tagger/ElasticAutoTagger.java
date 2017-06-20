package com.liferay.tagger;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetTagLocalServiceUtil;
import com.liferay.asset.kernel.service.persistence.AssetEntryQuery;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.service.ServiceContext;

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
	
	//TODO make this dynamic with elastic perculator
	//String autoTag = "autotag";
	
	private static Log _log = LogFactoryUtil.getLog(ElasticAutoTagger.class);
	private static String USER_AGENT = "Mozarella/5.0";

	@Override
	public void onAfterCreate(AssetEntry entry) throws ModelListenerException {
		super.onAfterCreate(entry);
		
		//TODO only autotag if there's an autotag tag
		
		if (entry.getClassName().equalsIgnoreCase(JournalArticle.class.getName())) {
			_log.error("TAG ARTICLE " + entry.getTitle());
			_log.error("TAG ARTICLE " + entry.getTitleCurrentValue());
			_log.error("TAG ARTICLE " + entry.getTitleCurrentLanguageId());
			//_log.error("TAG ARTICLE " + entry.getTitle(entry.getTitleCurrentLanguageId()));
			/*Properties props = PropsUtil.getProperties();
		      Enumeration<Object> e = props.keys();
			
	
		    while (e.hasMoreElements()) {
		      String key = (String) e.nextElement();
		      _log.error(key + " -- " + props.getProperty(key));
		    }*/
			
			try {
				
				
				String[] tags = fetchTags(entry.getTitleCurrentValue());
				
				if (tags != null && tags.length > 0) {
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setCompanyId(entry.getCompanyId());
				  
				//TODO loop over comma-separated items in case of multiple tags..
				String tagName = tags[0];
				_log.info("tagname: " + tagName);
				AssetTag assetTag;
				if (AssetTagLocalServiceUtil.hasTag(entry.getGroupId(), tagName)) {
					assetTag = AssetTagLocalServiceUtil.getTag(entry.getGroupId(), tagName);
				} else {
					_log.info("create tagname: " + tagName);
					assetTag = AssetTagLocalServiceUtil.addTag(entry.getUserId(), entry.getGroupId(), tagName, serviceContext);
				}
				
				
				
				
				long[] tagIds = { assetTag.getTagId() };
				
				_log.info("tag: " + assetTag.getName());
				
				
					_log.info("entryID: " + entry.getEntryId());
				
					// connect the tag to the asset
					AssetTagLocalServiceUtil.addAssetEntryAssetTag(entry.getEntryId(), assetTag);
					
				
				}
			} catch (Exception ex) {
				_log.error("Foutje: " + ex.getMessage());
			}
		}
	}

	/**
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 */
	private String[] fetchTags(String text) throws MalformedURLException, IOException, ProtocolException {
		String url = "http://localhost:8080/tagger-service/rest/tags";

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
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
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