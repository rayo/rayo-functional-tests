package com.rayo.functional.rayoapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.xmpp.extensions.Extension;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.rayo.core.EndEvent;
import com.rayo.core.EndEvent.Reason;
import com.rayo.core.RejectCommand;
import com.rayo.core.validation.Messages;
import com.rayo.functional.base.RayoBasedIntegrationTest;

public class RayoRejectTest extends RayoBasedIntegrationTest {

	@Test
	public void testEmptyReject() throws Exception {
		
		String outgoing = dial().getCallId();
		try {
			String incomingCallId = getIncomingCall().getCallId();			
			rayoClient.reject(incomingCallId);
			EndEvent end = assertReceived(EndEvent.class,incomingCallId);
			assertEquals(end.getReason(), Reason.REJECT);
		} finally {
			rayoClient.hangup(outgoing);
		}
	}
	

	@Test
	public void testRejectWithNoReason() throws Exception {
		
		String outgoing = dial().getCallId();

		try {
	    	String incomingCallId = getIncomingCall().getCallId();   
		    assertNotNull(incomingCallId);

		    RejectCommand reject = new RejectCommand();
		    reject.setReason(null);
		    XmppConnection connection = rayoClient.getXmppConnection();
		    IQ iq = new IQ(IQ.Type.get)
				.setFrom(buildFrom(connection))
				.setTo(incomingCallId+"@"+rayoServer+"/"+RayoClient.DEFAULT_RESOURCE)
				.setChild(Extension.create(reject));
		    IQ result = (IQ)connection.sendAndWait(iq);		    
		    assertTrue(result.isError());
		    assertEquals(result.getError().getCondition(), Condition.bad_request);
		    assertTrue(result.getError().getText().contains(Messages.MISSING_REASON));
		} finally {
			rayoClient.hangup(outgoing);
		}
	}
	
	
	private String buildFrom(XmppConnection connection) {
		
		return connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource();
	}
}
