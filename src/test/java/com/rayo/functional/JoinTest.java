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
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
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
	    Input<Call> input2 = incoming2.input(new InputCommand(new SimpleGrammar("yes,no")));
	    outgoing2.output("yes"); // this goes to incoming1
	    outgoing1.output("yes"); // this goes to incoming2
	    
	    assertReceived(MohoInputCompleteEvent.class, incoming1);
	    assertReceived(MohoInputCompleteEvent.class, incoming2);
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore
	//TODO: #1579864
	public void testJoinBridgeFails() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    incoming1.join(outgoing2, JoinType.BRIDGE, Direction.DUPLEX);
	    	    	    
	    assertReceived(MohoJoinCompleteEvent.class, incoming1);
	    
	    Input<Call> input1 = outgoing1.input(new InputCommand(new SimpleGrammar("yes,no")));
	    Input<Call> input2 = outgoing2.input(new InputCommand(new SimpleGrammar("yes,no")));
	    waitForEvents();
	    incoming1.output("yes");
	    
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input1);
	    assertReceived(InputCompleteEvent.class, input2);
	    
	    incoming1.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore
	//TODO: #1579867
	public void joinOutgoingCalls() {
			
	    OutgoingCall outgoing1 = dial();	    	    
	    OutgoingCall outgoing2 = dial();
	    outgoing1.join(outgoing2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, outgoing1);
	    assertReceived(JoinCompleteEvent.class, outgoing2);
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void joinCallsRecvMode() {
		
	    dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.RECV);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore
	public void testMediaWithCallsJoinedOnRecvMode() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.RECV);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    // Assert leg does receive media
	    Input<Call> input1 = incoming1.input("yes,no");
	    outgoing2.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input1);	 
	    
	    // Asserts the other leg does not receive media
	    Input<Call> input2 = incoming2.input("yes,no");
	    outgoing1.output("yes");
	    waitForEvents();
	    assertNotReceived(InputCompleteEvent.class, input2);   
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}	
}
