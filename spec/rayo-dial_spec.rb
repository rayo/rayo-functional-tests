require 'spec_helper'

describe "Dial command" do
  it "should place an outbound call, receive a ring event, receive a reject event and then hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      hangup
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    @call = @rayo.dial(tropo1_dial_options).should have_dialed_correctly
    @call.next_event.should be_a_valid_reject_event
  end

  it "should place an outbound call and then receive a reject event" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      reject
    TROPO_SCRIPT_CONTENT

    @call = @rayo.dial(tropo1_dial_options).should have_dialed_correctly
    @call.next_event.should be_a_valid_reject_event
  end

  it "should place an outbound call and send SIP headers" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      ozone_testing_server.result = $currentCall.getHeader('x-rayo-test')
      sleep 1
      hangup
    TROPO_SCRIPT_CONTENT

    dial_options = tropo1_dial_options
    dial_options[:headers]['x-rayo-test'] = 'booyah!'

    @call = @rayo.dial(dial_options).should have_dialed_correctly
    @call.next_event.should be_a_valid_answered_event
    @call.next_event.should be_a_valid_hangup_event
    @tropo1.result.should eql 'booyah!'
  end

  it "should place an outbound call which is answered and then hangup" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      wait_to_hangup
    TROPO_SCRIPT_CONTENT

    @call = @rayo.dial(tropo1_dial_options).should have_dialed_correctly
    @call.next_event.should be_a_valid_answered_event
    hangup_and_confirm
  end

  it "should abort a dial before it is accepted/answered" do
    @call = @rayo.dial tropo1_dial_options
    hangup_and_confirm
  end

  it "should dial multiple calls" do
    @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
      answer
      wait 100
      hangup
    TROPO_SCRIPT_CONTENT

    call1 = @rayo.dial(tropo1_dial_options).should have_dialed_correctly
    call1.next_event.should be_a_valid_answered_event
    call1.next_event.should be_a_valid_hangup_event
    call1.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true

    call2 = @rayo.dial(tropo1_dial_options).should have_dialed_correctly
    call2.next_event.should be_a_valid_answered_event
    call2.next_event.should be_a_valid_hangup_event
    call2.last_event?(@config['rayo_queue']['last_stanza_timeout']).should == true
  end

  it "should get an error if we dial an invalid address" do
    lambda { @rayo.dial tropo1_dial_options.merge(:to => 'foobar') }.should raise_error(Punchblock::ProtocolError)
  end

  describe "when dialing another Rayo user" do
    before(:all) do
      @client2 = RSpecRayo::RayoDriver.new :username         => ENV['TROPO2_ALT_JID'] || $config['rayo_server']['alt_jid'] || random_jid,
                                           :password         => ENV['TROPO2_ALT_PASSWORD'] || $config['rayo_server']['alt_password'],
                                           :wire_logger      => Logger.new($config['rayo_server']['wire_log']),
                                           :transport_logger => Logger.new($config['rayo_server']['transport_log']),
                                           :log_level        => Logger::DEBUG,
                                           :queue_timeout    => $config['rayo_queue']['connection_timeout'],
                                           :write_timeout    => $config['rayo_server']['write_timeout']

      @client2.read_queue(@client2.event_queue).should be_a Punchblock::Connection::Connected
      @client2.start_event_dispatcher
    end

    it "should direct events to the dialing party" do
      @call1 = @rayo.dial :to => 'sip:' + $config['rayo_server']['alt_sip_uri'], :from => 'tel:+14155551212'

      @call2 = @client2.get_call
      @call2.call_event.should be_a_valid_offer_event
      @call2.answer.should have_executed_correctly

      @call1.ring_event.should be_a_valid_ringing_event
      @call1.next_event.should be_a_valid_answered_event

      output = @call1.output(:text => 'Hello').should have_executed_correctly
      output.next_event.should be_a_valid_output_event

      hangup_and_confirm @call1

      @call2.next_event.should be_a_valid_hangup_event
    end

    after do
      begin
        @client2.read_event_queue(@config['rayo_queue']['last_stanza_timeout']) until @rayo.event_queue.empty?
      ensure
        @client2.cleanup_calls
      end
    end
  end
end
