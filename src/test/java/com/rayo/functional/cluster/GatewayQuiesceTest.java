package com.rayo.functional.cluster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rayo.client.JmxClient;
import com.rayo.functional.base.RayoBasedIntegrationTest;

/**
 * Tests Gateway Quiesce operations
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
	@Ignore
	public void testCanQueryQuiesceStatus() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		boolean quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
		assertFalse(quiesce);
		
		client.jmxExec("com.rayo.gateway:Type=Gateway", "ban", xmppUsername+"@"+xmppServer);				
	}
	
	@Test
	@Ignore
	public void testCanQuiesce() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		try {
			boolean quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
			
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "enableQuiesce");		
			quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);
	
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "disableQuiesce");		
			quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
		} finally {
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "disableQuiesce");		
		}
	}
	
	@Test
	@Ignore
	public void testCallsRejectedOnQuiesce() throws Exception {
		
		JmxClient client = new JmxClient(rayoServer, "8080");
		try {
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "enableQuiesce");		
			boolean quiesce = (Boolean)client.jmxValue("com.rayo.gateway:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);

			try {
				dial();
				fail("Expected Exception");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			client.jmxExec("com.rayo.gateway:Type=Admin,name=Admin", "disableQuiesce");		
		}
	}
	
	@Test
	@Ignore
	//TODO
	public void testUnavailablePresenceOnQuiesce() throws Exception {
		
	}
	
	
	@Test
	@Ignore
	//TODO
	public void testAvailablePresenceOnQuiesce() throws Exception {
		
	}
	
	
	@Test
	@Ignore
	//TODO
	public void testQuiesceLetsActiveCallsEnd() throws Exception {
		
	}
}
