require 'spec_helper'

describe "Transfer verb" do
  it "should answer a call and then transfer it" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    # Set a script that handles the incoming Xfer from Tropo2
    @tropo1.script_content = <<-SCRIPT_CONTENT
      answer
      wait 1000
      hangup
    SCRIPT_CONTENT

    transfer = @call.transfer(:to      => @config['tropo1']['call_destination'],
                              :headers => { 'x-rayo-drb-address' => @drb_server_uri }).should have_executed_correctly

    # The spec does not call for an <answered/> event here but may soon
    #@call.next_event.should be_a_valid_answered_event

    transfer.next_event.should be_a_valid_transfer_event
    hangup_and_confirm
  end

  it "should try to transfer but get a timeout" do
    place_call_with_script <<-SCRIPT_CONTENT
      call_rayo
      wait_to_hangup
    SCRIPT_CONTENT

    get_call_and_answer

    # Set a script that handles the incoming Xfer from Tropo2
    @tropo1.script_content = <<-SCRIPT_CONTENT
      wait 5000
    SCRIPT_CONTENT

    transfer = @call.transfer(:to      => @config['tropo1']['call_destination'],
                              :timeout => 2000,
                              :headers => { 'x-rayo-drb-address' => @drb_server_uri }).should have_executed_correctly

    transfer.next_event.should be_a_valid_transfer_timeout_event
    hangup_and_confirm
  end
end
