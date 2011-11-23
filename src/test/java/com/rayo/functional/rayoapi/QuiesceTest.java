package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rayo.client.JmxClient;
import com.rayo.functional.base.RayoBasedIntegrationTest;

/**
 * Tests Rayo Server Quiesce operations
 * 
 * @author martin
 *
 */
public class QuiesceTest extends RayoBasedIntegrationTest {
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}
	
	@After
	public void shutdown() throws Exception {}
	
	
	@Test
	public void testCanQueryQuiesceStatus() throws Exception {
		
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080");
		boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
		assertFalse(quiesce);
	}
	
	@Test
	public void testCanQuiesce() throws Exception {
		
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080");
		try {
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
			
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");		
			quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);
	
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");		
			quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
		} finally {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");		
		}
	}
	
	@Test
	public void testCallsRejectedOnQuiesce() throws Exception {
		
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080");

		try {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");		
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);

			try {
				dial(new URI("sip:usera@"+node));
				fail("Expected Exception");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");		
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
