package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.voxeo.rayo.client.JmxClient;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class UnavailablePresenceTest extends RayoBasedIntegrationTest {

	@Test
	// Cluster Docs. Scenario 8
	public void testUnavailablePresenceEndsActiveClientCalls() throws Exception {
		/*
		 * Test that when a client goes to unavailable status and there is active calls
		 * then these active calls are finished
		 */
		try {
			JmxClient gatewayClient = new JmxClient(rayoServer, "8080");

			JmxClient client = new JmxClient(getSipHostname(sipDialUri), "8080");
			long incomingCalls = getNodeIncomingCalls(client);			
			
			dial().getCallId();
			String incoming = getIncomingCall().getCallId();
			rayoClient.answer(incoming);			
			assertEquals(incomingCalls+1, getNodeIncomingCalls(client));
			
			long activeNodeCalls = getNodeActiveCalls(client);
			long activeGatewayCalls = getGatewayActiveCalls(gatewayClient);
			
			rayoClient.setAvailable(false); 
			waitForEvents(2000);
			
			// Two validations here. First incoming and perhaps also outgoing (depending on which node 
			// the dial command was dispatched to) calls should have been finished in the node. 
			// Second, the gateway will have two active calls less (both incoming and outgoing)
			assertTrue(activeNodeCalls > getNodeActiveCalls(client)); // 
			assertEquals(activeGatewayCalls-2, getGatewayActiveCalls(gatewayClient));

		} finally {
			if (rayoClient != null && rayoClient.getXmppConnection().isConnected()) {
				rayoClient.disconnect();
			}
		}
	}	
	

}
