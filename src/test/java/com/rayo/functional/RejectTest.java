package com.rayo.functional;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.AcceptableEvent.Reason;
import com.voxeo.moho.event.CallCompleteEvent.Cause;

public class RejectTest extends MohoBasedIntegrationTest {

	@Test
	public void testRejectDecline() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.reject(Reason.DECLINE);
		    waitForEvents(200);
		    
		    MohoCallCompleteEvent complete1 = assertReceived(MohoCallCompleteEvent.class, outgoing);
		    assertEquals(complete1.getCause(), Cause.DECLINE);
		    MohoCallCompleteEvent complete2 = assertReceived(MohoCallCompleteEvent.class, incoming);
		    assertEquals(complete2.getCause(), Cause.DECLINE);
		    
	    } finally {
	    	outgoing.hangup();
	    }
	}
	
	@Test
	public void testRejectBusy() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.reject(Reason.BUSY);
		    waitForEvents(200);
		    
		    MohoCallCompleteEvent complete1 = assertReceived(MohoCallCompleteEvent.class, outgoing);
		    assertEquals(complete1.getCause(), Cause.BUSY);
		    MohoCallCompleteEvent complete2 = assertReceived(MohoCallCompleteEvent.class, incoming);
		    assertEquals(complete2.getCause(), Cause.DECLINE);
		    
	    } finally {
	    	outgoing.hangup();
	    }
	}
	
	
	@Test
	public void testRejectError() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.reject(Reason.ERROR);
		    waitForEvents(200);
		    
		    MohoCallCompleteEvent complete1 = assertReceived(MohoCallCompleteEvent.class, outgoing);
		    assertEquals(complete1.getCause(), Cause.ERROR);
		    MohoCallCompleteEvent complete2 = assertReceived(MohoCallCompleteEvent.class, incoming);
		    assertEquals(complete2.getCause(), Cause.DECLINE);
		    
	    } finally {
	    	outgoing.hangup();
	    }
	}
}
