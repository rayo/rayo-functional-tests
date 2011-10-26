package com.rayo.functional;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;

public class JoinTest extends MohoBasedIntegrationTest {

	@Test
	public void testJoin() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    System.out.println("***" + incoming1);
	    incoming1.answer();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    System.out.println("***" + incoming2);
	    incoming2.join(incoming1, JoinType.BRIDGE, Direction.DUPLEX);
	    
	    
	    assertReceived(MohoJoinCompleteEvent.class, incoming1);
	    assertReceived(MohoJoinCompleteEvent.class, incoming2);
	}
}
