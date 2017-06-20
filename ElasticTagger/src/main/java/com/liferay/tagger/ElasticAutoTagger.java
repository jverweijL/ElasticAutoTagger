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

/*import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;*/
import java.net.InetAddress;
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

	@Override
	public void onAfterCreate(AssetEntry entry) throws ModelListenerException {
		super.onAfterCreate(entry);
		
		if (entry.getClassName().equalsIgnoreCase(JournalArticle.class.getName())) {
			//_log.error("TAG ARTICLE " + model.getTitle());
			
			/*Properties props = PropsUtil.getProperties();
		      Enumeration<Object> e = props.keys();
			
	
		    while (e.hasMoreElements()) {
		      String key = (String) e.nextElement();
		      _log.error(key + " -- " + props.getProperty(key));
		    }*/
			
			try {
				// doesn't seem to work due to OSGI no able to get to elastic libs
				//_log.error("create ELS client");
				//Client client = TransportClient.builder().build()
				//        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9200));
	
				// on shutdown
				//_log.error("close ELS client");
				//client.close();
				
				/*String url = "http://localhost:9200/liferay-autotagger/my-type/_percolate'";
	
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setDoOutput(true);
				con.setDoInput(true);
				// optional default is GET
				con.setRequestMethod("GET");
				
				OutputStream os = con.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
				osw.write("{\"doc\" : {\"message\" : \"A new bonsai tree in the office\"}}");
				osw.flush();
				osw.close();
				
				
				int responseCode = con.getResponseCode();
				
				_log.error("Responsecode: " + responseCode);
				
				con.disconnect();*/			
				
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setCompanyId(entry.getCompanyId());
				  
				//TODO get suggestion(s) from Elastic or other service..
				String tagName = "tennis";
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
				
				//AssetEntryQuery assetEntryQuery = new AssetEntryQuery();
				//assetEntryQuery.setAnyTagIds(tagIds);
				//assetEntryQuery.setClassName(JournalArticle.class.getName());
				//assetEntryQuery.setEnd(100);
				//TODO limit to speicific user
				//AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(model.getGroupId(), model.getUuid());
				//List<AssetEntry> assetEntryList = AssetEntryLocalServiceUtil.getEntries(assetEntryQuery);
				//for (AssetEntry assetEntry : assetEntryList) {
					_log.info("entryID: " + entry.getEntryId());
				//}
					//Here all the logic will go
					//_log.error(assetEntry.getEntryId() + " | " + assetEntry.getPrimaryKey() + " | " + assetEntry.getUserId() + " | " + assetEntry.getGroupId());
					//_log.info("PK:" + assetEntry.getClassPK() + " | " + model.getClassPK());
					//assentEntry getClassPK() with JournalArticleLocalServiceUtil.getLatestArticle(classPK)
					//_log.info("let's connect");
					// connect the tag to the asset
					AssetTagLocalServiceUtil.addAssetEntryAssetTag(entry.getEntryId(), assetTag);
					
				//}
				
				//_log.error(entry.getContent());
			} catch (Exception ex) {
				_log.error("Foutje: " + ex.getMessage());
			}
		}
	}
}