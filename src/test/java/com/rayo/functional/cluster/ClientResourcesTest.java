package com.rayo.functional.cluster;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

import com.rayo.client.JmxClient;
import com.rayo.client.RayoClient;
import com.rayo.client.xmpp.stanza.Presence.Show;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class ClientResourcesTest extends RayoBasedIntegrationTest {

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
			System.out.println(applications);
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

			rayoClient2.setAvailable(false);
			Thread.sleep(1000);
			JmxClient client = new JmxClient(rayoServer, "8080");
			JSONArray applications = (JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ClientApplications");
			System.out.println(applications);
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
			System.out.println(applications);
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
			System.out.println(applications);
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
