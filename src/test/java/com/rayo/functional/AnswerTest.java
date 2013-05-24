package com.rayo.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.AnsweredEvent;
import com.voxeo.moho.event.CallCompleteEvent.Cause;

public class AnswerTest extends MohoBasedIntegrationTest {

	@Test
	public void testAnswer() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	     
	    assertReceived(AnsweredEvent.class, outgoing);
	}
	
	@Test
	public void testAnswerMultipleTimes() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    	    
	    assertReceived(AnsweredEvent.class, outgoing);
	    
	    incoming.answer();
	    waitForEvents();
	    incoming.hangup();

	    MohoCallCompleteEvent endIncoming = assertReceived(MohoCallCompleteEvent.class, incoming);
	    assertEquals(endIncoming.getCause(), Cause.DISCONNECT);
	    
	    MohoCallCompleteEvent endOutgoing = assertReceived(MohoCallCompleteEvent.class, outgoing);
	    assertEquals(endOutgoing.getCause(), Cause.DISCONNECT);
	}
		
}
