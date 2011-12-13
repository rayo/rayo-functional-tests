package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.client.JmxClient;
import com.rayo.client.RayoClient;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoNodeTest extends RayoBasedIntegrationTest {

	Logger log = LoggerFactory.getLogger(RayoNodeTest.class);
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}
		
	// Cluster Docs. Scenario 13
	@Test
	@Ignore
	public void testRayoNodesReceiveErrorIfNoClients() throws Exception {

		// We are going to cheat. We are going to connect directly to one of 
		// the rayo nodes and dial to generate an offer. That offer will be 
		// rejected by the gateway as there will not be any client application
		// listening for offers.
		//
		// It is not a perfect test as we should validate the type of error. Feel 
		// free to improve it in the future.
		
		String directUsername = "usera";
		String directPassword = "1";
				
		try {
			String server = getNodeName();
			
			rayoClient = new RayoClient(server, server);
			rayoClient.connect(directUsername, directPassword);
			
			int errors = getTotalNodePresenceErrors();
			assertEquals(getClientsConnected(),0);

			dial();
			Thread.sleep(1000);

			assertEquals(getClientsConnected(),0);
			int errors2 = getTotalNodePresenceErrors();
			
			// why two? Because the error presence sent from the gateway to the node due 
			// to the missing client for the offer message will generate an end event 
			// which will also fail to be delivered causing another presence error
			assertEquals(errors2, errors+2);
		} finally {
			if (rayoClient != null && rayoClient.getXmppConnection().isConnected()) {
				rayoClient.disconnect();
			}
		}
	}
	
	
	// Cluster Docs. Scenario 14
	@Test
	public void testRayoNodesHavePlatform() throws Exception {
	
		List<String> nodesList = new ArrayList<String>();
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
		Iterator<JSONObject> it = nodes.iterator();
		while(it.hasNext()) {
			JSONObject json = it.next();
			JSONArray platforms = (JSONArray)json.get("platforms");
			assertTrue(platforms.size() > 0);
		}
	}
	
	// Cluster Docs. Scenario 16
	@Test
	public void testRayoNodeNotAvailableAfterQuiesce() throws Exception {
	
		String node = getNodeName();
		
		int nodes = getNodes();
		
		JmxClient nodeClient = new JmxClient(node, "8080");

		try {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");			int nodes2 = getNodes();
			assertTrue(nodes2 == nodes-1);
		} finally {			
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
		}			
	}
	
	// Cluster Docs. Scenario 17
	@Test
	public void testRayoNodeBackAvailableAfterDequiesced() throws Exception {
	
		String node = getNodeName();
		
		int nodes = getNodes();
		JmxClient nodeClient = new JmxClient(node, "8080");
		try {			
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			
			int nodes2 = getNodes();
			assertEquals(nodes2,nodes);
		} finally {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
		}
	}
	
	
	// Cluster Docs. Scenario 18
	@Test
	public void testRayoNodeLoadBalancing() throws Exception {
	
		String firstNode = getNodeName(0);
		String secondNode = getNodeName(1);
		JmxClient node1Client = new JmxClient(firstNode, "8080");
		JmxClient node2Client = new JmxClient(secondNode, "8080");		
		
		try {
			long initialCalls = getTotalCalls();
			rayoClient = new RayoClient(xmppServer, rayoServer);
			rayoClient.connect(xmppUsername, xmppPassword,"loadbalance");
			
			rayoClient.dial(new URI(sipDialUri)).getCallId();
			assertEquals(getTotalCalls(), initialCalls+2);
			
			node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
			waitForEvents(500);
			// we need to dial the second node as we have quiesced the first one
			URI secondURI = new URI("sip:usera@" + secondNode);
			rayoClient.dial(secondURI).getCallId();
			waitForEvents();
			assertEquals(getTotalCalls(), initialCalls+4);
			
			node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			node2Client.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
			waitForEvents(500);
			// we need to dial the first node as we have quiesced the second one
			URI firstURI = new URI("sip:usera@" + firstNode);
			rayoClient.dial(firstURI).getCallId();
			waitForEvents();
			assertEquals(getTotalCalls(), initialCalls+6);
	
			node2Client.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			waitForEvents(500);
			rayoClient.dial(new URI(sipDialUri)).getCallId();
			waitForEvents();
			assertEquals(getTotalCalls(), initialCalls+8);
		} finally {
			node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			node2Client.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");			
		}
	}
	
	private int getTotalNodePresenceErrors() throws Exception {
		
		int totalNodeErrors = 0;
		List<String> nodesList = new ArrayList<String>();
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
		Iterator<JSONObject> it = nodes.iterator();
		while(it.hasNext()) {
			JSONObject json = it.next();
			String jid = (String)json.get("JID");
			nodesList.add(jid);
		}
		
		for (String node: nodesList) {
			JmxClient nodeClient = new JmxClient(node, "8080");
			Long errors = (Long)nodeClient.jmxValue("com.rayo:Type=Rayo", "PresenceErrorsReceived");
			if (errors != null) {
				totalNodeErrors+=errors.intValue();
			}
		}
		
		return totalNodeErrors;
	}
		
	private int getClientsConnected() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray clients = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "ClientApplications"));
		return clients.size();
	}

	private long getTotalCalls() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		return ((Long)client.jmxValue("com.rayo.gateway:Type=GatewayStatistics", "TotalCallsCount"));
	}
	
	private int getNodes() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
		return nodes.size();
	}
	
	private void disconnect(String call) {

		if (call != null) {
			try {
				rayoClient.hangup(call);
			} catch (Exception e) {
				log.error("ERROR: " + e.getMessage());
			}
		}
	}
}
