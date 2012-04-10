package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.core.JoinDestinationType;
import com.rayo.core.JoinedEvent;
import com.rayo.core.StartedSpeakingEvent;
import com.rayo.core.StoppedSpeakingEvent;
import com.rayo.core.UnjoinedEvent;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.OutputCompleteEvent;
import com.rayo.core.verb.RecordCompleteEvent;
import com.rayo.core.verb.VerbCompleteEvent;
import com.rayo.core.verb.VerbCompleteEvent.Reason;
import com.rayo.core.verb.VerbRef;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.xmpp.stanza.IQ;


public class RayoMixerApiTest extends RayoBasedIntegrationTest {

	@Test
	public void testJoinAndUnjoin() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		String mixerId = UUID.randomUUID().toString();
		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		JoinedEvent event = assertReceived(JoinedEvent.class, incomingCallId);
		assertEquals(event.getTo(), mixerId);
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		UnjoinedEvent unjoined = assertReceived(UnjoinedEvent.class, incomingCallId);
		assertEquals(unjoined.getFrom(), mixerId);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testActiveMixersInNode() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		String outgoingCallId2 = dial().getCallId();
		String incomingCallId2 = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId2);
		waitForEvents();

		String mixerId = UUID.randomUUID().toString();
		String mixerId2 = UUID.randomUUID().toString();
		
		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		JoinedEvent event = assertReceived(JoinedEvent.class, incomingCallId);
		assertEquals(event.getTo(), mixerId);

		iq = rayoClient.join(mixerId2, "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId2);
		assertTrue(iq.isResult());
		JoinedEvent event2 = assertReceived(JoinedEvent.class, incomingCallId2);
		assertEquals(event2.getTo(), mixerId2);

		assertEquals(activeMixers + 2, getActiveMixersInNodes());
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		UnjoinedEvent unjoined = assertReceived(UnjoinedEvent.class, incomingCallId);
		assertEquals(unjoined.getFrom(), mixerId);

		assertEquals(activeMixers + 1, getActiveMixersInNodes());

		iq = rayoClient.unjoin(mixerId2, JoinDestinationType.MIXER, incomingCallId2);
		assertTrue(iq.isResult());
		UnjoinedEvent unjoined2 = assertReceived(UnjoinedEvent.class, incomingCallId2);
		assertEquals(unjoined2.getFrom(), mixerId2);

		rayoClient.hangup(outgoingCallId);
		rayoClient.hangup(outgoingCallId2);
		waitForEvents();
		
		assertEquals(activeMixers, getActiveMixersInNodes());
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
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		waitForEvents();
		rayoClient.input("yes,no", outgoing1);
		rayoClient.input("yes,no", outgoing2);

		waitForEvents();
		rayoClient.output("yes", mixerName);
		
		waitForEvents(1000);
		// Expect input completes. Does not work
		assertReceived(InputCompleteEvent.class, outgoing1);
		assertReceived(InputCompleteEvent.class, outgoing2);
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();	
		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testAcceptAndJoinMixer() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.accept(incoming1);

		IQ iq = rayoClient.join(mixerName, null, null, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		waitForEvents();
		rayoClient.input("yes,no", outgoing1);

		waitForEvents();
		rayoClient.output("yes", mixerName);
		
		waitForEvents(1000);
		// Expect input completes. Does not work
		assertNotReceived(InputCompleteEvent.class, outgoing1);
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testNoSelfEchoOnMixer() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerId = UUID.randomUUID().toString();
		String outgoing1 = dial().getCallId();
		waitForEvents(100);
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		IQ iq = rayoClient.join(mixerId, null, null, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		waitForEvents();
		rayoClient.input("yes,no", outgoing1);
		waitForEvents(200);
		rayoClient.output("yes", outgoing1);
		waitForEvents(2000);
		
		// Expect no echo
		assertNotReceived(InputCompleteEvent.class, outgoing1);
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}	
	
	@Test
	@Ignore
	public void testNoEchoIfOutputAndJoinMixer() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.accept(incoming1);
		
		rayoClient.output("welcome", incoming1);

		IQ iq = rayoClient.join(mixerName, null, null, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		waitForEvents();
		rayoClient.input("yes,no", outgoing1);
		waitForEvents(200);
		rayoClient.output("yes", outgoing1);
		waitForEvents(2000);
		
		// Expect no echo
		assertNotReceived(InputCompleteEvent.class, outgoing1);

		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();			
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testInputOnMixer() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
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

		waitForEvents();
		rayoClient.input("yes,no", mixerId);
		waitForEvents(200);
		rayoClient.output("yes", outgoing1);
		waitForEvents(2000);
		
		// Expect input completes
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, mixerId);
		assertEquals(complete.getUtterance(), "yes");
		
		// the input is now gone
		rayoClient.output("yes", outgoing2);
		assertNotReceived(InputCompleteEvent.class, mixerId);
		
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	
	@Test
	// This test reflects the fact that if we unjoin a mixer, we will received an output 
	// complete event that may arrive after the mixer has been disposed on the mixer and therefore
	// the client application will never get the output complete event. 
	public void testCompleteEventNotReceivedIfUnjoin() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);
		waitForEvents();

		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
        waitForEvents(500);
        
		rayoClient.output("This is a long phrase. It will never have the chance to finish as we are unjoining the mixer very soon" ,mixerName);
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		assertNotReceived(OutputCompleteEvent.class, mixerName);

		rayoClient.hangup(outgoing1);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testHoldUnholdOnMixer() throws Exception {
		
		String mixerId = UUID.randomUUID().toString();
		int activeMixers = getActiveMixersInNodes();
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

		waitForEvents();
		rayoClient.input("yes,no", outgoing1);
		rayoClient.input("yes,no", outgoing2);
		
		rayoClient.hold(outgoing1);
		waitForEvents();		
		
		rayoClient.output("yes", mixerId);
		waitForEvents(1000);
		
		assertNotReceived(InputCompleteEvent.class, outgoing1);
		assertReceived(InputCompleteEvent.class, outgoing2);
		
		rayoClient.unhold(outgoing1);
		waitForEvents(1000);
		rayoClient.output("yes", mixerId);
		waitForEvents(1000);
		
		assertReceived(InputCompleteEvent.class, outgoing1);
		assertNotReceived(InputCompleteEvent.class, outgoing2);		
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}

	@Test
	public void testActiveSpeakerEvents() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
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
		waitForEvents(1000);
		StartedSpeakingEvent started = assertReceived(StartedSpeakingEvent.class, mixerId);
		assertEquals(started.getSpeakerId(), incoming1);
		waitForEvents(2000);
		StoppedSpeakingEvent stopped = assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertEquals(stopped.getSpeakerId(), incoming1);		
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testMixerActorsAreDisposedOnUnjoin() throws Exception {
		/*
		 * If actors are not disposed they will receive and dispatch events even though 
		 * the mixer may not exist
		 */
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String mixerId = UUID.randomUUID().toString();

		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		rayoClient.output("Hello world.", outgoing1);
		waitForEvents(100);

		assertReceived(StartedSpeakingEvent.class, mixerId);
		// We should only got 1 event
		assertNotReceived(StartedSpeakingEvent.class, mixerId);

		assertReceived(StoppedSpeakingEvent.class, mixerId);
		// We should only got 1 event.
		assertNotReceived(StoppedSpeakingEvent.class, mixerId);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents();
		
		// Now repeat the same process again. Joining the mixer will create a new actor
		// but no additional events should pop up
		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		rayoClient.output("Hello world.", outgoing1);
		waitForEvents(100);
		
		assertReceived(StartedSpeakingEvent.class, mixerId);
		// We should only got 1 event
		assertNotReceived(StartedSpeakingEvent.class, mixerId);

		assertReceived(StoppedSpeakingEvent.class, mixerId);
		// We should only got 1 event.
		assertNotReceived(StoppedSpeakingEvent.class, mixerId);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());				
		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();
	}
	
	@Test
	public void testMixerActorsAreDisposedOnEnd() throws Exception {
		/*
		 * If actors are not disposed they will receive and dispatch events even though 
		 * the mixer may not exist
		 */
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String mixerId = UUID.randomUUID().toString();

		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		rayoClient.output("Hello world.", outgoing1);
		waitForEvents(100);

		assertReceived(StartedSpeakingEvent.class, mixerId);
		// We should only got 1 event
		assertNotReceived(StartedSpeakingEvent.class, mixerId);

		assertReceived(StoppedSpeakingEvent.class, mixerId);
		// We should only got 1 event.
		assertNotReceived(StoppedSpeakingEvent.class, mixerId);

		rayoClient.hangup(outgoing1);
		waitForEvents();
		
		// Now repeat the same process again. Joining the mixer will create a new actor
		// but no additional events should pop up
		outgoing1 = dial().getCallId();
		incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);
		waitForEvents();
		
		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		rayoClient.output("Hello world.", outgoing1);
		waitForEvents(100);
		
		assertReceived(StartedSpeakingEvent.class, mixerId);
		// We should only got 1 event
		assertNotReceived(StartedSpeakingEvent.class, mixerId);

		assertReceived(StoppedSpeakingEvent.class, mixerId);
		// We should only got 1 event.
		assertNotReceived(StoppedSpeakingEvent.class, mixerId);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());				
		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();
	}

	@Test
	public void testActiveSpeakerEventsMultiplePhrases() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
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
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testMultipleSpeakers() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
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
		
		List<String> expectedSpeakers = new ArrayList<String>();
		expectedSpeakers.add(incoming1);
		expectedSpeakers.add(incoming2);
		
		StartedSpeakingEvent started1 = assertReceived(StartedSpeakingEvent.class, mixerId);
		assertTrue(expectedSpeakers.contains(started1.getSpeakerId()));
		expectedSpeakers.remove(started1.getSpeakerId());
		
		StartedSpeakingEvent started2 = assertReceived(StartedSpeakingEvent.class, mixerId);
		assertTrue(expectedSpeakers.contains(started2.getSpeakerId()));
		expectedSpeakers.remove(started2.getSpeakerId());		
		assertTrue(expectedSpeakers.size() == 0);

		expectedSpeakers.add(incoming1);
		expectedSpeakers.add(incoming2);

		StoppedSpeakingEvent stopped1 = assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertTrue(expectedSpeakers.contains(stopped1.getSpeakerId()));
		expectedSpeakers.remove(stopped1.getSpeakerId());
		
		StoppedSpeakingEvent stopped2 = assertReceived(StoppedSpeakingEvent.class, mixerId);
		assertTrue(expectedSpeakers.contains(stopped2.getSpeakerId()));
		expectedSpeakers.remove(stopped2.getSpeakerId());
		assertTrue(expectedSpeakers.size() == 0);
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();	
		assertEquals(activeMixers, getActiveMixersInNodes());
	}	

	@Test
	// If we whisper to a member of a conference there should not be active speaker events
	public void testWhisperDoesNotGenerateActiveSpeakerEvents() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
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
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}

	@Test
	public void testMixersAreDisposedAfterLastParticipantsLeave() throws Exception {
		
		int activeMixersInNodes = getActiveMixersInNodes();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		int activeMixers = getActiveMixers();
		
		String mixerId = UUID.randomUUID().toString();
		
		IQ iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(200);
		
		assertEquals(getActiveMixers(), activeMixers + 1);

		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		waitForEvents(200);

		// number of mixers should be preserved, the call has join an existing mixer
		assertEquals(getActiveMixers(), activeMixers + 1);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(200);

		// number of mixers should be preserved, there still should be one participant
		assertEquals(getActiveMixers(), activeMixers + 1);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		waitForEvents(200);
		
		// last participant leaves. Mixer should have been disposed
		assertEquals(getActiveMixers(), activeMixers);
	
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();	
		assertEquals(activeMixersInNodes, getActiveMixersInNodes());
	}
	
	@Test
	public void testTotalMixers() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		long totalMixers = getTotalMixers();
		
		String mixerId1 = UUID.randomUUID().toString();
		String mixerId2 = UUID.randomUUID().toString();
		
		IQ iq = rayoClient.join(mixerId1, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);		
		iq = rayoClient.unjoin(mixerId1, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);		

		rayoClient.join(mixerId2, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);		
		iq = rayoClient.unjoin(mixerId2, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);		

		assertEquals(getTotalMixers(), totalMixers + 2);
	
		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testShouldRecordAndGetValidMetadata() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		String mixerName = UUID.randomUUID().toString();
		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		
		VerbRef recordRef = rayoClient.record(mixerName);
		rayoClient.output("Hello World", mixerName);
		waitForEvents();
		assertReceived(OutputCompleteEvent.class, mixerName);
		
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, mixerName);
		assertNotNull(complete);
		assertEquals(complete.getReason(), Reason.STOP);
		assertTrue(complete.getDuration().getMillis() >= 1000);
		assertTrue(complete.getSize() > 15000);
		assertNotNull(complete.getUri());
		//assertTrue(new File(complete.getUri()).exists()); Difficult to run on cluster setup
		assertTrue(complete.getUri().toString().endsWith(".wav"));
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());

		rayoClient.hangup(outgoingCallId);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	
	@Test
	@Ignore
	public void testMediaRecorded() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		String mixerName = UUID.randomUUID().toString();
		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());

		VerbRef recordRef = rayoClient.record(mixerName);		
		rayoClient.output("Hello World", mixerName);
		waitForEvents();
		assertReceived(OutputCompleteEvent.class, mixerName);
		
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, mixerName);

		// We now do output on the mixer. Important thing to remember here is that the .wav file
		// was stored on the hostname that holds the mixer. So sending the output to the mixer 
		// is the safest way to ensure that we actually play the file.
		waitForEvents();
		rayoClient.input("hello world, thanks frank", outgoingCallId);
		waitForEvents();
		rayoClient.output(complete.getUri(), mixerName);
		waitForEvents();
		
		InputCompleteEvent inputComplete = assertReceived(InputCompleteEvent.class, outgoingCallId);
		assertEquals(inputComplete.getUtterance(), "hello world");
				
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incomingCallId);
		assertTrue(iq.isResult());
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	// Tests that mixer participants are tracked in the gateway and are
	// removed when a call is finished
	public void testMixerParticipants() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String outgoing1 = dial().getCallId();
		waitForEvents(500);
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		waitForEvents(500);
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		String mixerId1 = UUID.randomUUID().toString();

		assertTrue(getParticipants(mixerId1).isEmpty());
		
		IQ iq = rayoClient.join(mixerId1, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);
		
		List<String> participants = getParticipants(mixerId1);
		assertEquals(participants.size(), 1);
		assertTrue(participants.contains(incoming1));
		
		iq = rayoClient.join(mixerId1, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		waitForEvents(500);
		
		participants = getParticipants(mixerId1);
		assertEquals(participants.size(), 2);
		assertTrue(participants.contains(incoming1));
		assertTrue(participants.contains(incoming2));
		
		iq = rayoClient.unjoin(mixerId1, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);		
		
		participants = getParticipants(mixerId1);
		assertEquals(participants.size(), 1);
		assertTrue(participants.contains(incoming2));

		iq = rayoClient.unjoin(mixerId1, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		waitForEvents(500);		
		
		assertTrue(getParticipants(mixerId1).isEmpty());	

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
		assertEquals(activeMixers, getActiveMixersInNodes());
	}

	@Test
	// Tests that mixer participants are tracked in the gateway and are
	// removed when a call ends
	public void testMixerParticipantsOnCallEnd() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String outgoing1 = dial().getCallId();
		waitForEvents(500);
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		String outgoing2 = dial().getCallId();
		waitForEvents(500);
		String incoming2 = getIncomingCall().getCallId();
		rayoClient.answer(incoming2);

		String mixerId1 = UUID.randomUUID().toString();

		assertTrue(getParticipants(mixerId1).isEmpty());
		
		IQ iq = rayoClient.join(mixerId1, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);
		
		iq = rayoClient.join(mixerId1, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		waitForEvents(500);
		
		List<String> participants = getParticipants(mixerId1);
		assertEquals(participants.size(), 2);
		assertTrue(participants.contains(incoming1));
		assertTrue(participants.contains(incoming2));
		
		rayoClient.hangup(outgoing1);		
		waitForEvents();
		
		participants = getParticipants(mixerId1);
		assertEquals(participants.size(), 1);
		assertTrue(participants.contains(incoming2));
		
		rayoClient.hangup(outgoing2);		
		waitForEvents();	
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	// Tests that verb resources for mixers are tracked in the Gateway and are 
	// removed when the verb is completed
	public void testMixerVerbs() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		assertEquals(getActiveVerbsCount(mixerName), 0);
		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(100);
		assertEquals(getActiveVerbsCount(mixerName), 0);
		
		rayoClient.output("yes. This is an output", mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 1);
		assertReceived(OutputCompleteEvent.class, mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 0);
		
		waitForEvents();
		rayoClient.output("this is another output.", mixerName);
		rayoClient.input("yes, no", mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 2);		
		assertReceived(OutputCompleteEvent.class, mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 1);

		rayoClient.output("yes", outgoing1);
		assertReceived(InputCompleteEvent.class, mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 0);
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();	
		assertEquals(activeMixers, getActiveMixersInNodes());
	}

	
	@Test
	@Ignore
	// Two outputs on a mixer. how this should behave?
	public void testMixerAndTwoOutputs() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();
		
		String outgoing1 = dial().getCallId();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);		
		waitForEvents();
		rayoClient.output("this is a long phrase as we need to validate that there will be two active verbs", mixerName);
		rayoClient.output("yes. what is going on", mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 2);
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		waitForEvents();	
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	// Tests that mixer verbs are disposed on the Gateway after verb failure
	public void testMixerVerbsAreDisposedAfterVerbStop() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();		
		String outgoing1 = dial().getCallId();
		waitForEvents();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		assertEquals(getActiveVerbsCount(mixerName), 0);
		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(100);
		assertEquals(getActiveVerbsCount(mixerName), 0);
		
		VerbRef output = rayoClient.output("this is a large output phrase that we are going to stop and check if the verb gets disposed from the mixer.", mixerName);
		assertEquals(getActiveVerbsCount(mixerName), 1);		
		
		rayoClient.stop(output);		
		waitForEvents();
		assertEquals(getActiveVerbsCount(mixerName), 0);

		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		
		waitForEvents();
		rayoClient.hangup(outgoing1);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	// Tests that mixer verbs are disposed on the Gateway after verb failure
	public void testMixerVerbsAreDisposedOnFailure() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();		
		String outgoing1 = dial().getCallId();
		waitForEvents();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);

		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);
		
        String text = "<speak xmlns=\"http://www.w3.org/2001/10/synthesis\" version=\"1.0\" xml:lang=\"en-US\"><audio src=\"digits/3\"/></speak>";
        rayoClient.outputSsml(text,mixerName);

	    VerbCompleteEvent complete = assertReceived(VerbCompleteEvent.class, mixerName);
	    assertEquals(complete.getReason(), Reason.ERROR);
		assertEquals(getActiveVerbsCount(mixerName), 0);
		
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		
		waitForEvents();
		rayoClient.hangup(outgoing1);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	// Tests that mixer verbs are disposed on the Gateway after verb failure
	public void testMixerVerbsDisposedOnMixerUnjoin() throws Exception {
		
		int activeMixers = getActiveMixersInNodes();
		String mixerName = UUID.randomUUID().toString();		
		String outgoing1 = dial().getCallId();
		waitForEvents();
		String incoming1 = getIncomingCall().getCallId();
		rayoClient.answer(incoming1);
		waitForEvents();

		IQ iq = rayoClient.join(mixerName, "bridge", "duplex", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);
		
        rayoClient.output("This is a long phrase. It will never have the chance to finish as we are unjoining the mixer very soon. This is a long phrase. It will never have the chance to finish as we are unjoining the mixer very soon. This is a long phrase. It will never have the chance to finish as we are unjoining the mixer very soon." ,mixerName);
        assertEquals(getActiveVerbsCount(mixerName), 1);
        
		iq = rayoClient.unjoin(mixerName, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());
		waitForEvents(500);
        assertEquals(getActiveVerbsCount(mixerName), 0);
		
		waitForEvents();
		rayoClient.hangup(outgoing1);
		waitForEvents();
		assertEquals(activeMixers, getActiveMixersInNodes());
	}
	
	@Test
	public void testJoinCallsAndThenJoinMixer() throws Exception {
		
		String outgoingCallId1 = dial().getCallId();
		String incomingCallId1 = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId1);
		waitForEvents();
		
		String outgoingCallId2 = dial().getCallId();
		String incomingCallId2 = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId2);
		waitForEvents();
		
		String mixerId = UUID.randomUUID().toString();
				
		IQ iq = rayoClient.join(incomingCallId1, "bridge", "duplex", JoinDestinationType.CALL, incomingCallId2);
		assertTrue(iq.isResult());
		assertReceived(JoinedEvent.class, incomingCallId1);

		iq = rayoClient.join(mixerId, null, null, JoinDestinationType.MIXER, incomingCallId1);
		assertTrue(iq.isResult());

		iq = rayoClient.join(mixerId, null, null, JoinDestinationType.MIXER, incomingCallId2);
		assertTrue(iq.isResult());

		JoinedEvent event = assertReceived(JoinedEvent.class, mixerId);
		event = assertReceived(JoinedEvent.class, mixerId);

		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incomingCallId1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incomingCallId2);
		assertTrue(iq.isResult());

		assertReceived(UnjoinedEvent.class, incomingCallId1);
		assertReceived(UnjoinedEvent.class, incomingCallId2);
		
		rayoClient.hangup(outgoingCallId1);
		rayoClient.hangup(outgoingCallId2);
		waitForEvents();
	}
}
