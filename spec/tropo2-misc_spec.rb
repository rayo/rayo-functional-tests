require 'spec_helper'

describe "Misc commands" do
  it "can mute & unmute the call" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.mute.should be_true
    @call.unmute.should be_true
    hangup_and_confirm
  end
end
