require 'spec_helper'

# startCallRecording 'http://tropo-audiofiles-to-s3.heroku.com/post_audio_to_s3?file_name=ozone2_testing.wav'

describe "Call accept, answer and hangup handling" do
  it "should receive a call and then hangup" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    hangup_and_confirm
  end

  it "should answer and hangup" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer
    hangup_and_confirm
  end

  it "should throw an error if we try to answer a call that is hungup" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer
    hangup_and_confirm

    lambda { @call.answer }.should raise_error(Punchblock::ProtocolError)
  end

  it "should accept and hangup" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    @call.accept.should be_true
    hangup_and_confirm
  end

  it "should answer a call and let the farside hangup" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      sleep 1
      hangup
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer
    @call.next_event.should be_a_valid_hangup_event
  end
end
