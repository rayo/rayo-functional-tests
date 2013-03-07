package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.core.verb.CpaData;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.InputCompleteEvent.Reason;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.SignalEvent;
import com.rayo.core.verb.VerbRef;
import com.rayo.functional.InputTest;
import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.RayoClient.Grammar;
import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.IQ;

@Ignore
public class RayoCPATest extends RayoBasedIntegrationTest {

	@Test
	public void testBeep() throws Exception {
		
		beepTest("beep");
	}

	@Test
	public void testMultiplePossibleSignals() throws Exception {

		beepTest("beep", "fax");
	}
	
	@Test
	public void testSit() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("sit");
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.sit.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(3000);
		
		SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
		assertTrue(signal.getType().equalsIgnoreCase("sit"));
		
		rayoClient.hangup(incomingCallId);
	}

	@Test
	public void testFax() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("fax","fax-cng");
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.fax.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(6000);
		SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
		assertTrue(signal.getType().equalsIgnoreCase("fax"));
		
		rayoClient.hangup(incomingCallId);
	}	

	@Test
	public void testMultipleIncomingEvents() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("fax","fax-cng");
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.fax.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(3000);
		SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
		assertTrue(signal.getType().equalsIgnoreCase("fax"));
		assertNotReceived(SignalEvent.class, incomingCallId);
		
		waitForEvents(3000);
		signal = assertReceived(SignalEvent.class, incomingCallId);
		assertTrue(signal.getType().equalsIgnoreCase("fax"));
		
		rayoClient.hangup(incomingCallId);
	}
	
	@Test
	public void testStop() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("sit");
		cpaInput.setCpaData(cpaData);
		
		VerbRef inputRef = rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.sit.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(300);
		
		rayoClient.stop(inputRef);		
		waitForEvents(3000);
		
		assertNotReceived(SignalEvent.class, incomingCallId);
		
		rayoClient.hangup(incomingCallId);
	}
	
	@Test
	public void testCpaWithMultipleGrammars() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("sit");
		cpaInput.setCpaData(cpaData);		

		Grammar grammar = rayoClient.new Grammar(InputTest.grxml, "application/srgs+xml");
		
		rayoClient.input(cpaInput, incomingCallId, grammar);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.sit.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(3000);
		
		SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
		assertTrue(signal.getType().equalsIgnoreCase("sit"));
		
		rayoClient.hangup(incomingCallId);
	}

	
	@Test
	public void testSendDtmf() throws Exception {
		
		dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    IQ result = rayoClient.dtmf("1", incomingCall);
	    assertNotNull(result);
	    assertTrue(result.isResult());	    
	}
	
	@Test
	public void testInvalidDtmf() throws Exception {
		
		dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    IQ result = rayoClient.dtmf("a", incomingCall);
	    assertNotNull(result);
	    assertTrue(result.isError());	    
	    assertEquals(result.getError().getCondition(), Condition.bad_request);
	    assertTrue(result.getError().getText().contains("Invalid DTMF key"));
	}
	
	@Test
	public void testDtmfSignal() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("dtmf");
		cpaInput.setCpaData(cpaData);
		cpaInput.setMode(InputMode.DTMF);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
	    IQ result = rayoClient.dtmf("1", outgoingCallId);
	    assertNotNull(result);
	    assertTrue(result.isResult());	    
	    
	    waitForEvents(300);
	    SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal.getType().equalsIgnoreCase("dtmf"));
	    assertEquals(signal.getTone(), "1");
	}
	
	@Test
	public void testMultipleDtmfSignals() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("dtmf");
		cpaInput.setCpaData(cpaData);
		cpaInput.setMode(InputMode.DTMF);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
	    IQ result = rayoClient.dtmf("12", outgoingCallId);
	    assertNotNull(result);
	    assertTrue(result.isResult());	    
	    
	    waitForEvents(300);
	    SignalEvent signal1 = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal1.getType().equalsIgnoreCase("dtmf"));
	    assertEquals(signal1.getTone(), "1");
	    SignalEvent signal2 = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal2.getType().equalsIgnoreCase("dtmf"));
	    assertEquals(signal2.getTone(), "2");
	}
	
	@Test
	public void testMachineSpeech() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("speech");
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.am.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(20000);
	    
	    SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal.getType().equalsIgnoreCase("speech"));
	    assertEquals(signal.getSource(), "machine");
	    assertTrue(signal.getDuration() > 19000);
	}

	@Test
	public void testMaxTime() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("speech");
		// 2 seconds machine threshold
		cpaData.setMaxTime(2000L);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
		// 4.8 seconds speech
		URL beep = Thread.currentThread().getContextClassLoader().getResource("human.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(6000);
	    
	    SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal.getType().equalsIgnoreCase("speech"));
	    assertEquals(signal.getSource(), "machine");
	    assertTrue(signal.getDuration() > 4000);
	}

	@Test
	public void testHuman() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("speech");
		// 6 seconds machine threshold
		cpaData.setMaxTime(6000L);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
		// 4.8 seconds speech
		URL beep = Thread.currentThread().getContextClassLoader().getResource("human.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(6000);
	    
	    SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal.getType().equalsIgnoreCase("speech"));
	    assertEquals(signal.getSource(), "human");
	    assertTrue(signal.getDuration() > 4000);
	}

	private void beepTest(String... signals) throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData(signals);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("beep.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(3000);
		
		SignalEvent signal = assertReceived(SignalEvent.class, incomingCallId);
		assertTrue(signal.getType().equalsIgnoreCase("beep"));
		
		rayoClient.hangup(incomingCallId);	
	}

	@Test
	public void testBadSignal() throws Exception {
		
		dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("foo");
		cpaInput.setCpaData(cpaData);
		
		try {
			rayoClient.input(cpaInput, incomingCallId);
			fail("should not be here");
		} catch (XmppException xe) {
			assertEquals(xe.getError().getCondition(), Condition.bad_request);
			assertTrue(xe.getError().getText().contains("Invalid signal: foo"));
		}
	}
	
	@Test
	public void testSignalTermination() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("sit");
		cpaData.setTerminate(true);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.sit.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(3000);
		
		assertNotReceived(SignalEvent.class, incomingCallId);
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertNotNull(complete.getSignalEvent());
		assertEquals(complete.getSignalEvent().getType(), "sit");
		
		rayoClient.hangup(incomingCallId);
	}
	
	@Test
	public void testMultipleSignalsTermination() throws Exception {
		
		String outgoingCallId = dial().getCallId();
		String incomingCallId = getIncomingCall().getCallId();
		
		rayoClient.answer(incomingCallId);
		waitForEvents();

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("sit","fax");
		cpaData.setTerminate(true);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
		
		URL beep = Thread.currentThread().getContextClassLoader().getResource("test.cpa.sit.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(3000);
		
		assertNotReceived(SignalEvent.class, incomingCallId);
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertNotNull(complete.getSignalEvent());
		assertEquals(complete.getSignalEvent().getType(), "sit");
		
		rayoClient.hangup(incomingCallId);
	}
	
	@Test
	public void testDtmfSignalTermination() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("dtmf");
		cpaData.setTerminate(true);
		cpaInput.setCpaData(cpaData);
		cpaInput.setMode(InputMode.DTMF);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
	    IQ result = rayoClient.dtmf("1", outgoingCallId);
	    assertNotNull(result);
	    assertTrue(result.isResult());	    
	    
	    waitForEvents(300);
		assertNotReceived(SignalEvent.class, incomingCallId);
		InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertNotNull(complete.getSignalEvent());
		assertEquals(complete.getSignalEvent().getType(), "dtmf");
		assertEquals(complete.getSignalEvent().getTone(), "1");
	}
	
	@Test
	public void testDtmfWithDtmfGrammarTermination() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("dtmf");
		cpaInput.setCpaData(cpaData);
		cpaInput.setMode(InputMode.DTMF);
		
		Grammar grammar = rayoClient.new Grammar("[2 DIGITS]", "application/grammar+voxeo");		
		rayoClient.input(cpaInput, incomingCallId, grammar);
		waitForEvents();
	    
	    IQ result = rayoClient.dtmf("12", outgoingCallId);
	    assertNotNull(result);
	    assertTrue(result.isResult());	    
	    
	    waitForEvents(300);
	    SignalEvent signal1 = assertReceived(SignalEvent.class, incomingCallId);
	    assertTrue(signal1.getType().equalsIgnoreCase("dtmf"));
	    assertEquals(signal1.getTone(), "1");
	    
	    // second event wont be a signal but complete as we are using a grammar for termination	    
	    assertNotReceived(SignalEvent.class, incomingCallId);
	    InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
	    assertEquals(complete.getReason(), Reason.MATCH);
	    assertEquals(complete.getInterpretation(),"dtmf-1 dtmf-2");		
	}
	
	@Test
	public void testHumanTermination() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("speech");
		// 6 seconds machine threshold
		cpaData.setMaxTime(6000L);
		cpaData.setTerminate(true);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
		// 4.8 seconds speech
		URL beep = Thread.currentThread().getContextClassLoader().getResource("human.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(6000);
	    
	    assertNotReceived(SignalEvent.class, incomingCallId);
	    InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertNotNull(complete.getSignalEvent());
		assertEquals(complete.getSignalEvent().getType(), "speech");
		assertEquals(complete.getSignalEvent().getSource(), "human");
		assertTrue(complete.getSignalEvent().getDuration() > 4000);
	}
	
	@Test
	public void testMachineTermination() throws Exception {
		
		String outgoingCallId = dial().getCallId();
	    
    	String incomingCallId = getIncomingCall().getCallId();   
	    assertNotNull(incomingCallId);
	    rayoClient.answer(incomingCallId);
	    waitForEvents(200);

		Input cpaInput = new Input();
		CpaData cpaData = new CpaData("speech");
		// 2 seconds machine threshold
		cpaData.setMaxTime(2000L);
		cpaData.setTerminate(true);
		cpaInput.setCpaData(cpaData);
		
		rayoClient.input(cpaInput, incomingCallId);
		waitForEvents();
	    
		// 4.8 seconds speech
		URL beep = Thread.currentThread().getContextClassLoader().getResource("human.wav");
		rayoClient.output(beep.toURI(), outgoingCallId);
		waitForEvents(6000);
	    
	    assertNotReceived(SignalEvent.class, incomingCallId);
	    InputCompleteEvent complete = assertReceived(InputCompleteEvent.class, incomingCallId);
		assertNotNull(complete.getSignalEvent());
		assertEquals(complete.getSignalEvent().getType(), "speech");
		assertEquals(complete.getSignalEvent().getSource(), "machine");
		assertTrue(complete.getSignalEvent().getDuration() > 4000);
	}	
}
