package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.joda.time.Duration;
import org.junit.Test;

import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.OutputCompleteEvent;
import com.rayo.core.verb.Record;
import com.rayo.core.verb.RecordCompleteEvent;
import com.rayo.core.verb.VerbCompleteEvent.Reason;
import com.rayo.core.verb.VerbRef;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoRecordTest extends RayoBasedIntegrationTest {

	@Test
	public void testShouldRecordAndGetValidMetadata() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		VerbRef recordRef = rayoClient.record(incomingCallId);		
		rayoClient.output("Hello World", outgoingCallId);
		waitForEvents();
		assertReceived(OutputCompleteEvent.class, outgoingCallId);
		
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		assertNotNull(complete);
		assertEquals(complete.getReason(), Reason.STOP);
		assertTrue(complete.getDuration().getMillis() >= 1000);
		assertTrue(complete.getSize() > 15000);
		assertNotNull(complete.getUri());
		//assertTrue(new File(complete.getUri()).exists()); Difficult to run on cluster setup
		assertTrue(complete.getUri().toString().endsWith(".wav"));
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testMediaRecorded() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		VerbRef recordRef = rayoClient.record(incomingCallId);		
		rayoClient.output("Hello World", outgoingCallId);
		waitForEvents();
		assertReceived(OutputCompleteEvent.class, outgoingCallId);
		
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);

		rayoClient.input("hello world, thanks frank", outgoingCallId);
		
		// It is important to do output on the same leg that made the recording as otherwise the 
		// recorded file may not be found ( dial requests are load balanced and could have ended up 
		// in a different server )
		rayoClient.output(complete.getUri(), incomingCallId);
		waitForEvents();
		
		InputCompleteEvent inputComplete = assertReceived(InputCompleteEvent.class, outgoingCallId);
		assertEquals(inputComplete.getUtterance(), "hello world");
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testInitialTimeout() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		Record record = new Record();
		record.setInitialTimeout(new Duration(100));
		rayoClient.record(record, incomingCallId);		
		Thread.sleep(1000);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		assertTrue(complete.getDuration().getMillis() < 1000);
		assertEquals(complete.getReason(), com.rayo.core.verb.RecordCompleteEvent.Reason.INI_TIMEOUT);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testFinalTimeout() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		Record record = new Record();
		record.setFinalTimeout(new Duration(500));
		VerbRef recordRef = rayoClient.record(record, incomingCallId);		
		rayoClient.output("a", outgoingCallId);
		Thread.sleep(4000);		
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		assertTrue(complete.getDuration().getMillis() <= 1500);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testMaxDuration() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		Record record = new Record();
		record.setMaxDuration(new Duration(1000));
		rayoClient.record(record, incomingCallId);		
		rayoClient.output("Hello World. This is a long test. So it should take a bit to be read. I hope it takes more than three seconds.", outgoingCallId);

		Thread.sleep(2000);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		
		assertEquals(complete.getDuration().getMillis(), 1000);
		assertEquals(complete.getReason(), com.rayo.core.verb.RecordCompleteEvent.Reason.TIMEOUT);		
				
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}	
	
	
	@Test
	public void testPauseAndResume() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		VerbRef recordRef = rayoClient.record(new Record(), incomingCallId);		
		rayoClient.output("Hello World. This is a long test. So it should take a bit to be read. I hope it takes more than three seconds. Hello World. This is a long test. So it should take a bit to be read.", outgoingCallId);
		Thread.sleep(500);
		rayoClient.pauseRecord(recordRef);
		Thread.sleep(3000);
		rayoClient.resumeRecord(recordRef);
		Thread.sleep(500);
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		
		assertTrue(complete.getDuration().getMillis() < 2000);
				
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
}
