package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.functional.base.RayoBasedIntegrationTest;


public class RayoMixerApiTest extends RayoBasedIntegrationTest {

	@Test
	public void testJoinAndUnjoin() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		IQ iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	

	@Test
	public void testJoinAndUnjoinMultipleCalls() throws Exception {
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();		
		rayoClient.answer(incoming1);
		waitForEvents();

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();		
		rayoClient.answer(incoming2);
		waitForEvents();

		String outgoing3 = dial().getCallId();
		String incoming3 = getIncomingCall().getCallId();		
		rayoClient.answer(incoming3);
		waitForEvents();

		IQ iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming3);
		assertTrue(iq.isResult());
		
		waitForEvents(3000);
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming3);
		assertTrue(iq.isResult());
		
		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		rayoClient.hangup(outgoing3);
		waitForEvents(3000);
	}
	
	@Test
	//TODO: #1578848
	public void testOutputOnMixer() throws Exception {
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		IQ iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join("1234", "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		waitForEvents();
		rayoClient.input("yes,no", incoming1);
		rayoClient.input("yes,no", incoming2);

		waitForEvents();
		rayoClient.output("yes", "1234");
		
		waitForEvents(1000);
		// Expect input completes. Does not work
		assertReceived(InputCompleteEvent.class, incoming1);
		assertReceived(InputCompleteEvent.class, incoming2);
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
		
	}
	
}
