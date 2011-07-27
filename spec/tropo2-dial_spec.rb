require 'spec_helper'

describe "Dial command" do
  it "should place an outbound call, receive a ring event, receive a reject event and then hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      accept
      hangup
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_reject_event
  end

  it "should place an outbound call and then receive a reject event" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      reject
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri }
    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_reject_event
  end

  it "should place an outbound call and send SIP headers" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      ozone_testing_server.result = $currentCall.getHeader('x-tropo2-test')
      sleep 1
      hangup
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri,
                                       'x-tropo2-test'        => 'booyah!' }
    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_answered_event
    @call.next_event.should be_a_valid_hangup_event
    @tropo1.result.should eql 'booyah!'
  end

  it "should dial multiple calls" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      wait 100
      hangup
    TROPO_SCRIPT_CONTENT

    call1 = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri,
                                       'x-tropo2-test'        => 'booyah!' }
    call2 = @tropo2.dial :to      => @config['tropo1']['call_destination'],
                         :from    => 'tel:+14155551212',
                         :headers => { 'x-tropo2-drb-address' => @drb_server_uri,
                                       'x-tropo2-test'        => 'booyah!' }

    call1.ring_event.should be_a_valid_ringing_event
    call1.next_event.should be_a_valid_answered_event
    call1.next_event.should be_a_valid_hangup_event
    call1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should be_true

    call2.ring_event.should be_a_valid_ringing_event
    call2.next_event.should be_a_valid_answered_event
    call2.next_event.should be_a_valid_hangup_event
  end

  it "should get an error if we dial an invalid address" do
    lambda { @tropo2.dial :to      => 'foobar',
                          :from    => 'tel:+14155551212',
                          :headers => { 'x-tropo2-drb-address' => @drb_server_uri,
                                        'x-tropo2-test'        => 'booyah!' } }.should raise_error(Punchblock::Protocol::ProtocolError)
  end
end
