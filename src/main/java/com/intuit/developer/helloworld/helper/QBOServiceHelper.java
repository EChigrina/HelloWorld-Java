package com.intuit.developer.helloworld.helper;

import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QBOServiceHelper {
	
	@Autowired
	OAuth2PlatformClientFactory factory;
	
	private static final Logger logger = Logger.getLogger(QBOServiceHelper.class);

	public DataService getDataService(String realmId, String accessToken) throws FMSException {
		
    	String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";

		Config.setProperty(Config.BASE_URL_QBO, url);

		OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);

		Context context = new Context(oauth, ServiceType.QBO, realmId);

		return new DataService(context);
	}

	public static <T> List<T> executeQuery(DataService service, String query) throws FMSException {
		QueryResult queryResult = service.executeQuery(query);
		List<? extends IEntity> entities = queryResult.getEntities();
		List<T> objects = new ArrayList<>();
		if(!entities.isEmpty()) {
			for(Object o : entities) {
				objects.add((T)o);
			}
		}
		return objects;
	}
}
