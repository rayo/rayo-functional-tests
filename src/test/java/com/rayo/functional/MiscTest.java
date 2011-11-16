package com.rayo.functional;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;

public class MiscTest extends MohoBasedIntegrationTest {

	@Test
	public void testMuteAndUnmute() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.answer();
		    Thread.sleep(100);
		    incoming.mute();
		    
		    Thread.sleep(500);
		    
		    incoming.unmute();
	    } finally {
	    	outgoing.hangup();
	    }
	}
	
	@Test
	public void testHoldUnhold() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.answer();
		    Thread.sleep(100);
		    incoming.hold();
		    
		    Thread.sleep(500);
		    
		    incoming.unhold();
	    } finally {
	    	outgoing.hangup();
	    }
	}
}
