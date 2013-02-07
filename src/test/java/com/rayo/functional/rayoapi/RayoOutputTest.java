package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Test;

import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.verb.Output;
import com.rayo.core.verb.Ssml;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.moho.Participant.JoinType;

public class RayoOutputTest extends RayoBasedIntegrationTest {
/*
	@Test
	public void testCantOutputNonAnsweredCall() throws Exception {
		
		String outgoing = dial().getCallId();
		try {
			String incomingCallId = getIncomingCall().getCallId();
			
			try {
				rayoClient.output("hello",incomingCallId);
				fail("Expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("The call has not been answered"));
			}
		} finally {
			rayoClient.hangup(outgoing);
		}
	}
*/	
	@Test
	public void testOutputBroadcast() throws Exception {
		
		String outgoing = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		rayoClient.answer(incomingCallId);
		
		String outgoing2 = dial().getCallId();
		String incomingCallId2 = getIncomingCall().getCallId();
		rayoClient.answer(incomingCallId2);
		
		try {
			JoinCommand join = new JoinCommand();
			join.setTo(incomingCallId2);
			join.setDirection(Direction.DUPLEX);
			join.setMedia(JoinType.BRIDGE_EXCLUSIVE);
			join.setType(JoinDestinationType.CALL);
			rayoClient.join(join, incomingCallId);
			
			Output output = new Output();
			output.setBroadcast(true);
			output.setPrompt(new Ssml("hello"));
			rayoClient.output(output,incomingCallId);

		} finally {
			rayoClient.hangup(outgoing);
			rayoClient.hangup(outgoing2);
		}
	}
}
