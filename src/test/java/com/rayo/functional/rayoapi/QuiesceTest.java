package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

import com.rayo.client.JmxClient;
import com.rayo.client.xmpp.stanza.Presence.Show;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.functional.base.RayoBasedIntegrationTest;

/**
 * Tests Rayo Server Quiesce operations
 * 
 * @author martin
 *
 */
public class QuiesceTest extends RayoBasedIntegrationTest {
		
	protected Show status;


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
			waitForEvents();
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

			String outgoingCall = dial(new URI("sip:usera@"+node)).getCallId();
			waitForEvents();
			EndEvent end = assertReceived(EndEvent.class, outgoingCall);
			assertEquals(end.getReason(), Reason.BUSY);
		} finally {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");		
			waitForEvents();
		}
	}
	
	@Test
	public void testServerGoesAwayOnQuiesce() throws Exception {
				
		int nodes = getNodeNames().size();
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080");

		try {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
			waitForEvents(300);
			int nodes2 = getNodeNames().size();
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);
			assertEquals(nodes-1, nodes2);
			
		} finally {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			waitForEvents();
		}
	}
	
	
	@Test
	public void testServerComesBackAfterQuiesce() throws Exception {
		
		int nodes = getNodeNames().size();
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080");
		
		try {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");
			waitForEvents(300);
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			waitForEvents(300);
			int nodes2 = getNodeNames().size();
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
			assertEquals(nodes, nodes2);
			
		} finally {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");	
			waitForEvents();
		}
	}
	
	
	@Test
	public void testQuiesceLetsActiveCallsFinish() throws Exception {
				
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080");
		String outgoingCall1 = dial(new URI("sip:usera@"+node)).getCallId();
		String incomingCall1 = getIncomingCall().getCallId();
		rayoClient.answer(incomingCall1);

		try {
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "enableQuiesce");		
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);

			String outgoingCall2 = dial(new URI("sip:usera@"+node)).getCallId();
			waitForEvents();
			EndEvent end = assertReceived(EndEvent.class, outgoingCall2);
			assertEquals(end.getReason(), Reason.BUSY);
			
			// but we still can process events on the other call
			rayoClient.output("hello", outgoingCall1);
			waitForEvents(300);
			rayoClient.hangup(outgoingCall1);
			waitForEvents(500);
			end = assertReceived(EndEvent.class, outgoingCall1);
			assertEquals(end.getReason(), Reason.HANGUP);
			
		} finally {			
			nodeClient.jmxExec("com.rayo:Type=Admin,name=Admin", "disableQuiesce");
			rayoClient.hangup(outgoingCall1);
			waitForEvents();
		}
	}
}
