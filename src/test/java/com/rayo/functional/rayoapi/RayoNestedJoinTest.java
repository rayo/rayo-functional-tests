package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.core.DialCommand;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.moho.Participant.JoinType;

public class RayoNestedJoinTest extends RayoBasedIntegrationTest {


	public void testNestedJoinDuplex() throws Exception {
		
		doTest(JoinType.BRIDGE, Direction.DUPLEX);
	}


	public void testNestedJoinSend() throws Exception {
		
		doTest(JoinType.BRIDGE, Direction.SEND);
	}


	public void testNestedJoinRecv() throws Exception {
		
		doTest(JoinType.BRIDGE, Direction.RECV);
	}


	public void testNestedJoinDirect() throws Exception {
		
		doTest(JoinType.DIRECT, Direction.DUPLEX);
	}


	public void testNestedJoinBridgeExclusive() throws Exception {
		
		doTest(JoinType.BRIDGE_EXCLUSIVE, Direction.DUPLEX);
	}


	public void testNestedJoinBridgeShared() throws Exception {
		
		doTest(JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	}
	

	public void testNestedJoinAndReject() throws Exception {
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		System.out.println(outgoing1 + " - " + incoming1);
		rayoClient.answer(incoming1);
		
		JoinCommand join = new JoinCommand();
		join.setTo(incoming1);
		join.setDirection(Direction.DUPLEX);
		join.setMedia(JoinType.BRIDGE_EXCLUSIVE);
		join.setType(JoinDestinationType.CALL);
		
		DialCommand dialCommand = new DialCommand();
		dialCommand.setTo(new URI("sip:usera@localhost"));
		dialCommand.setFrom(new URI("sip:test@localhost"));
		dialCommand.setJoin(join);
		
		String outgoing2 = rayoClient.dial(dialCommand).getCallId();
		String incoming2 = getIncomingCall().getCallId();
		System.out.println(outgoing2 + " - " + incoming2);
		rayoClient.reject(incoming2);
		
		EndEvent end = assertReceived(EndEvent.class, incoming2);
		assertEquals(end.getReason(), Reason.REJECT);
		end = assertReceived(EndEvent.class, outgoing2);
		assertEquals(end.getReason(), Reason.REJECT);
		
		waitForEvents();
		rayoClient.hangup(outgoing1);
		waitForEvents();
	}
	
	private void doTest(JoinType type, Direction direction) throws Exception {
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		System.out.println(outgoing1 + " - " + incoming1);
		rayoClient.answer(incoming1);
		
		JoinCommand join = new JoinCommand();
		join.setTo(incoming1);
		join.setDirection(direction);
		join.setMedia(type);
		join.setType(JoinDestinationType.CALL);
		
		DialCommand dialCommand = new DialCommand();
		dialCommand.setTo(new URI(sipDialUri));
		dialCommand.setFrom(new URI("sip:test@localhost"));
		dialCommand.setJoin(join);
		
		String outgoing2 = rayoClient.dial(dialCommand).getCallId();
		String incoming2 = getIncomingCall().getCallId();
		System.out.println(outgoing2 + " - " + incoming2);
		rayoClient.answer(incoming2);
		
		JoinedEvent joined = assertReceived(JoinedEvent.class, incoming1);
		assertEquals(joined.getTo(), outgoing2);
		joined = assertReceived(JoinedEvent.class, outgoing2);
		assertEquals(joined.getTo(), incoming1);
		
		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();
	}
	
}
