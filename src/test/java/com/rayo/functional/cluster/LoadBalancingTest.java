package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.client.JmxClient;
import com.rayo.client.RayoClient;
import com.rayo.client.filter.XmppObjectExtensionNameFilter;
import com.rayo.client.filter.XmppObjectFilter;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class LoadBalancingTest extends RayoBasedIntegrationTest {

	Logger log = LoggerFactory.getLogger(LoadBalancingTest.class);
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}

	@Test
	public void testNodesEvenlyLoadBalanced() throws Exception {

		String firstNode = getNodeName(0);
		String secondNode = getNodeName(1);
		JmxClient node1Client = new JmxClient(firstNode, "8080");
		long node1Dials = getOutgoingCalls(node1Client);
		JmxClient node2Client = new JmxClient(secondNode, "8080");		
		long node2Dials = getOutgoingCalls(node2Client);
		
		List<String> dialed = new ArrayList<String>();
		try {
			rayoClient = new RayoClient(xmppServer, rayoServer);
			rayoClient.connect(xmppUsername, xmppPassword,"loadbalance");
			
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			
			assertEquals(getOutgoingCalls(node1Client), node1Dials + 3);
			assertEquals(getOutgoingCalls(node2Client), node2Dials + 3);
			
		} finally {
			for (String callid: dialed) {
				disconnect(callid);
			}
		}
	}
	
	@Test
	public void testLoadIsweighted() throws Exception {

		String firstNode = getNodeName(0);
		String secondNode = getNodeName(1);
		JmxClient node1Client = new JmxClient(firstNode, "8080");
		long node1Dials = getOutgoingCalls(node1Client);
		JmxClient node2Client = new JmxClient(secondNode, "8080");		
		long node2Dials = getOutgoingCalls(node2Client);
		
		// node1 should now get twice the load
		node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "weight", 20);
		
		// Wait for the presence to be broadcasted to the gateway so the routing 
		// engine gets updated
		waitForEvents(2000);
		
		List<String> dialed = new ArrayList<String>();
		try {
			rayoClient = new RayoClient(xmppServer, rayoServer);
			rayoClient.connect(xmppUsername, xmppPassword,"loadbalance");
			
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			
			assertEquals(getOutgoingCalls(node1Client), node1Dials + 4);
			assertEquals(getOutgoingCalls(node2Client), node2Dials + 2);
			
		} finally {
			node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "weight", 10);
			
			for (String callid: dialed) {
				disconnect(callid);
			}
		}
	}
	
	
	@Test
	public void testPriorityIsConsideredWhenLoadBalancing() throws Exception {

		String firstNode = getNodeName(0);
		String secondNode = getNodeName(1);
		JmxClient node1Client = new JmxClient(firstNode, "8080");
		long node1Dials = getOutgoingCalls(node1Client);
		JmxClient node2Client = new JmxClient(secondNode, "8080");		
		long node2Dials = getOutgoingCalls(node2Client);
		
		// node1 will not get any calls
		node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "priority", 2);
		
		// Wait for the presence to be broadcasted to the gateway so the routing 
		// engine gets updated
		waitForEvents(2000);
		
		List<String> dialed = new ArrayList<String>();
		try {
			rayoClient = new RayoClient(xmppServer, rayoServer);
			rayoClient.connect(xmppUsername, xmppPassword,"loadbalance");
			
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			dialed.add(rayoClient.dial(new URI(sipDialUri)).getCallId());
			waitForEvents();
			
			assertEquals(getOutgoingCalls(node1Client), node1Dials);
			assertEquals(getOutgoingCalls(node2Client), node2Dials + 6);
			
		} finally {
			node1Client.jmxExec("com.rayo:Type=Admin,name=Admin", "priority", 1);
			
			for (String callid: dialed) {
				disconnect(callid);
			}
		}
	}
	
	@Test
	public void testResourcesLoadBalancing() throws Exception {
		
		RayoClient rayoClient1 = null;
		RayoClient rayoClient2 = null;
		RayoClient rayoClient3 = null;
		
		List<String> calls = new ArrayList<String>();	
		try {
			rayoClient1 = new RayoClient(xmppServer, rayoServer);
			rayoClient1.connect(xmppUsername, xmppPassword, "resource8");
			rayoClient2 = new RayoClient(xmppServer, rayoServer);
			rayoClient2.connect(xmppUsername, xmppPassword, "resource9");			
			rayoClient3 = new RayoClient(xmppServer, rayoServer);
			rayoClient3.connect(xmppUsername, xmppPassword, "resource10");			
			
			waitForEvents(2000);
			
			// one offer filter per clint
			XmppObjectFilter filter1 = new XmppObjectExtensionNameFilter("offer");
			rayoClient1.addFilter(filter1);
			XmppObjectFilter filter2 = new XmppObjectExtensionNameFilter("offer");
			rayoClient2.addFilter(filter2);
			XmppObjectFilter filter3 = new XmppObjectExtensionNameFilter("offer");
			rayoClient3.addFilter(filter3);

			for (int i=0;i<6;i++) {
				calls.add(rayoClient1.dial(new URI(sipDialUri)).getCallId());
			}
			waitForEvents(1000);
			
			// First resource gets 2
			assertNotNull(filter1.poll());
			assertNotNull(filter1.poll());
			assertNull(filter1.poll(100));

			// Second resource gets another 2
			assertNotNull(filter2.poll());
			assertNotNull(filter2.poll());
			assertNull(filter2.poll(100));

			// And last one gets another 2
			assertNotNull(filter3.poll());
			assertNotNull(filter3.poll());
			assertNull(filter3.poll(100));
			
			for (int i=0;i<6;i++) {
				disconnect(calls.get(i));
				waitForEvents(100);
			}
			calls.clear();
			
			// Remove one of the resources
			rayoClient3.setAvailable(false);
			if (rayoClient3 != null && rayoClient3.getXmppConnection().isConnected()) {
				rayoClient3.disconnect();
			}
			waitForEvents(1000);
			
			for (int i=0;i<4;i++) {
				calls.add(rayoClient1.dial(new URI(sipDialUri)).getCallId());
			}
			// First resource gets 2
			assertNotNull(filter1.poll());
			assertNotNull(filter1.poll());			
			assertNull(filter1.poll(100));

			// Second resource gets another 2
			assertNotNull(filter2.poll());
			assertNotNull(filter2.poll());
			assertNull(filter2.poll(100));
		} finally {
			for (int i=0;i<4;i++) {
				disconnect(calls.get(i));
			}
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
	
	protected long getOutgoingCalls(JmxClient client) throws Exception {
		
		return ((Long)client.jmxValue("com.rayo:Type=CallStatistics", "OutgoingCalls"));
	}
}
