package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.voxeo.rayo.client.JmxClient;
import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.filter.XmppObjectExtensionNameFilter;
import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.Presence;
import com.voxeo.rayo.client.xmpp.stanza.Presence.Show;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class ClientResourcesTest extends RayoBasedIntegrationTest {

	private Logger log = LoggerFactory.getLogger(ClientResourcesTest.class);
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}
	
	@After
	public void shutdown() throws Exception {}
	
	
	// Cluster Docs. Scenario 1
	@Test
	public void testMultipleClientResources() throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource1");
			
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource2");
	
			waitForEvents(500);
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
			
			fail(String.format("No client application or statuses  found for [%s] - [%s]",xmppUsername+"@"+xmppServer, applications));
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
	
	// Cluster Docs. Scenario 2
	@Test
	public void testBusyFailover() throws Exception {
		
		doShowTest(Show.away);
	}	
	
	// Cluster Docs. Scenario 3
	@Test
	public void testDndFailover() throws Exception {
		
		doShowTest(Show.dnd);
	}	
	
	// Cluster Docs. Scenario 4
	@Test
	public void testXaFailover() throws Exception {
		
		doShowTest(Show.xa);
	}	
	
	
	// Cluster Docs. Scenario 5
	@Test
	public void testShowChatBackAvailable() throws Exception {
		
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource1");
			
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource2");

			rayoClient2.setStatus(Show.away);
			Thread.sleep(1000);
			rayoClient2.setStatus(Show.chat);
			Thread.sleep(1000);
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
			
			fail(String.format("No client application or statuses  found for [%s] - [%s]",xmppUsername+"@"+xmppServer, applications));
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
	
	// Cluster Docs. Scenario 6
	@Test
	public void testUnavailableFailover() throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource1");
			
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource2");

			rayoClient2.setAvailable(false); // broadcasts unavailable presence
			Thread.sleep(1000);
			JmxClient client = new JmxClient(rayoServer, "8080");
			JSONArray applications = (JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ClientApplications");
			Iterator<JSONObject> it = applications.iterator();
			while(it.hasNext()) {
				JSONObject json = it.next();
				if (json.get("JID").equals(xmppUsername+"@"+xmppServer)) {
					JSONArray resources = (JSONArray)json.get("resources");
					assertTrue(resources.contains("resource1"));
					assertFalse(resources.contains("resource2"));
					return; // all good
				}				
			}
			
			fail(String.format("No client application or statuses  found for [%s] - [%s]",xmppUsername+"@"+xmppServer, applications));
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
	
	// Cluster Docs. Scenario 7
	@Test
	public void testUnavailableFailoverAndBackAvailable() throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource1");
			
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource2");

			rayoClient2.setAvailable(false);
			Thread.sleep(1000);
			rayoClient2.setAvailable(true);
			Thread.sleep(1000);
			
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
			
			fail(String.format("No client application or statuses  found for [%s] - [%s]",xmppUsername+"@"+xmppServer, applications));
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
	
	// Cluster Docs. Scenario 9
	@Test
	public void testInvalidClientApplication() throws Exception {
		
		RayoClient rayoClient = null;
		JmxClient client = null;
		int clients = getClientsConnected();
		try {
			client = new JmxClient(rayoServer, "8080");
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "ban", xmppUsername+"@"+xmppServer);
			
			rayoClient = new RayoClient(xmppServer, rayoServer);
			XmppObjectFilter filter = new XmppObjectExtensionNameFilter("error");
			rayoClient.addFilter(filter);
			
			rayoClient.connect(xmppUsername, xmppPassword, "resource1");
			Presence presence = (Presence)filter.poll(2000);
			assertNotNull(presence);			
			assertNotNull(presence.getError());
			assertEquals(presence.getError().getCondition(), Condition.recipient_unavailable);
			
			assertEquals(getClientsConnected(), clients);						
		} finally {
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "unban", xmppUsername+"@"+xmppServer);
		}
	}	
	
	
	// Cluster Docs. Scenario 12
	@Test
	public void testOffersBalancedAcrossResources() throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		
		String call1 = null;
		String call2 = null;
		String call3 = null;
		String call4 = null;
		
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource5");
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource6");			
			
			waitForEvents(2000);
			
			// one offer filter per clint
			XmppObjectFilter filter1 = new XmppObjectExtensionNameFilter("offer");
			rayoClient1.addFilter(filter1);
			XmppObjectFilter filter2 = new XmppObjectExtensionNameFilter("offer");
			rayoClient2.addFilter(filter2);

			// Now we dial 4 times from the same client and each of the filter 
			// should be hit twice which means each client got two offers
			call1 = rayoClient1.dial(new URI(sipDialUri)).getCallId();
			call2 = rayoClient1.dial(new URI(sipDialUri)).getCallId();
			call3 = rayoClient1.dial(new URI(sipDialUri)).getCallId();
			call4 = rayoClient1.dial(new URI(sipDialUri)).getCallId();			
			Thread.sleep(1000);
			
			assertNotNull(filter1.poll());
			assertNotNull(filter1.poll());
			assertNull(filter1.poll(100));

			assertNotNull(filter2.poll());
			assertNotNull(filter2.poll());
			assertNull(filter2.poll(100));
			
		} finally {
			disconnect(call1);
			disconnect(call2);
			disconnect(call3);
			disconnect(call4);
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
	
	private void doShowTest(Show status) throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource1");
			
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource2");

			rayoClient2.setStatus(status);
			Thread.sleep(1000);
			JmxClient client = new JmxClient(rayoServer, "8080");
			JSONArray applications = (JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ClientApplications");
			Iterator<JSONObject> it = applications.iterator();
			while(it.hasNext()) {
				JSONObject json = it.next();
				if (json.get("JID").equals(xmppUsername+"@"+xmppServer)) {
					JSONArray resources = (JSONArray)json.get("resources");
					assertTrue(resources.contains("resource1"));
					assertFalse(resources.contains("resource2"));
					return; // all good
				}				
			}
			
			fail(String.format("No client application or statuses  found for [%s] - [%s]",xmppUsername+"@"+xmppServer, applications));
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
