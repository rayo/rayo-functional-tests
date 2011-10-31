package com.rayo.functional;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.AnsweredEvent;
import com.voxeo.moho.event.CallCompleteEvent.Cause;

public class HangupTest extends MohoBasedIntegrationTest {

	@Test
	public void testAnswerAndHangup() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    	    
	    assertReceived(AnsweredEvent.class, outgoing);
	    
	    incoming.hangup();

	    MohoCallCompleteEvent endIncoming = assertReceived(MohoCallCompleteEvent.class, incoming);
	    assertEquals(endIncoming.getCause(), Cause.DISCONNECT);
	    
	    MohoCallCompleteEvent endOutgoing = assertReceived(MohoCallCompleteEvent.class, outgoing);
	    assertEquals(endOutgoing.getCause(), Cause.DISCONNECT);
	}
	
	@Test
	public void testAcceptAndHangup() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.accept();
	    
	    waitForEvents(1000);
	    incoming.hangup();

	    MohoCallCompleteEvent endIncoming = assertReceived(MohoCallCompleteEvent.class, incoming);
	    assertEquals(endIncoming.getCause(), Cause.DISCONNECT);
	    
	    MohoCallCompleteEvent endOutgoing = assertReceived(MohoCallCompleteEvent.class, outgoing);
	    assertEquals(endOutgoing.getCause(), Cause.DECLINE);		
	}

	@Test
	public void testAnswerAndLetFarsideHangup() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    	    
	    assertReceived(AnsweredEvent.class, outgoing);
	    
	    outgoing.hangup();

	    MohoCallCompleteEvent endIncoming = assertReceived(MohoCallCompleteEvent.class, incoming);
	    assertEquals(endIncoming.getCause(), Cause.DISCONNECT);
	    
	    MohoCallCompleteEvent endOutgoing = assertReceived(MohoCallCompleteEvent.class, outgoing);
	    assertEquals(endOutgoing.getCause(), Cause.DISCONNECT);
	}		
}
