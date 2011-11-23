package com.rayo.functional;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;

public class CdrTest extends MohoBasedIntegrationTest {

	@Test
	@Ignore
	public void testShouldCreateCdrOnIncomingCall() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    
	    assertJmxNodeContains("com.rayo:Type=Cdrs", "ActiveCDRs", "callId",incoming.getId());	    
	    
	    outgoing.hangup();
	}

	@Test
	@Ignore
	public void testShouldCreateCdrOnOutgoingCall() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    getIncomingCall();
	    
	    assertCdrExists(outgoing.getId());	    
	    
	    outgoing.hangup();
	}
	
	@Test
	@Ignore
	public void testShouldCreateTranscript() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    incoming.answer();
	    
	    incoming.output("Hello").get();
	    
	    String value = getCdrTranscript(incoming.getId());
	    assertTrue(value.contains("<offer"));
	    assertTrue(value.contains("<answer"));
	    assertTrue(value.contains("<output"));
	    assertTrue(value.contains("<complete"));
	    assertTrue(value.contains("<success"));
	    
	    outgoing.hangup();
	}
}
