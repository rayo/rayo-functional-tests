require 'spec_helper'

describe "Ask command" do
  let :grxml do
    <<-GRXML
      <grammar xmlns="http://www.w3.org/2001/06/grammar" root="MAINRULE">
        <rule id="MAINRULE">
          <one-of>
            <item>
              <item repeat="0-1"> need a</item>
              <item repeat="0-1"> i need a</item>
              <one-of>
                <item> clue </item>
              </one-of>
              <tag> out.concept = "clue";</tag>
            </item>
            <item>
              <item repeat="0-1"> have an</item>
              <item repeat="0-1"> i have an</item>
              <one-of>
                <item> answer </item>
              </one-of>
              <tag> out.concept = "answer";</tag>
            </item>
          </one-of>
        </rule>
      </grammar>
    GRXML
  end

  it "should ask something with ASR and get the utterance back" do
    @tropo1.add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      say 'yes'
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask(:prompt  => { :text  => 'One1' },
              :choices => { :value => 'yes, no' }).should be_true

    @tropo1.wait :responded

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'yes'

    hangup_and_confirm
  end

  it "should ask something with DTMF and get the interpretation back" do
    pending 'Tropo2 does not currently support in-band DTMF'
    @tropo1.add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      play_dtmf 3
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask(:prompt  => { :text  => 'One2' },
              :choices => { :value => '[1 DIGITS]' },
              :mode    => :dtmf).should be_true

    @tropo1.wait :responded

    ask_event = @call.next_event 2
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.interpretation.should eql '3'

    hangup_and_confirm
  end

  it "should ask with an SSML as a prompt" do
    @tropo1.add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      say 'yes'
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask(:prompt  => { :text  => '<say-as interpret-as="ordinal">100</say-as>' },
              :choices => { :value => 'yes, no' }).should be_true

    @tropo1.wait :responded

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'yes'

    hangup_and_confirm
  end

  it "should ask with a GRXML grammar" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep 3
      say 'clue'
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask(:prompt  => { :text         => 'One3' },
              :choices => { :value        =>  grxml,
                            :content_type => 'application/grammar+grxml' } ).should be_true

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'clue'

    hangup_and_confirm
  end

  it "should ask with an SSML prompt and a GRXML grammar" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep 1
      ask 'clue', :choices     => 'one hundred, ireland',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask(:prompt  => { :text  => '<say-as interpret-as="ordinal">100</say-as>' },
              :choices => { :value => grxml,
                            :content_type => 'application/grammar+grxml' } ).should be_true

    ask_event = @call.next_event
    ask_event.should be_a_valid_successful_ask_event
    ask_event.reason.utterance.should eql 'clue'

    hangup_and_confirm
  end

  it "should ask and get a NOINPUT event" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 5000
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask :prompt  => { :text  => 'Yeap' },
              :choices => { :value => 'yes, no' },
              :timeout => 2000

    @call.next_event.should be_a_valid_ask_noinput_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask and get a NOMATCH event with min_confidence set to 1" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 1000
      say 'blue'
      wait_to_hangup
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask :prompt         => { :text  => 'Yeap' },
              :choices        => { :value => 'red, green' },
              :timeout        => 3000,
              :min_confidence => 1

    @call.next_event.should be_a_valid_ask_nomatch_event

    hangup_and_confirm
  end

  it "should ask and get a STOP if the farside hangs up before the command complete" do
    pending 'https://github.com/tropo/tropo2/issues/59'
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 8000
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.ask :prompt  => { :text  => 'Yeap' },
              :choices => { :value => 'red, green' }

    @call.next_event.should be_a_valid_stopped_ask_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should ask something with an invalid grammar and get an error back" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 2000
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    lambda { @call.ask :prompt  => { :text => 'One4' },
                       :choices => { :value => '<grammar>' } }.should raise_error(Punchblock::Protocol::ProtocolError)

    @call.next_event.reason.should eql :error
  end
end
