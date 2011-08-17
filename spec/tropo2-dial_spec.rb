require 'spec_helper'

describe "Dial command" do
  it "should place an outbound call, receive a ring event, receive a reject event and then hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      hangup
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial tropo1_dial_options
    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_reject_event
  end

  it "should place an outbound call and then receive a reject event" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      reject
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial tropo1_dial_options
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

    dial_options = tropo1_dial_options
    dial_options[:headers]['x-tropo2-test'] = 'booyah!'

    @call = @tropo2.dial dial_options
    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_answered_event
    @call.next_event.should be_a_valid_hangup_event
    @tropo1.result.should eql 'booyah!'
  end

  it "should place an outbound call and then hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    @call = @tropo2.dial tropo1_dial_options
    @call.ring_event.should be_a_valid_ringing_event
    @call.next_event.should be_a_valid_answered_event
    hangup_and_confirm
  end

  it "should dial multiple calls" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      wait 100
      hangup
    TROPO_SCRIPT_CONTENT

    call1 = @tropo2.dial tropo1_dial_options
    call2 = @tropo2.dial tropo1_dial_options

    call1.ring_event.should be_a_valid_ringing_event
    call1.next_event.should be_a_valid_answered_event
    call1.next_event.should be_a_valid_hangup_event
    call1.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true

    call2.ring_event.should be_a_valid_ringing_event
    call2.next_event.should be_a_valid_answered_event
    call2.next_event.should be_a_valid_hangup_event
    call2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true
  end

  it "should get an error if we dial an invalid address" do
    lambda { @tropo2.dial tropo1_dial_options.merge(:to => 'foobar') }.should raise_error(Punchblock::ProtocolError)
  end

  describe "when dialing another Rayo user" do
    before(:all) do
      @client2 = RSpecRayo::RayoDriver.new :username         => ENV['TROPO2_ALT_JID'] || $config['tropo2_server']['alt_jid'] || random_jid,
                                           :password         => ENV['TROPO2_ALT_PASSWORD'] || $config['tropo2_server']['alt_password'],
                                           :wire_logger      => Logger.new($config['tropo2_server']['wire_log']),
                                           :transport_logger => Logger.new($config['tropo2_server']['transport_log']),
                                           :log_level        => Logger::DEBUG,
                                           :queue_timeout    => $config['tropo2_queue']['connection_timeout'],
                                           :write_timeout    => $config['tropo2_server']['write_timeout']

      @client2.read_queue(@client2.event_queue).should == 'CONNECTED'
      @client2.start_event_dispatcher
    end

    it "should direct events to the dialing party" do
      @call1 = @tropo2.dial :to => 'sip:' + $config['tropo2_server']['alt_sip_uri'], :from => 'tel:+14155551212'

      @call2 = @client2.get_call
      @call2.call_event.should be_a_valid_offer_event
      @call2.answer.should be_true

      @call1.ring_event.should be_a_valid_ringing_event
      @call1.next_event.should be_a_valid_answered_event

      @call1.output(:text => 'Hello').should be_true
      @call1.next_event.should be_a_valid_output_event

      hangup_and_confirm @call1

      @call2.next_event.should be_a_valid_hangup_event
    end

    after do
      begin
        @client2.read_event_queue(@config['tropo2_queue']['last_stanza_timeout']) until @tropo2.event_queue.empty?
      ensure
        @client2.cleanup_calls
      end
    end
  end
end
