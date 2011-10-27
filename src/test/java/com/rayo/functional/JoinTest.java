package com.rayo.functional;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.MohoInputCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

public class JoinTest extends MohoBasedIntegrationTest {

	@Test
	public void testJoinBridge() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.join(incoming1, JoinType.BRIDGE, Direction.DUPLEX);
	    
	    assertReceived(MohoJoinCompleteEvent.class, incoming1);
	    assertReceived(MohoJoinCompleteEvent.class, incoming2);
	    
	    Input<Call> input1 = incoming1.input(new InputCommand(new SimpleGrammar("yes,no")));
	    incoming2.output("yes");
	    outgoing1.output("yes");
	    outgoing2.output("yes");

	    // Asserting, but is not received
	    assertReceived(MohoInputCompleteEvent.class, incoming1);
	}
	
	@Test
	@Ignore
	public void testJoinBridgeFails() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    
	    OutgoingCall outgoing2 = dial();
	    incoming1.join(outgoing2, JoinType.BRIDGE, Direction.DUPLEX);
	    	    	    
	    assertReceived(MohoJoinCompleteEvent.class, incoming1);
	    
	    Input<Call> input1 = outgoing1.input(new InputCommand(new SimpleGrammar("yes,no")));
	    Input<Call> input2 = outgoing2.input(new InputCommand(new SimpleGrammar("yes,no")));
	    incoming1.output("yes");
	    
	    try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
