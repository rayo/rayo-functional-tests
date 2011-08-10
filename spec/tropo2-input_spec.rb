require 'spec_helper'

describe "Input component" do
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

  it "should input something with ASR and get the utterance back" do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      say 'yes'
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input(:grammar => { :value => 'yes, no' }).should be_true

    wait_on_latch :responded

    @call.next_event.should be_a_valid_successful_input_event.with_utterance('yes')

    hangup_and_confirm
  end

  it "should input something with DTMF and get the interpretation back" do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      play_dtmf 3
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input(:grammar => { :value => '[1 DIGITS]' }, :mode => :dtmf).should be_true

    wait_on_latch :responded

    @call.next_event.should be_a_valid_successful_input_event.with_interpretation('3')

    hangup_and_confirm
  end

  it "should input with an SSML as a prompt" do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      say 'yes'
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input(:grammar => { :value => 'yes, no' }).should be_true

    wait_on_latch :responded

    @call.next_event.should be_a_valid_successful_input_event.with_utterance('yes')

    hangup_and_confirm
  end

  it "should input with a GRXML grammar" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep 3
      say 'clue'
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input(:grammar => { :value        =>  grxml,
                              :content_type => 'application/grammar+grxml' } ).should be_true

    @call.next_event.should be_a_valid_successful_input_event.with_utterance('clue')

    hangup_and_confirm
  end

  it "should input with an SSML prompt and a GRXML grammar" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep 1
      ask 'clue', :choices     => 'one hundred, ireland',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input(:grammar => { :value => grxml,
                              :content_type => 'application/grammar+grxml' } ).should be_true

    @call.next_event.should be_a_valid_successful_input_event.with_utterance('clue')

    hangup_and_confirm
  end

  it "should input and get a NOINPUT event" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 5000
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input :grammar => { :value => 'yes, no' }, :initial_timeout => 2000

    @call.next_event.should be_a_valid_input_noinput_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should input and get a NOMATCH event with min_confidence set to 1" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 1000
      say 'blue'
      wait_to_hangup
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input :grammar          => { :value => 'red, green' },
                :complete_timeout => 3000,
                :min_confidence   => 1

    @call.next_event.should be_a_valid_input_nomatch_event

    hangup_and_confirm
  end

  it "should input and get a STOP if the farside hangs up before the command complete" do
    pending 'https://github.com/tropo/tropo2/issues/59'
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 8000
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input :grammar => { :value => 'red, green' }

    @call.next_event.should be_a_valid_stopped_input_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should input something with an invalid grammar and get an error back" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait 2000
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    lambda { @call.input :grammar => { :value => '<grammar>' } }.should raise_error(Punchblock::ProtocolError)

    @call.next_event.reason.should eql :error
  end
end
