package com.rayo.functional.rayoapi;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

import com.voxeo.rayo.client.JmxClient;
import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.xmpp.stanza.Presence.Show;
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
		JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");
		boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
		assertFalse(quiesce);
	}
	
	@Test
	public void testCanQuiesce() throws Exception {
		
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");
		try {
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
			
			quiesceNode(nodeClient);
			quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);
	
			dequiesceNode(nodeClient);	
			quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
		} finally {
			dequiesceNode(nodeClient);
		}
	}
	
	@Test
	public void testCallsRejectedOnQuiesce() throws Exception {
		
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");

		try {
			quiesceNode(nodeClient);	
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);

			try {
				dial(new URI("sip:usera@"+node)).getCallId();
				fail("This should not work");
			} catch (XmppException xe) {
				assertEquals(xe.getMessage(), "This node has been quiesced");
			}
		} finally {
			dequiesceNode(nodeClient);
		}
	}
		
	@Test
	public void testServerComesBackAfterQuiesce() throws Exception {
		
		int nodes = getNodeNames().size();
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");
		
		try {
			quiesceNode(nodeClient);
			dequiesceNode(nodeClient);
			int nodes2 = getNodeNames().size();
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertFalse(quiesce);
			assertEquals(nodes, nodes2);
			
		} finally {
			dequiesceNode(nodeClient);
		}
	}
	
	
	@Test
	public void testQuiesceLetsActiveCallsFinish() throws Exception {
				
		String node = getNodeName();
		JmxClient nodeClient = new JmxClient(node, "8080", "rayo/jmx");
		String outgoingCall1 = dial(new URI("sip:usera@"+node)).getCallId();
		String incomingCall1 = getIncomingCall().getCallId();
		rayoClient.answer(incomingCall1);

		try {
			quiesceNode(nodeClient);	
			boolean quiesce = (Boolean)nodeClient.jmxValue("com.rayo:Type=Admin,name=Admin", "QuiesceMode");
			assertTrue(quiesce);

			try {
				dial(new URI("sip:usera@"+node)).getCallId();
				fail("This should not work");
			} catch (XmppException xe) {
				assertEquals(xe.getMessage(), "This node has been quiesced");
			}
			
			// but we still can process events on the other call
			rayoClient.output("hello", outgoingCall1);
			waitForEvents(300);
			rayoClient.hangup(outgoingCall1);
			waitForEvents(500);
			EndEvent end = assertReceived(EndEvent.class, outgoingCall1);
			assertEquals(end.getReason(), Reason.HANGUP);
			
		} finally {			
			dequiesceNode(nodeClient);
			rayoClient.hangup(outgoingCall1);
			waitForEvents();
		}
	}
}
