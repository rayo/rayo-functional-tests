package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rayo.client.XmppConnection;
import com.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Ping;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoMiscTest extends RayoBasedIntegrationTest {
	
	@Test
	public void testFailToUnmuteAnUnansweredCall() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);

    	rayoClient.unmute(incomingCallId);
    	Thread.sleep(100);
    	com.rayo.client.xmpp.stanza.Error error = getLastError(incomingCallId);
    	assertNotNull(error);
    	assertTrue(error.getText().contains("Call has not been answered"));

    	rayoClient.hangup(outgoingCallId);
	}
	
	@Test
	public void testFailToUnholdAnUnansweredCall() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);

    	rayoClient.unhold(incomingCallId);
    	Thread.sleep(100);
    	com.rayo.client.xmpp.stanza.Error error = getLastError(incomingCallId);
    	assertNotNull(error);
    	assertTrue(error.getText().contains("Call has not been answered"));

    	rayoClient.hangup(outgoingCallId);
	}
	
	@Test
	public void testSendCommandToGatewayWithInvalidAddress() throws Exception {

		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);

	    XmppConnection connection = rayoClient.getXmppConnection();
		IQ iq = new IQ(IQ.Type.get)
			.setFrom(buildFrom(connection))
			.setTo("crapurl@")
			.setChild(new Ping());
	    IQ result = (IQ)connection.sendAndWait(iq);
	    assertTrue(result.isError());
	    assertEquals(result.getError().getCondition(),Condition.jid_malformed);
	    
    	rayoClient.hangup(outgoingCallId);
	}
	
	@Test
	public void testSendCommandToRayoNodeWithInvalidAddress() throws Exception {

		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);

	    XmppConnection connection = rayoClient.getXmppConnection();
		IQ iq = new IQ(IQ.Type.get)
			.setFrom(buildFrom(connection))
			.setTo("crapurl@")
			.setChild(new Ping());
	    IQ result = (IQ)connection.sendAndWait(iq);
	    assertTrue(result.isError());
	    assertEquals(result.getError().getCondition(),Condition.jid_malformed);
	    
    	rayoClient.hangup(outgoingCallId);
	}
	
	private String buildFrom(XmppConnection connection) {
		
		return connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource();
	}
}
