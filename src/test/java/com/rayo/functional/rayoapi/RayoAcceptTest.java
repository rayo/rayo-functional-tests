package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.Duration;
import org.junit.Ignore;
import org.junit.Test;

import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.OutputCompleteEvent;
import com.rayo.core.verb.RecordCompleteEvent;
import com.rayo.core.verb.VerbCompleteReason;
import com.rayo.core.verb.VerbRef;
import com.rayo.core.verb.VerbCompleteEvent.Reason;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;

public class RayoAcceptTest extends RayoBasedIntegrationTest {

	@Test
	public void testAcceptWithEarlyMediaAndOutput() throws Exception {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Supported", "100rel");
		String outgoingCallId = dial(headers).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		IQ result = rayoClient.accept(incomingCallId, true);
		assertFalse(result.isError());
		
		rayoClient.output("hello world", incomingCallId);
		waitForEvents(2000);
		
		OutputCompleteEvent complete = assertReceived(OutputCompleteEvent.class, incomingCallId);
		assertNotNull(complete);
		assertEquals(complete.getReason(), com.rayo.core.verb.OutputCompleteEvent.Reason.SUCCESS);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}

	@Test
	public void testAcceptWithEarlyMediaAndInput() throws Exception {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Supported", "100rel");
		String outgoingCallId = dial(headers).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		IQ result = rayoClient.accept(incomingCallId, true);
		assertFalse(result.isError());
		
		Input input = buildInput("yes,no");
		input.setInitialTimeout(Duration.standardSeconds(1));
		rayoClient.input(input, incomingCallId);
		waitForEvents(2000);
		
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertNotNull(complete);
		assertEquals(complete.getReason(), InputCompleteEvent.Reason.NOINPUT);
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}

	@Test
	public void testAcceptWithEarlyMediaAndRecord() throws Exception {
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Supported", "100rel");
		String outgoingCallId = dial(headers).getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		IQ result = rayoClient.accept(incomingCallId, true);
		assertFalse(result.isError());
		
		VerbRef recordRef = rayoClient.record(incomingCallId);		
		rayoClient.output("Hello World", incomingCallId);		
		waitForEvents();
		rayoClient.stop(recordRef);
		
		RecordCompleteEvent complete = assertReceived(RecordCompleteEvent.class, incomingCallId);
		assertNotNull(complete);
		assertEquals(complete.getReason(), Reason.STOP);
		assertTrue(complete.getDuration().getMillis() >= 1000);
		assertTrue(complete.getSize() > 15000);
		assertNotNull(complete.getUri());
		assertTrue(complete.getUri().toString().startsWith("http"));
		
		rayoClient.hangup(outgoingCallId);
		waitForEvents();
	}

	@Test
	@Ignore
	public void testAcceptWithEarlyMediaFailsIfNo100rel() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		IQ result = rayoClient.accept(incomingCallId, true);
		assertTrue(result.isError());
		assertEquals(result.getError().getText(), "Call does not accept early media");
		assertEquals(result.getError().getCondition(), Condition.conflict);
	}

	private Input buildInput(String simpleGrammar) {
		
		Input input = new Input();
		List<Choices> choices = new ArrayList<Choices>();
		Choices choice = new Choices();
		choice.setContent(simpleGrammar);
		choice.setContentType("application/grammar+voxeo");
		choices.add(choice);
		input.setGrammars(choices);
		
		return input;

	}
}
