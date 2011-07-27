require 'spec_helper'
require 'net/http'
require 'json'

describe "CDR Manager" do

  it "Should create a CDR for an incoming call" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

	get_call_and_answer
  
    server = @config['tropo2_server']['server']
    port = @config['tropo2_server']['port'].to_i  
    res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs/0/callId', port)
    json = JSON.parse res.body
    callId = json['value']
	callId.should eql @call.call_event.call_id
    
    hangup_and_confirm
  end
  
  it "Should create a CDR for an outgoing call" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      accept      
      wait #{@config['tropo1']['wait_to_hangup']}
      hangup
    TROPO_SCRIPT_CONTENT
    
    @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
    
    server = @config['tropo2_server']['server']
    port = @config['tropo2_server']['port'].to_i  
    res = Net::HTTP.get_response(server, '/tropo2/jmx/read/com.tropo:Type=Cdrs/ActiveCDRs/0/callId', port)
    json = JSON.parse res.body
    callId = json['value']
	callId.should eql @call.call_event.call_id

    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_reject_event    
  end
end
