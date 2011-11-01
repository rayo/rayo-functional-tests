package com.rayo.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

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

        String text = "<speak><say-as interpret-as=\"ordinal\">100</say-as></speak>";
        OutputCommand outputCommand = new OutputCommand(new TextToSpeechResource(text));        
	    outgoing.output(outputCommand);

	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getInterpretation(),"one hundred");
	    
	    incoming.hangup();
	    waitForEvents();
	}	
	
	@Test
	public void testErrorOnInvalidSsml() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
        Input<Call> input = incoming.input("one hundred,ireland");

        String text = "<speak><output-as interpret-as=\"ordinal\">100</output-as></speak>";
        OutputCommand outputCommand = new OutputCommand(new TextToSpeechResource(text));        
	    Output<Call> output = outgoing.output(outputCommand);
	    waitForEvents();
	    
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
	    assertEquals(complete.getCause(), Cause.ERROR);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testAudioPlayback() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    output.pause();
	    waitForEvents(6000);
	    output.resume();
	    waitForEvents(1000);
	    
	    output.stop();
	    
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
	    assertEquals(complete.getCause(), Cause.CANCEL);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testAudioSeek() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    output.move(true, 2000);
	    output.move(false, 2000);
	    
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);

	    waitForEvents(2000);
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
	    assertEquals(complete.getCause(), Cause.END);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testAudioSpeedUp() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    // Normally it takes 6 seconds. See test above.
	    output.speed(true);
	    waitForEvents(4000);

	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output, 1);
	    assertEquals(complete.getCause(), Cause.END);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testAudioSlowDown() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    output.speed(false);
	    output.speed(false);
	    waitForEvents(4000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    waitForEvents(2000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    waitForEvents(8000);
	    
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output, 1);
	    assertEquals(complete.getCause(), Cause.END);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	
	@Test
	public void testAudioVolume() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    output.volume(true);
	    waitForEvents();
	    output.volume(false);
	    waitForEvents(4000);
	    
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output, 1);
	    assertEquals(complete.getCause(), Cause.END);
	    
	    incoming.hangup();
	    waitForEvents();
	}

}
