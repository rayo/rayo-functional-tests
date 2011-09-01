require 'spec_helper'

describe "Say command" do
  it "should say something with TTS" do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      ask 'One7', :choices     => 'yes, no',
                 :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                 :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    say = @call.say(:text => 'yes').should have_executed_correctly

    wait_on_latch :responded

    say.next_event.should be_a_valid_say_event

    hangup_and_confirm

    @tropo1.result.should eql 'yes'
  end

  it "should say an audio URL and hangup" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      wait_to_hangup 3
    SCRIPT_CONTENT

    get_call_and_answer

    say = @call.say(:ssml => audio_ssml(:url => @config['audio_url'])).should have_executed_correctly

    sleep 9 #Wait for audio file to complete playing

    say.next_event.should be_a_valid_say_event

    hangup_and_confirm
  end

  it "should say SSML" do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      ask 'One8', :choices     => 'one hundred, ireland',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    say = @call.say(:ssml => '<say-as interpret-as="ordinal">100</say-as>').should have_executed_correctly

    wait_on_latch :responded

    say.next_event.should be_a_valid_say_event

    hangup_and_confirm

    @tropo1.result.should eql 'one hundred'
  end

  it "should say some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      wait_to_hangup 2
    SCRIPT_CONTENT

    get_call_and_answer

    say_command = @call.say :ssml => audio_ssml(:url => @config['audio_url'])

    sleep 2
    say_command.pause!
    sleep 2
    say_command.resume!
    sleep 2
    say_command.stop!

    say_command.next_event.should be_a_valid_stopped_say_event

    hangup_and_confirm
  end

  it "should say an audio URL and get a stop event" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      sleep 2
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    say = @call.say(:ssml => audio_ssml(:url => @config['audio_url'])).should have_executed_correctly

    say.next_event.should be_a_valid_complete_hangup_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should error on a say and return a complete event" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      sleep 2
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    lambda { @call.say :text => '' }.should raise_error(Punchblock::ProtocolError)

    @call.next_event.should be_a_valid_hangup_event
  end
end
