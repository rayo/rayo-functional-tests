package com.rayo.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

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

	String audioURL = "http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3"; // 7-8 seconds length
	
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
	    
	    // Forward
	    Output<Call> output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);	    
	    output.move(true, 6000);
	    waitForEvents(1000);	    
	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
	    assertEquals(complete.getCause(), Cause.END);
	    
	    // Backward. Same as above but we move back before finishing up	    
	    output = incoming.output(new URI(audioURL));
	    waitForEvents(1000);	    
	    output.move(true, 6000);
	    output.move(false, 4000);
	    waitForEvents(1000);	    
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testAudioSpeedUp() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    long init = System.currentTimeMillis();
	    Output<Call> output = incoming.output(new URI(audioURL));	    
	    // Audio is 7-8 seconds
	    output.speed(true);
	    output.speed(true);
	    output.speed(true);
	    output.speed(true);
	    output.speed(true);
	    waitForEvents(4000);

	    OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output, 0);
	    long end = System.currentTimeMillis();
	    assertEquals(complete.getCause(), Cause.END);
	    assertTrue(end - init < 7000);
	    
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
	    
	    output.speed(false);
	    output.speed(false);
	    output.speed(false);
	    output.speed(false);
	    output.speed(false);

	    waitForEvents(9000);
	    assertNotReceived(OutputCompleteEvent.class, output);
	    
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

	    // Difficult test. Just check that messages don't throw exceptions.
	    output.volume(true);
	    output.volume(false);
	    
	    incoming.hangup();
	    waitForEvents();
	}

	@Test
	public void testNewOutputStopsActiveOutput() throws Exception {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output1 = incoming.output(new URI(audioURL));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output1);

	    Output<Call> output2 = incoming.output("hello");
	    waitForEvents(1000);
	    
	    OutputCompleteEvent<?> complete1 = assertReceived(OutputCompleteEvent.class, output1, 1);
	    assertEquals(complete1.getCause(), Cause.CANCEL);

	    waitForEvents(1000);
	    OutputCompleteEvent<?> complete2 = assertReceived(OutputCompleteEvent.class, output2, 1);
	    assertEquals(complete2.getCause(), Cause.END);
	    
	    incoming.hangup();
	    waitForEvents();
	}
}
