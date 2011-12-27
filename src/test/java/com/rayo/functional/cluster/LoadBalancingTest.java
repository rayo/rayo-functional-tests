package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.client.JmxClient;
import com.rayo.client.RayoClient;
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
	
	protected long getOutgoingCalls(JmxClient client) throws Exception {
		
		return ((Long)client.jmxValue("com.rayo:Type=CallStatistics", "OutgoingCalls"));
	}
}
