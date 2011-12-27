package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.client.JmxClient;
import com.rayo.client.XmppException;
import com.rayo.client.filter.XmppObjectExtensionNameFilter;
import com.rayo.client.filter.XmppObjectFilter;
import com.rayo.client.xmpp.stanza.Error;
import com.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class DialTest extends RayoBasedIntegrationTest {

	private Logger log = LoggerFactory.getLogger(DialTest.class);
	
	// Cluster Docs. Scenario 10
	@Test
	public void testDialsAreLoadBalanced() throws Exception {

		String call1 = null;
		String call2 = null;
		String call3 = null;
		String call4 = null;
		try {
			call1 = dial().getCallId();
			call2 = dial().getCallId();
			call3 = dial().getCallId();
			call4 = dial().getCallId();
	
			JmxClient client = new JmxClient(rayoServer, "8080");
			Map<String, Integer> nodesCount = new HashMap<String, Integer>();
			
			JSONObject info1 = (JSONObject)client.jmxExec("com.rayo.gateway:Type=Gateway", "callInfo", call1);
			updateCount(nodesCount,info1);
			JSONObject info2 = (JSONObject)client.jmxExec("com.rayo.gateway:Type=Gateway", "callInfo", call2);
			updateCount(nodesCount,info2);
			JSONObject info3 = (JSONObject)client.jmxExec("com.rayo.gateway:Type=Gateway", "callInfo", call3);
			updateCount(nodesCount,info3);
			JSONObject info4 = (JSONObject)client.jmxExec("com.rayo.gateway:Type=Gateway", "callInfo", call4);
			updateCount(nodesCount,info4);
			
			JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
			
			if (nodes.size() == 1) {
				// not common just for the sake of solo testing			
				assertEquals(nodesCount.values().iterator().next().intValue(), 4);
			} else if (nodes.size() == 2) {
				Iterator<JSONObject> it = nodes.iterator();
				while(it.hasNext()) {
					JSONObject json = it.next();
					String jid = (String)json.get("JID");
					assertNotNull(nodesCount.get(jid));
					
					// With a two nodes rayo cluster load should be evenly balanced
					assertEquals(nodesCount.get(jid).intValue(), 2);
				}
	
			} else if (nodes.size() > 2) {
				// not common just for the sake of testing
				assertTrue(nodesCount.keySet().size() > 2);
			} else {
				fail("Expected some nodes");
			}
		} finally {
			disconnect(call1);
			disconnect(call2);
			disconnect(call3);
			disconnect(call4);
		}		
	}
	
	// Cluster Docs. Scenario 11
	@Test
	public void testDialWhenNoAvailableNodes() throws Exception {

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
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
		}
		
		Thread.sleep(1000);
		
		try {
			XmppObjectFilter filter = new XmppObjectExtensionNameFilter("error");
			rayoClient.addFilter(filter);
			try {
				dial();
				fail("Expected exception on dial. No rayo nodes should be available.");
			} catch (XmppException xe) {
				Error error = xe.getError();
				assertNotNull(error);
				assertEquals(error.getCondition(), Condition.service_unavailable);
			}
			
			
		} finally {
			for (String node: nodesList) {
				JmxClient nodeClient = new JmxClient(node, "8080");
				nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			}			
		}
	}

	private void updateCount(Map<String, Integer> nodesCount, JSONObject info1) {

		String node = (String)info1.get("rayoNode");
		if (node != null) {
			Integer count = nodesCount.get(node);
			if (count == null) {
				count = 0;
			}
			nodesCount.put(node, count+1);
		}
	}
}
