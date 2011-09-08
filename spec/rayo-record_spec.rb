require 'spec_helper'

describe "Record command" do
  let(:record_options) { {} }
  let :tropo1_script do
    <<-TROPO_SCRIPT_CONTENT
      call_rayo
      trigger_latch :answered
      say 'Hello world'
      trigger_latch :spoke
      wait_to_hangup
    TROPO_SCRIPT_CONTENT
  end

  before do
    add_latch :answered, :spoke

    place_call_with_script tropo1_script

    get_call_and_answer

    wait_on_latch :answered

    @record_command = @call.record record_options
    @record_command.should have_executed_correctly
  end

  it "should record a call", :'load-suite' => true do
    wait_on_latch :spoke
    hangup_and_confirm do
      @record_command.next_event.should be_a_valid_complete_recording_event
    end
  end

  it "should record the correct content and return the correct filename" do
    wait_on_latch :spoke
    hangup_and_confirm do
      @recording = @record_command.next_event
      @recording.should be_a_valid_complete_recording_event
    end

    @call.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true

    add_latch :check_answered, :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      ask 'One8', :choices     => 'hello world, thanks frank',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup 2
    SCRIPT_CONTENT

    get_call_and_answer

    output = @call.output(:ssml => audio_ssml(:url => @recording.recording.uri)).should have_executed_correctly

    wait_on_latch :responded
    output.next_event.should be_a_valid_output_event
    hangup_and_confirm
    @tropo1.result.should == 'hello world'
  end

  it "finishes early if the initial timeout is exceeded"

  it "finishes early if the final timeout is exceeded"

  describe "with a long running call" do
    let :tropo1_script do
      <<-TROPO_SCRIPT_CONTENT
        call_rayo
        trigger_latch :answered
        3.times do
          say 'Hello world'
          sleep 2
        end
        trigger_latch :spoke
        wait_to_hangup
      TROPO_SCRIPT_CONTENT
    end

    describe "with a maximum duration of 2 seconds" do
      let(:record_options) { {:max_duration => 2000} }

      it "finishes recording when the maximum duration expires" do
        @record_command.next_event.should be_a_valid_stopped_recording_event
        hangup_and_confirm
      end
    end

    it "can be paused, resumed and stopped" do
      sleep 2
      @record_command.pause!.should have_executed_correctly
      sleep 2
      @record_command.resume!.should have_executed_correctly
      sleep 2
      @record_command.stop!.should have_executed_correctly

      @record_command.next_event.should be_a_valid_stopped_recording_event

      hangup_and_confirm
    end
  end
end