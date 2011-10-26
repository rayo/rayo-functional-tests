package com.rayo.functional;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent.Cause;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

public class InputTest extends MohoBasedIntegrationTest {

	@Test
	public void testInput() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Input<Call> input = incoming.input(new InputCommand(new SimpleGrammar("yes,no")));
	    waitForEvents();
	    outgoing.output("yes");
	    
	    waitForEvents(2000);	    
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.MATCH);
	    assertEquals(complete.getInterpretation(),"yes");
	}
}
