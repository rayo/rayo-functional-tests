package com.rayo.functional.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent.Cause;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

/**
 * Load tests are executed by JMeter. Feel free to add here any scenarios that should be ran 
 * in load tests.
 * 
 * @author martin
 *
 */
public class LoadTest extends MohoBasedIntegrationTest {

	@Test
	public void testLoadScenario1() {
		
	    dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Output<Call> output = incoming.output(
	    		"German speaking communities can be found in the former German colony of Namibia, " + 
	    		"independent from South Africa since 1990, as well as in the other countries of " + 
	    		"German emigration such as the US, Canada, Mexico, Dominican Republic, Brazil, Argentina, " +
	    		"Paraguay, Uruguay, Chile, Peru, Venezuela (where the dialect Alemán Coloniero developed), " +
	    		"South Africa and Australia. In Namibia, German Namibians retain German educational institutions");
	    waitForEvents(20000);
	    assertReceived(OutputCompleteEvent.class, output);
	    
	    incoming.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testLoadScenario2() {
		
		OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    // Some output
	    Output<Call> output = incoming.output("Hello World");
	    assertReceived(OutputCompleteEvent.class, output);
	    
	    // Some input
	    Input<Call> input = incoming.input(new InputCommand(new SimpleGrammar("yes,no")));
	    waitForEvents(500);
	    outgoing.output("yes");	    
	    
	    incoming.output("Input executed successfully");
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.MATCH);
	    assertEquals(complete.getInterpretation(),"yes");
	    
	    // Some recording
	    /*
	    Recording<Call> recording = incoming.record(new RecordCommand(null));	    
	    output = outgoing.output("Hello Recording");
	    waitForEvents();	    
	    assertReceived(OutputCompleteEvent.class, output);
	    recording.stop();
	    waitForEvents();
	    RecordCompleteEvent<?> completeRecording = assertReceived(RecordCompleteEvent.class, recording);
		assertEquals(completeRecording.getCause(), com.voxeo.moho.event.RecordCompleteEvent.Cause.CANCEL);
		*/
		
	    incoming.hangup();
	    waitForEvents();

	}
	
	/**
	 * Public method that will be invoked by The Grinder. This method will return an integer other than 
	 * 0 if the test scenario has failed
	 */
	public int loadTest(int user) {
		
		try {
			loadProperties();
			setup(xmppUsername+user, xmppPassword+user);
			
			String defaultSipUri = sipDialUris.get(0);
			int i = defaultSipUri.indexOf("@")+1;
			int j = defaultSipUri.indexOf(":",i);
			if (j == -1) {
				j = defaultSipUri.length();
			}
			String serverName = defaultSipUri.substring(i,j);
			sipDialUris.clear();
			String[] uris = new String[]{"sip:user"+user+"@"+serverName};
			sipDialUris.addAll(Arrays.asList(uris));
			
			return 0;
		} catch (Exception e) {
			return 1;
		}
	}
}
