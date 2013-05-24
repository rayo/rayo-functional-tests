package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.IQ;


public class RayoDtmfTest extends RayoBasedIntegrationTest {
	
	@Test
	public void testSendDtmf() throws Exception {
		
		dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    IQ result = rayoClient.dtmf("1", incomingCall);
	    assertNotNull(result);
	    assertTrue(result.isResult());	    
	}
	
	@Test
	public void testInvalidDtmf() throws Exception {
		
		dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    IQ result = rayoClient.dtmf("a", incomingCall);
	    assertNotNull(result);
	    assertTrue(result.isError());	    
	    assertEquals(result.getError().getCondition(), Condition.bad_request);
	    assertTrue(result.getError().getText().contains("Invalid DTMF key"));
	}	
	
}