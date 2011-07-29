require 'spec_helper'

describe "Record command" do
  before do
    @tropo1.add_latch :answered
    @tropo1.add_latch :spoke

    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      trigger_latch :answered
      say 'Hello world'
      trigger_latch :spoke
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer

    @tropo1.wait :answered

    record_command = @call.record
    record_command.should be_true

    @tropo1.wait :spoke
  end

  it "should record a call" do
    hangup_and_confirm do
      @call.next_event.should be_a_valid_complete_recording_event
    end
  end

  it "should record the correct content and return the correct filename"

  it "finishes recording when the maximum duration expires"

  it "finishes early if the initial timeout is exceeded"

  it "finishes early if the final timeout is exceeded"

  it "can be paused, resumed and stopped"
end
