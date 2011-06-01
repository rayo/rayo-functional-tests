require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Dial command" do
    it "Should place an outbound call" do
      pending('https://github.com/tropo/tropo2/issues/26')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      # Request an outbound call
      @tropo2.dial(:to => @config['tropo1']['call_destination'], :from => 'tel:+14155551212').should eql true
      @tropo2.read_event_queue.should be_a_valid_call_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should place an outbound call with a Caller ID set" do
      pending('https://github.com/tropo/tropo2/issues/26')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      @tropo2.dial({ :to        => @config['tropo1']['call_destination'], 
                     :from      => 'tel:+14155551212' }).should eql true
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
      call_event.headers[:from].match(/\A\<tel:\+14155551212\>/).should_not eql nil
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should place an outbound call and receive a ringing event" do
      pending('https://github.com/tropo/tropo2/issues/26')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
    
      @tropo2.dial({ :to        => @config['tropo1']['call_destination'], 
                     :from      => 'tel:+14155551212' }).should eql true
      call_event = @tropo2.read_event_queue
      ap call_event
      ap @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
      call_event.headers[:from].match(/\A\<tel:\+14155551212\>/).should_not eql nil
      ap @tropo2.read_event_queue
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should place an outbound call and then receive an answered event" do
      pending('https://github.com/tropo/tropo2/issues/26')
    end
    
    it "Should place an outbound call and then receive an unanswered event for calls not answered" do
      pending('https://github.com/tropo/tropo2/issues/26')
    end
  end
end