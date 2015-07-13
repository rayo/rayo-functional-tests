package com.rayo.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.event.RecordCompleteEvent.Cause;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Recording;
import com.voxeo.moho.media.record.RecordCommand;

public class RecordTest extends MohoBasedIntegrationTest {

	@Test
	public void testRecordEndsHangup() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    waitForEvents();
	    
	    Recording<Call> recording = incoming.record(new RecordCommand(null));
	    
	    Output<Call> output = outgoing.output("Hello World");
	    waitForEvents();
	    
	    assertReceived(OutputCompleteEvent.class, output);
	    
	    outgoing.hangup();
	    waitForEvents();
	    RecordCompleteEvent<?> complete =  assertReceived(RecordCompleteEvent.class, recording);
		//assertEquals(complete.getCause(), Cause.DISCONNECT);	
	    assertNotNull(complete.getCause());
	}
	
	@Test
	public void testRecordEndsStop() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    waitForEvents();
	    
	    Recording<Call> recording = incoming.record(new RecordCommand(null));
	    
	    Output<Call> output = outgoing.output("Hello World");
	    waitForEvents();
	    assertReceived(OutputCompleteEvent.class, output);
	    
	    recording.stop();

	    waitForEvents();
	    RecordCompleteEvent<?> complete = assertReceived(RecordCompleteEvent.class, recording);
		assertEquals(complete.getCause(), Cause.CANCEL);
	    
	    outgoing.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testCantRecordNonAnswered() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();	    
	    assertNotNull(incoming);

	    try {
	    	incoming.record(new RecordCommand(null));
	    	fail("Expected exception");
	    } catch(Exception e) {
	    	assertTrue(e.getMessage().contains("Media not available on this connection yet"));
	    } finally {
	    	Thread.sleep(100);	
	    	outgoing.hangup();
	    }
	}
}
