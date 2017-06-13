package com.liferay.tagger;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetTagLocalServiceUtil;
import com.liferay.asset.kernel.service.persistence.AssetEntryQuery;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;

import java.net.InetAddress;
import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
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
public class ElasticAutoTagger extends BaseModelListener<JournalArticle> {
	
	//TODO make this dynamic with elastic perculator
	String autoTag = "autotag";
	
	private static Log _log = LogFactoryUtil.getLog(ElasticAutoTagger.class);

	@Override
	public void onAfterCreate(JournalArticle model) throws ModelListenerException {
		super.onAfterCreate(model);
		
		_log.error("TAG ARTICLE " + model.getTitle());
		
		/*Properties props = PropsUtil.getProperties();
	      Enumeration<Object> e = props.keys();
		

	    while (e.hasMoreElements()) {
	      String key = (String) e.nextElement();
	      _log.error(key + " -- " + props.getProperty(key));
	    }*/
		
		try {
			_log.error("create ELS client");
			Client client = TransportClient.builder().build()
			        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9200));

			// on shutdown
			_log.error("close ELS client");
			client.close();
			
			
			AssetTag assetTag = AssetTagLocalServiceUtil.getTag(model.getGroupId(), autoTag);
			long[] tagIds = { assetTag.getTagId() };
			
			AssetEntryQuery assetEntryQuery = new AssetEntryQuery();
			assetEntryQuery.setAnyTagIds(tagIds);
			assetEntryQuery.setClassName(JournalArticle.class.getName());
			//TODO limit to speicific user
			
			List<AssetEntry> assetEntryList = AssetEntryLocalServiceUtil.getEntries(assetEntryQuery);
			for (AssetEntry assetEntry : assetEntryList) {
				//Here all the logic will go
				_log.error(assetEntry.getEntryId() + " | " + assetEntry.getPrimaryKey() + " | " + assetEntry.getUserId() + " | " + assetEntry.getGroupId());
			}
			
			_log.error(model.getContent());
		} catch (Exception ex) {
			_log.error(ex.getMessage());
		}
	}
}