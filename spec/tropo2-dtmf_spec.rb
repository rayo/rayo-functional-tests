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

    @call.next_event.should be_a_valid_dtmf_event.with_signal('3')

    hangup_and_confirm
  end

  it "should be generated when DTMF tones are detected during an Input" do
    pending 'https://github.com/tropo/tropo2/issues/135'
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep_for_media_assertion
      play_dtmf 3
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.input(:grammar => { :value => '[1 DIGITS]' }, :mode => :dtmf).should have_executed_correctly

    wait_on_latch :responded

    @call.next_event.should be_a_valid_successful_input_event.with_interpretation('3')

    @call.next_event.should be_a_valid_dtmf_event.with_signal('3')

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

    say = @call.say(:audio => { :url => 'dtmf:5' }).should have_executed_correctly

    wait_on_latch :responded

    say.next_event.should be_a_valid_say_event

    hangup_and_confirm

    @tropo1.result.should eql '5'
  end
end
