package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.core.DtmfEvent;
import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.InputMode;
import com.rayo.functional.base.RayoBasedIntegrationTest;


public class RayoDtmfTest extends RayoBasedIntegrationTest {
	
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
	public void testDtmfEvent() throws Exception {
		
		dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    rayoClient.dtmf("1", incomingCall);
	    waitForEvents(500);
	    DtmfEvent event = assertReceived(DtmfEvent.class, incomingCall);
	    assertEquals(event.getSignal(),"1");
	}
	
	
	@Test
	public void testDtmfMultipleTones() throws Exception {
		
		dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);
	    
	    rayoClient.dtmf("12", incomingCall);
	    waitForEvents(500);
	    DtmfEvent event1 = assertReceived(DtmfEvent.class, incomingCall);
	    DtmfEvent event2 = assertReceived(DtmfEvent.class, incomingCall);
	    assertEquals(event1.getSignal(),"1");
	    assertEquals(event2.getSignal(),"2");
	}
	
	@Test
	public void testDtmfInput() throws Exception {
		
		String outgoingCall = dial().getCallId();
	    
    	String incomingCall = getIncomingCall().getCallId();   
	    assertNotNull(incomingCall);
	    rayoClient.answer(incomingCall);

		Input input = new Input();
		List<Choices> choices = new ArrayList<Choices>();
		Choices choice = new Choices();
		choice.setContent("[1 DIGIT]");
		choice.setContentType("application/grammar+voxeo");
		choices.add(choice);
		input.setGrammars(choices);
		input.setMode(InputMode.DTMF);	    
	    rayoClient.input(input, incomingCall);
	    waitForEvents(200);
	    	    
	    rayoClient.dtmf("1", outgoingCall);
	    waitForEvents(500);
	    DtmfEvent event1 = assertReceived(DtmfEvent.class, outgoingCall);
	    InputCompleteEvent event2 = assertReceived(InputCompleteEvent.class, incomingCall);
	    assertEquals(event1.getSignal(),"1");
	
	}
}