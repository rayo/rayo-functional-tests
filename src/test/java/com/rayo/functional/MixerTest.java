package com.rayo.functional;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.ActiveSpeakerEvent;
import com.voxeo.moho.event.InputCompleteEvent;

public class MixerTest extends MohoBasedIntegrationTest {
  private Logger log = LoggerFactory.getLogger(MixerTest.class);

  // test https://voxeolabs.atlassian.net/browse/RAYO-4
  @Test
  public void testJoinSpeak() {
    OutgoingCall outgoing1 = null;
    OutgoingCall outgoing2 = null;

    Mixer mixer = null;
    outgoing1 = dial();
    IncomingCall incoming1 = getIncomingCall();
    incoming1.answer();
    waitForEvents();

    outgoing2 = dial();
    IncomingCall incoming2 = getIncomingCall();
    incoming2.answer();
    waitForEvents();

    // create mixer
    MixerEndpoint mixerEndpoint = mohoRemote.createMixerEndpoint();
    mixer = mixerEndpoint.create("1234", null);

    Joint joint = incoming1.join(mixer, JoinType.BRIDGE, Direction.DUPLEX);

    try {
      joint.get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    outgoing1.input("yes,no");
    mixer.output("yes");
    waitForEvents(4000);
    assertReceived(InputCompleteEvent.class, outgoing1);

    Joint joint2 = incoming2.join(mixer, JoinType.BRIDGE, Direction.DUPLEX);

    try {
      joint2.get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    outgoing1.input("yes,no");
    outgoing2.input("yes,no");
    mixer.output("yes");
    waitForEvents(4000);
    assertReceived(InputCompleteEvent.class, outgoing1);
    assertReceived(InputCompleteEvent.class, outgoing2);

    outgoing1.hangup();
    outgoing2.hangup();
    mixer.disconnect();
    waitForEvents();
  }

  @Test
  public void testActiveSpeakerEvent() {
    OutgoingCall outgoing1 = dial();
    IncomingCall incoming1 = getIncomingCall();
    incoming1.answer();
    waitForEvents();

    // create mixer
    Mixer mixer = createMixer("1234");

    Joint joint = incoming1.join(mixer, JoinType.BRIDGE, Direction.DUPLEX);

    try {
      joint.get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    outgoing1.output("yes");
    waitForEvents(4000);
    assertReceived(ActiveSpeakerEvent.class, mixer);

    outgoing1.hangup();
    mixer.disconnect();
    waitForEvents();
  }
}
