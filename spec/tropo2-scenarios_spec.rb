require 'spec_helper'

describe "Call Scenarios" do
  it "Incoming call transferred in parallel" do
    # 1. A company receives a call in one of the virtual numbers (1.800.555.1212)
    place_call_with_script <<-CALL_SCRIPT
      call_tropo2
      sleep 3
    CALL_SCRIPT

    # 2. One number service answers the call and plays an announcement (selected from a predefined set or from the recordings made by user)
    get_call_and_answer
    call_output = @call.output(:audio => { :url => @config['audio_url'] }).should be_true

    # 3. While the announcement is being played the call is transferred to N employees in parallel (all the employees’ phones ring in parallel)
    @tropo1.script_content = <<-SCRIPT_CONTENT
      answer
      wait_to_hangup
    SCRIPT_CONTENT

    @employee1 = @tropo2.dial(:to       => @config['tropo1']['call_destination'],
                              :from     => 'tel:+14159998888',
                              :headers  => { 'x-tropo2-drb-address' => @drb_server_uri}).should be_true

    @employee2 = @tropo2.dial(:to       => @config['tropo1']['call_destination'],
                              :from     => 'tel:+14159998888',
                              :headers  => { 'x-tropo2-drb-address' => @drb_server_uri}).should be_true

    @employee3 = @tropo2.dial(:to       => @config['tropo1']['call_destination'],
                              :from     => 'tel:+14159998888',
                              :headers  => { 'x-tropo2-drb-address' => @drb_server_uri}).should be_true

    # 4. The possible answers for each of the phones are reject, busy, timeout (no answer), cancelled (connFu hangs that call before any other answer) or accepted
    #
    # Based on the above dial commands the client will start receiving progress events for all three calls.
    @employee1.ring_event.should be_a_valid_ringing_event
    @employee2.ring_event.should be_a_valid_ringing_event
    @employee3.ring_event.should be_a_valid_ringing_event

    # 5. One of them takes the call (employee1) [If none of them takes the call then play an announcement (selected from a predefined set or from the recordings made by user) and clear the call]
    @employee1.next_event.should be_a_valid_answered_event

    # 6. All other pending legs with employees are hung up
    @employee2.hangup.should be_true
    @employee3.hangup.should be_true

    # 7. The call is established end to end between the customer and employee1
    call_output.stop!.should be_true
    @call.next_event.should be_a_valid_stopped_output_event

    # Join employee1 to the customer
    @employee1.join(:other_call_id => @call.call_id).should be_true
    @call.next_event.should be_a_valid_joined_event
    @employee1.next_event.should be_a_valid_joined_event

    # 8. The customer hangs up, and we hangup employee1
    @call.next_event.should be_a_valid_hangup_event
    @employee1.hangup.should be_true
  end
end
