package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.voxeo.rayo.client.JmxClient;
import com.voxeo.rayo.client.RayoClient;
import com.rayo.core.verb.OutputCompleteEvent;
import com.rayo.core.verb.OutputCompleteEvent.Reason;
import com.rayo.functional.base.RayoBasedIntegrationTest;

/**
 * Tests Gateway's Quiesce operations
 * 
 * @author martin
 *
 */
public class GatewayQuiesceTest extends RayoBasedIntegrationTest {

	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}
	
	@After
	public void shutdown() throws Exception {}
	
	
	@Test
	public void testCanQueryQuiesceStatus() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		boolean quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
		assertFalse(quiesce);			
	}
	
	@Test
	public void testCanQuiesce() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		try {
			boolean quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
			
			quiesceGateway(client);	
			quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);
	
			dequiesceGateway(client);		
			quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
		} finally {
			dequiesceGateway(client);
		}
	}
	
	@Test
	public void testCallsRejectedOnQuiesce() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		try {
			quiesceGateway(client);	
			boolean quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);

			rayoClient = new RayoClient(xmppServer, rayoServer);
			rayoClient.connect(xmppUsername, xmppPassword, "resource1");
			String outgoing = null;
			try {
				outgoing = dial().getCallId();
				fail("Expected Exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Gateway Server is on Quiesce Mode"));
			} finally {
				rayoClient.hangup(outgoing);
				rayoClient.disconnect();
			}
		} finally {
			dequiesceGateway(client);	
		}
	}
	
	@Test
	@Ignore
	public void testQuiesceLetsActiveCallsEnd() throws Exception {
				
		JmxClient client = new JmxClient(rayoServer, "8080");
		rayoClient = new RayoClient(xmppServer, rayoServer);
		rayoClient.connect(xmppUsername, xmppPassword, "resource1");
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId(); 
		try {
			quiesceGateway(client);	
			
			try {
				dial();
				fail("Expected Exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Gateway Server is on Quiesce Mode"));
			}			
			
			rayoClient.output("hello", incoming1);
			waitForEvents();
			OutputCompleteEvent complete = assertReceived(OutputCompleteEvent.class, incoming1);
			assertEquals(complete.getReason(), Reason.SUCCESS);
			
		} finally {
			rayoClient.hangup(outgoing1);
			dequiesceGateway(client);	
		}
	}
}
