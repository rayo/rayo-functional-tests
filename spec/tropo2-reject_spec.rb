require 'spec_helper'

describe "Reject command" do
  it "Should reject with a declined reason" do
    pending 'https://github.com/tropo/tropo2/issues/81'
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.reject(:reason => :declined).should eql true
    call.next_event.should be_a_valid_reject_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
  end

  it "Should reject with a busy reason" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.reject(:reason => :busy).should eql true
    call.next_event.should be_a_valid_reject_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
  end

  it "Should reject with a error reason" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    call.reject(:reason => :error).should eql true
    call.next_event.should be_a_valid_reject_event

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
  end

  it "Should reject and raise an error due to an invalid reason" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
      wait #{@config['tropo1']['wait_to_hangup']}
    TROPO_SCRIPT_CONTENT
    @tropo1.place_call @config['tropo1']['session_url']

    call = @tropo2.get_call
    call.call_event.should be_a_valid_call_event
    lambda { call.reject :reason => :foobar }.should raise_error(ArgumentError)

    call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
  end
end
