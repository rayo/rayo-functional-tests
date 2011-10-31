package com.rayo.functional;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

import com.rayo.core.verb.Ssml;
import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.OutputCommand;

public class OutputTest extends MohoBasedIntegrationTest {

	String audioURL = "http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3";
	
	@Test
	public void testOutputCompleteReceived() {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output("Hello World");
	    assertReceived(OutputCompleteEvent.class, output);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testOutputSomethingWithTTS() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall call = getIncomingCall();
	    assertNotNull(call);
	    call.answer();
	    
	    Input<Call> input = call.input("yes,no");
	    
	    Output<Call> output = outgoing.output("yes");
	    assertReceived(OutputCompleteEvent.class, output);
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getInterpretation(),"yes");
	    
	    outgoing.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testOutputAudioURL() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    waitForEvents(6000);
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
	    assertEquals(complete.getCause(), Cause.END);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testOutputSSML() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
        Input<Call> input = incoming.input("one hundred,ireland");

        Ssml prompt = new Ssml("<say-as interpret-as=\"ordinal\">100</say-as>");
        OutputCommand outputCommand = new OutputCommand(resolveAudio(prompt));        
	    outgoing.output(outputCommand);

	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getInterpretation(),"one hundred");
	    
	    incoming.hangup();
	    waitForEvents();
	}	
	
}
