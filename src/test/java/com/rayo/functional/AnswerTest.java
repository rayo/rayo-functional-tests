package com.rayo.functional;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.AnsweredEvent;

public class AnswerTest extends MohoBasedIntegrationTest {

	@Test
	public void testAnswer() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	     
	    assertReceived(AnsweredEvent.class, outgoing);
	}
}
