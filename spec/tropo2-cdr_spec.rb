require 'spec_helper'
require 'net/http'
require 'json'

describe "CDR Manager" do

  let :active_cdrs do
    response = Net::HTTP.get_response(@config['tropo2_server']['server'], '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs', @config['tropo2_server']['port'].to_i)
    JSON.parse(response.body)['value']
  end

  def check_cdr_is_current_call
    active_cdrs.should have(1).record
    active_cdrs.first['callId'].should eql @call.call_event.call_id
  end

  it "Should create a CDR for an incoming call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer false
    hangup_and_confirm
  end

  it "Should create a CDR for an incoming call" do
    place_call_with_script <<-TROPO_SCRIPT_CONTENT
      call_tropo2
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    get_call_and_answer

    check_cdr_is_current_call

    hangup_and_confirm
  end

  it "Should create a CDR for an outgoing call" do
    pending do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        accept
        wait_to_hangup
        hangup
      TROPO_SCRIPT_CONTENT

      @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                           :from    => 'tel:+14155551212',
                           :headers => { 'x-tropo2-drb-address' => @drb_server_uri }

      p active_cdrs
      p @call.call_event.call_id

      check_cdr_is_current_call

      @call.ring_event.should be_a_valid_ringing_event
      @call.next_event.should be_a_valid_reject_event
    end
  end

  it "Should create a CDR with transcript with all actions" do
    @tropo1.add_latch :responded

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

    @tropo1.wait :responded

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
