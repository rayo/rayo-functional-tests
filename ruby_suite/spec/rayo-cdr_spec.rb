require 'spec_helper'
require 'net/http'
require 'json'

describe "CDR Manager" do

  it "should create a CDR for an incoming call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_rayo
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    hangup_and_confirm
  end

  it "should create a CDR for an incoming call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_rayo
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer

    check_cdr_is_current_call

    hangup_and_confirm
  end

  it "should create a CDR for an outgoing call" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      sleep 1
      hangup
    TROPO_SCRIPT_CONTENT

    @call = @rayo.dial(tropo1_dial_options).should have_dialed_correctly

    check_cdr_is_current_call

    @call.next_event.should be_a_valid_reject_event
  end

  it "should create a CDR with transcript with all actions" do
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
