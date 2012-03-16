package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.UUID;

import org.junit.Test;

import com.rayo.core.JoinDestinationType;
import com.rayo.core.OfferEvent;
import com.rayo.core.StartedSpeakingEvent;
import com.rayo.core.StoppedSpeakingEvent;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;


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
		rayoClient.input("yes,no", outgoing1);
		rayoClient.input("yes,no", outgoing2);

		waitForEvents();
		rayoClient.output("yes", "1234");
		
		waitForEvents(1000);
		// Expect input completes. Does not work
		assertReceived(InputCompleteEvent.class, outgoing1);
		assertReceived(InputCompleteEvent.class, outgoing2);
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}
	
	@Test
	public void testInputOnMixer() throws Exception {
		
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
		rayoClient.input("yes,no", "1234");
		waitForEvents(200);
		rayoClient.output("yes", outgoing1);
		waitForEvents(500);
		
		// Expect input completes
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, "1234");
		assertEquals(complete.getUtterance(), "yes");
		
		// the input is now gone
		rayoClient.output("yes", outgoing2);
		assertNotReceived(InputCompleteEvent.class, "1234");
		
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}
	
	
	@Test
	public void testHoldUnholdOnMixer() throws Exception {
		
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
		rayoClient.input("yes,no", outgoing1);
		rayoClient.input("yes,no", outgoing2);
		
		rayoClient.hold(outgoing1);
		waitForEvents();		
		
		rayoClient.output("yes", "1234");
		waitForEvents(1000);
		
		assertNotReceived(InputCompleteEvent.class, outgoing1);
		assertReceived(InputCompleteEvent.class, outgoing2);
		
		rayoClient.unhold(outgoing1);
		waitForEvents(300);
		rayoClient.output("yes", "1234");
		waitForEvents(1000);
		
		assertReceived(InputCompleteEvent.class, outgoing1);
		assertNotReceived(InputCompleteEvent.class, outgoing2);		
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}

	@Test
	public void testActiveSpeakerEvents() throws Exception {
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);
		
		String mixerId = UUID.randomUUID().toString();

		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		rayoClient.output("Hello this is a long phrase without stops so there is no multiple events.", outgoing1);
		
		// one client has spoken, We expect a started speaking event. And a bit later a stopped speaking event
		waitForEvents(400);
		StartedSpeakingEvent started = assertReceived(StartedSpeakingEvent.class, mixerId);
		assertEquals(started.getSpeakerId(), incoming1);
		waitForEvents(2000);
		StoppedSpeakingEvent stopped = assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertEquals(stopped.getSpeakerId(), incoming1);		
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}
	

	@Test
	public void testActiveSpeakerEventsMultiplePhrases() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		rayoClient.output("Hello this is a short phrase.   Hello this is a short phrase.   Hello this is a short phrase.", outgoing1);
		
		waitForEvents(4000);
		assertReceived(StartedSpeakingEvent.class, mixerId);
		assertReceived(StartedSpeakingEvent.class, mixerId);
		assertReceived(StartedSpeakingEvent.class, mixerId);

		assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertReceived(StoppedSpeakingEvent.class, mixerId);
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}
	
	@Test
	public void testMultipleSpeakers() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		rayoClient.output("Hello this is a short phrase.", outgoing1);
		waitForEvents(300);
		rayoClient.output("Hello this is a short phrase.", outgoing2);
		
		waitForEvents(2000);
		
		StartedSpeakingEvent started1 = assertReceived(StartedSpeakingEvent.class, mixerId);
		assertEquals(started1.getSpeakerId(), incoming1);
		StartedSpeakingEvent started2 = assertReceived(StartedSpeakingEvent.class, mixerId);
		assertEquals(started2.getSpeakerId(), incoming2);
		
		StoppedSpeakingEvent stopped1 = assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertEquals(stopped1.getSpeakerId(), incoming1);	
		StoppedSpeakingEvent stopped2 = assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertEquals(stopped2.getSpeakerId(), incoming2);
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}	

	@Test
	// If we whisper to a member of a conference there should not be active speaker events
	public void testWhisperDoesNotGenerateActiveSpeakerEvents() throws Exception {
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);
		
		String mixerId = UUID.randomUUID().toString();

		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		// Whispering. Sending output to incoming1
		rayoClient.output("Hello this is a long phrase without stops so there is no multiple events.", incoming1);

		waitForEvents();
		assertNotReceived(StartedSpeakingEvent.class, mixerId);
		assertNotReceived(StoppedSpeakingEvent.class, mixerId);
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, outgoing2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}
}
