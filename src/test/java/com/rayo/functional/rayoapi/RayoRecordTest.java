package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.joda.time.Duration;
import org.junit.Ignore;
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
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
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
		assertTrue(new File(complete.getUri()).exists());
		System.out.println(complete.getUri());
		assertTrue(complete.getUri().toString().endsWith(".wav"));
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testMediaRecorded() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		VerbRef recordRef = rayoClient.record(incomingCallId);		
		rayoClient.output("Hello World", outgoingCallId);
		waitForEvents();
		assertReceived(OutputCompleteEvent.class, outgoingCallId);
		
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);

		rayoClient.input("hello world, thanks frank", incomingCallId);
		
		rayoClient.output(complete.getUri(), outgoingCallId);
		waitForEvents();
		
		InputCompleteEvent inputComplete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertEquals(inputComplete.getUtterance(), "hello world");
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	@Ignore
	//TODO:  #1584774
	public void testInitialTimeout() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		Record record = new Record();
		record.setInitialTimeout(new Duration(100));
		rayoClient.record(record, incomingCallId);		
		Thread.sleep(1000);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		System.out.println(complete.getDuration().getMillis());
		assertTrue(complete.getDuration().getMillis() < 1000);
		assertEquals(complete.getReason(), com.rayo.core.verb.RecordCompleteEvent.Reason.INI_TIMEOUT);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	@Ignore
	//TODO:  #1584780
	public void testFinalTimeout() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
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
		System.out.println(complete.getDuration().getMillis());
		assertTrue(complete.getDuration().getMillis() <= 1000);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
	
	@Test
	public void testMaxDuration() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
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
		System.out.println(complete.getReason());
		assertEquals(complete.getReason(), com.rayo.core.verb.RecordCompleteEvent.Reason.TIMEOUT);		
				
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}	
	
	
	@Test
	@Ignore
	//TODO:  #1584783
	public void testPauseAndResume() throws Exception {
		
		String outgoingCallId = rayoClient.dial(new URI("sip:usera@localhost")).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();
		
		VerbRef recordRef = rayoClient.record(new Record(), incomingCallId);		
		rayoClient.output("Hello World. This is a long test. So it should take a bit to be read. I hope it takes more than three seconds. Hello World. This is a long test. So it should take a bit to be read.", outgoingCallId);
		Thread.sleep(500);
		rayoClient.pause(recordRef);
		Thread.sleep(2000);
		rayoClient.resume(recordRef);
		Thread.sleep(500);
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		System.out.println(complete.getDuration());
		
		assertTrue(complete.getDuration().getMillis() < 1500);
				
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}
}
