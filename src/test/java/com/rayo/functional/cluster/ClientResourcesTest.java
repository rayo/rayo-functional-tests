package com.rayo.functional.cluster;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

import com.rayo.client.JmxClient;
import com.rayo.client.RayoClient;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class ClientResourcesTest extends RayoBasedIntegrationTest {

	// Cluster Docs. Scneario 1
	@Test
	public void testMultipleClientResources() throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource1");
			
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource2");
	
			JmxClient client = new JmxClient(rayoServer, "8080");
			JSONArray applications = (JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ClientApplications");
			
			Iterator<JSONObject> it = applications.iterator();
			while(it.hasNext()) {
				JSONObject json = it.next();
				if (json.get("JID").equals(xmppUsername+"@"+xmppServer)) {
					JSONArray resources = (JSONArray)json.get("resources");
					assertTrue(resources.contains("resource1"));
					assertTrue(resources.contains("resource2"));
					return; // all good
				}				
			}
			
			fail(String.format("No client application found for [%s]",xmppUsername+"@"+xmppServer));
		} finally {
			try {
				if (rayoClient1 != null && rayoClient1.getXmppConnection().isConnected()) {
					rayoClient1.disconnect();
				}
			} finally {
				if (rayoClient2 != null && rayoClient2.getXmppConnection().isConnected()) {
					rayoClient2.disconnect();
				}
			}
		}
	}
}
