require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

# startCallRecording 'http://tropo-audiofiles-to-s3.heroku.com/post_audio_to_s3?file_name=ozone2_testing.wav'

describe "Tropo2AutomatedFunctionalTesting" do  
  describe "Call accept, answer and hangup handling" do
    it "Should receive a call arrives and then hangup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      # Read the Tropo2 queue and validate the offer
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event

      # Send a hangup to Tropo2
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end

    it "Should answer and hangup" do 
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      # Read the Tropo2 queue and validate the offer
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
    
      # Send an answer to Tropo2
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
      
      # Send a hangup to Tropo2
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should accept and hangup" do 
      pending('https://github.com/tropo/tropo2/issues/15')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      # Read the Tropo2 queue and validate the offer
      call_event = @tropo2.read_event_queue
      ap call_event
      call_event.should be_a_valid_call_event
    
      # Send an answer to Tropo2
      accept_event = @tropo2.accept
      ap accept_event
      accept_event.should be_a_valid_answer_event
      
      # Send a hangup to Tropo2
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
        
    it "Should answer a call and let the farside hangup" do
      pending('This bug being fixed: https://evolution.voxeo.com/ticket/1423272')
    end
  end
  
  describe "Transfer verb" do
    it "Should answer a call and then transfer it" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']

      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
      
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
      
      # Set a script that handles the incoming Xfer from Tropo2
      @tropo1.script_content = <<-SCRIPT_CONTENT
        answer
        wait 30000
      SCRIPT_CONTENT
      
      call_event = @tropo2.transfer(@config['tropo1']['call_destination'])
      call_event.should be_a_valid_call_event
      call_event.headers[:to].should eql @config['tropo1']['call_destination']
    end
    
    it "Should try to transfer but get a timeout" do
      pending('Implementation')
    end
  end
end