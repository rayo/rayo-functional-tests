package com.rayo.functional;

import static org.junit.Assert.assertNotNull;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

public class MultipleJoinTest extends MohoBasedIntegrationTest {

	@Test
	public void testOutputIsReceivedByMultipleCalls() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall call1 = getIncomingCall();
	    assertNotNull(call1);
	    call1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall call2 = getIncomingCall();
	    assertNotNull(call2);
	    call2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall call3 = getIncomingCall();
	    assertNotNull(call3);
	    call3.answer();

	    call1.join(call2, JoinType.BRIDGE_SHARED, true, Direction.DUPLEX);
	    call1.join(call3, JoinType.BRIDGE_SHARED, true, Direction.DUPLEX);
	    	    
	    InputCommand input = new InputCommand(new SimpleGrammar("yes,no"));
	    Input<Call> input2 = call2.input(input);
	    Input<Call> input3 = call3.input(input);
	    
	    outgoing1.output("yes");

	    assertReceived(InputCompleteEvent.class, input2);
	    assertReceived(InputCompleteEvent.class, input3);
	}
}
