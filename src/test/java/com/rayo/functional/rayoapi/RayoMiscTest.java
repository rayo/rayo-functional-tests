package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoMiscTest extends RayoBasedIntegrationTest {
	
	@Test
	public void testFailToUnmuteAnUnansweredCall() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);

    	rayoClient.unmute(incomingCallId);
    	Thread.sleep(100);
    	com.voxeo.rayo.client.xmpp.stanza.Error error = getLastError(incomingCallId);
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
    	com.voxeo.rayo.client.xmpp.stanza.Error error = getLastError(incomingCallId);
    	assertNotNull(error);
    	assertTrue(error.getText().contains("Call has not been answered"));

    	rayoClient.hangup(outgoingCallId);
	}
}
