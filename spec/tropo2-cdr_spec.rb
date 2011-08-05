require 'spec_helper'
require 'net/http'
require 'json'

describe "CDR Manager" do

  it "should create a CDR for an incoming call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    hangup_and_confirm
  end

  it "should create a CDR for an incoming call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer

    check_cdr_is_current_call

    hangup_and_confirm
  end

  it "should create a CDR for an outgoing call" do
    pending
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      wait_to_hangup
      hangup
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial tropo1_dial_options

    p active_cdrs
    p @call.call_event.call_id

    check_cdr_is_current_call

    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_reject_event
  end

  it "should create a CDR with transcript with all actions" do
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

    @call.say(:text => 'yes').should be_true

    wait_on_latch :responded

    @call.next_event.should be_a_valid_say_event

    check_cdr_is_current_call

    transcript = active_cdrs.first['transcript'].to_s

    transcript.should include("<offer")
    transcript.should include("<answer")
    transcript.should include("<say")
    transcript.should include("<complete")
    transcript.should include("<success")

    hangup_and_confirm

    @tropo1.result.should eql 'yes'
  end
end
