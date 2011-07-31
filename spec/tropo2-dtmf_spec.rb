require 'spec_helper'

describe "DTMF events" do
  it "should be generated when DTMF tones are detected" do
    pending "Currently need a running <ask/>"
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      play_dtmf 3
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    wait_on_latch :responded

    dtmf_event = @call.next_event 2
    dtmf_event.should be_a_valid_dtmf_event
    dtmf_event.signal.should == '3'

    hangup_and_confirm
  end

  it "should be generated when DTMF tones are detected during an <ask/>" do
    pending 'Tropo2 does not currently support in-band DTMF'
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      play_dtmf 3
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask(:prompt  => { :text  => 'Three?' },
              :choices => { :value => '[1 DIGITS]' },
              :mode    => :dtmf).should be_true

    wait_on_latch :responded

    ask_event = @call.next_event 2
    p ask_event
    ask_event.should be_a_valid_ask_event
    ask_event.reason.utterance.should eql '3'

    dtmf_event = @call.next_event
    dtmf_event.should be_a_valid_dtmf_event
    dtmf_event.signal.should == '3'

    hangup_and_confirm
  end

  it "should send DTMF tones correctly" do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      ask 'One6', :choices     => '[1 DIGITS]',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.say(:audio => { :url => 'dtmf:5' }).should be_true

    wait_on_latch :responded

    @call.next_event#.should be_a_valid_say_event

    hangup_and_confirm

    @tropo1.result.should eql '5'
  end
end
