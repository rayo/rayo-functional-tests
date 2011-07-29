require 'spec_helper'
require 'net/http'
require 'json'

describe "CDR Manager" do

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

    server = @config['tropo2_server']['server']
    port = @config['tropo2_server']['port'].to_i
    res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs', port)
    json = JSON.parse res.body
    activeCdrs = json['value']

    activeCdrs.first['callId'].should eql @call.call_event.call_id

    hangup_and_confirm
  end

  it "Should create a CDR for an outgoing call" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      accept
      wait_to_hangup
      hangup
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri }

    server = @config['tropo2_server']['server']
    port = @config['tropo2_server']['port'].to_i
    res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs', port)
    json = JSON.parse res.body
    activeCdrs = json['value']

    p activeCdrs
    p @call.call_event.call_id

    activeCdrs.first['callId'].should eql @call.call_event.call_id

    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_reject_event
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

    server = @config['tropo2_server']['server']
    port = @config['tropo2_server']['port'].to_i
    res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs', port)
    json = JSON.parse res.body
    activeCdrs = json['value']

    activeCdrs.first['callId'].should eql @call.call_event.call_id
    transcript = activeCdrs.first['transcript'].to_s

    transcript.should include("<offer")
    transcript.should include("<answer")
    transcript.should include("<say")
    transcript.should include("<complete")
    transcript.should include("<success")

    hangup_and_confirm

    @tropo1.result.should eql 'yes'
  end
end
