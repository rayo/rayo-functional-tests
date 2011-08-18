require 'spec_helper'

describe "Output component" do
  it "should output something with TTS", :'load-suite' => true do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      ask 'One7', :choices     => 'yes, no',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.output(:text => 'yes').should have_executed_correctly

    wait_on_latch :responded

    @call.next_event.should be_a_valid_output_event

    hangup_and_confirm

    @tropo1.result.should eql 'yes'
  end

  it "should output an audio URL and hangup", :'load-suite' => true do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup 4
    SCRIPT_CONTENT

    get_call_and_answer

    @call.output(:audio => { :url => @config['audio_url'] }).should have_executed_correctly

    sleep 9 #Wait for audio file to complete playing

    @call.next_event.should be_a_valid_output_event

    hangup_and_confirm
  end

  it "should output SSML", :'load-suite' => true do
    add_latch :responded

    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      ask 'One8', :choices     => 'one hundred, ireland',
                  :onBadChoice => lambda { ozone_testing_server.result = 'badchoice' },
                  :onChoice    => lambda { |event| ozone_testing_server.result = event.value  }
      trigger_latch :responded
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.output(:ssml => '<say-as interpret-as="ordinal">100</say-as>').should have_executed_correctly

    wait_on_latch :responded

    @call.next_event.should be_a_valid_output_event

    hangup_and_confirm

    @tropo1.result.should eql 'one hundred'
  end

  it "should throw an error on invalid SSML" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.output(:ssml => '<output-as interpret-as="ordinal">100</output-as>').should have_executed_correctly

    @call.next_event.should be_a_valid_complete_error_event.with_message("Invalid SSML: cvc-elt.1: Cannot find the declaration of element 'output-as'.")

    hangup_and_confirm
  end

  it "should output some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup 4
    SCRIPT_CONTENT

    get_call_and_answer

    output_command = @call.output :audio => { :url => @config['audio_url'] }

    sleep 2
    output_command.pause!
    sleep 2
    output_command.resume!
    sleep 2
    output_command.stop!

    @call.next_event.should be_a_valid_stopped_output_event

    hangup_and_confirm
  end

  it "should output an audio URL and get a stop event" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep 2
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    @call.output(:audio => { :url => @config['audio_url'] }).should have_executed_correctly

    @call.next_event.should be_a_valid_complete_hangup_event
    @call.next_event.should be_a_valid_hangup_event
  end

  it "should error on a output and return a complete event" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_tropo2
      sleep 2
      hangup
    SCRIPT_CONTENT

    get_call_and_answer

    lambda { @call.output :text => '' }.should raise_error(Punchblock::ProtocolError)

    @call.next_event.should be_a_valid_hangup_event
  end

  describe "audio manipulation" do
    before do
      place_call_with_script <<-SCRIPT_CONTENT
        call_tropo2
        wait_to_hangup 2
      SCRIPT_CONTENT

      get_call_and_answer
      @output_command = @call.output(:audio => { :url => @config['audio_url'] }).should have_executed_correctly
    end

    it "can seek within the output" do
      pending 'This requires a new build of Prism, which depends on Tropo2 using the new XMPP stack'
      @output_command.seek!(:direction => :forward, :amount => 3000).should have_executed_correctly
      @output_command.seek!(:direction => :back, :amount => 3000).should have_executed_correctly
    end

    it "can speed up output" do
      @output_command.speed_up!.should have_executed_correctly
      @output_command.speed_up!.should have_executed_correctly
    end

    it "can slow down output" do
      @output_command.slow_down!.should have_executed_correctly
      @output_command.slow_down!.should have_executed_correctly
    end

    it "can increase output volume" do
      @output_command.volume_up!.should have_executed_correctly
      @output_command.volume_up!.should have_executed_correctly
    end

    it "can decrease output volume" do
      @output_command.volume_down!.should have_executed_correctly
      @output_command.volume_down!.should have_executed_correctly
    end

    after do
      @output_command.stop!.should have_executed_correctly
      @call.next_event.should be_a_valid_stopped_output_event
      hangup_and_confirm
    end
  end
end
