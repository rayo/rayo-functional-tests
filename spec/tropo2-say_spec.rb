require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Say command" do
    it "Should say something with TTS" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        ask 'One', { :choices     => 'yes, no',
                     :onBadChoice => lambda { ozone_testing_server.tropo_result = 'badchoice' },
                     :onChoice    => lambda { |event| ozone_testing_server.result = event.value  } }
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
  
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
  
      say_event = @tropo2.say 'yes'
      say_event.should be_a_valid_successful_say_event

      # Give time for the media transaction to complete 
      sleep @config['media_assertion_timeout']
  
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event

      # Validate the media worked properly
      @tropo1.result.should eql 'yes'
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should say an audio URL" do
      pending('https://github.com/tropo/tropo2/issues/9')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
  
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
  
      @tropo2.say_nonblocking 'http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3', :url
    
      #Wait for audio file to complete playing
      sleep 9
    
      say_event = @tropo2.read_event_queue
      say_event.should be_a_valid_successful_say_event
  
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should say SSML" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        ask 'One', { :choices     => 'one hundred, ireland',
                     :onBadChoice => lambda { ozone_testing_server.tropo_result = 'badchoice' },
                     :onChoice    => lambda { |event| ozone_testing_server.result = event.value  } }
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
  
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
  
      say_event = @tropo2.say '<say-as interpret-as="ordinal">100</say-as>', :ssml
      say_event.should be_a_valid_successful_say_event

      # Give time for the media transaction to complete 
      sleep @config['media_assertion_timeout']
  
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event

      # Validate the media worked properly
      @tropo1.result.should eql 'one hundred'
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should say some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop" do
      pending('https://github.com/tropo/punchblock/issues/10')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
  
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
    
      say_event = @tropo2.say 'http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3', :url
      ap say_event
    
      sleep 2
      ap say_event.pause 
      sleep 2
      ap say_event.resume
      sleep 2
      ap say_event.stop

      hangup_event = @tropo2.hangup(call_event)
      hangup_event.should be_a_valid_hangup_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end