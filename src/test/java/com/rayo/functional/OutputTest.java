package com.rayo.functional;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Output;

public class OutputTest extends MohoBasedIntegrationTest {

	@Test
	public void testOutput() {
		
	    dial();
	    
	    IncomingCall call = getIncomingCall();
	    assertNotNull(call);
	    call.answer();
	    
	    Output<Call> output = call.output("Hello World");
	    waitForEvents();
	    assertReceived(OutputCompleteEvent.class, output);
	}
}
