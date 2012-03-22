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
		waitForEvents(2000);
		
		// Expect input completes
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, "1234");
		assertEquals(complete.getUtterance(), "yes");
		
		// the input is now gone
		rayoClient.output("yes", outgoing2);
		assertNotReceived(InputCompleteEvent.class, "1234");
		
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming2);
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
		
		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin("1234", JoinDestinationType.MIXER, incoming2);
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
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
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
		
		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}

	@Test
	public void testMixersAreDisposedAfterLastParticipantsLeave() throws Exception {
		
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
		
		assertEquals(getActiveMixers(), activeMixers + 1);

		iq = rayoClient.join(mixerId, "bridge", "duplex", JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());

		// number of mixers should be preserved, the call has join an existing mixer
		assertEquals(getActiveMixers(), activeMixers + 1);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming1);
		assertTrue(iq.isResult());

		// number of mixers should be preserved, there still should be one participant
		assertEquals(getActiveMixers(), activeMixers + 1);

		iq = rayoClient.unjoin(mixerId, JoinDestinationType.MIXER, incoming2);
		assertTrue(iq.isResult());
		
		// last participant leaves. Mixer should have been disposed
		assertEquals(getActiveMixers(), activeMixers);
	
		waitForEvents();

		rayoClient.hangup(outgoing1);
		rayoClient.hangup(outgoing2);
		waitForEvents();		
	}
	
	@Test
	public void testTotalMixers() throws Exception {
		
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
	}
	
	@Test
	public void testShouldRecordAndGetValidMetadata() throws Exception {
		
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
	}
	
	
	@Test
	@Ignore
	public void testMediaRecorded() throws Exception {
		
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
	}
}
