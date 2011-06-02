require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

# startCallRecording 'http://tropo-audiofiles-to-s3.heroku.com/post_audio_to_s3?file_name=ozone2_testing.wav'

describe "Tropo2AutomatedFunctionalTesting" do  
  describe "Call accept, answer and hangup handling" do
    it "Should receive a call and then hangup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end

    it "Should answer and hangup" do 
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.answer.should eql true
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should throw an error if we try to answer a call that is hungup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.answer.should eql true
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      
      begin
        @tropo2.answer
      rescue => error
        error.class.should eql Punchblock::Transport::TransportError 
      end
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should accept and hangup" do 
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.accept.should eql true
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should answer a call and let the farside hangup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 1
        hangup
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      @tropo2.read_event_queue.should be_a_valid_call_event
      @tropo2.answer.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end