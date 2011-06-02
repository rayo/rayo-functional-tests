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
  
      @tropo2.read_event_queue.should be_a_valid_call_event  
      @tropo2.answer.should eql true
  
      @tropo2.say('yes').should eql true
      
      sleep @config['media_assertion_timeout']
      
      @tropo2.read_event_queue.should be_a_valid_successful_say_event
  
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event

      @tropo1.result.should eql 'yes'
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should say an audio URL" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      @tropo2.read_event_queue.should be_a_valid_call_event  
      @tropo2.answer.should eql true
  
      @tropo2.say('http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3', :url).should eql true
    
      #Wait for audio file to complete playing
      sleep 9

      @tropo2.read_event_queue.should be_a_valid_successful_say_event
      
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event
    
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
  
      @tropo2.read_event_queue.should be_a_valid_call_event  
      @tropo2.answer.should eql true
  
      @tropo2.say('<say-as interpret-as="ordinal">100</say-as>', :ssml).should eql true
      @tropo2.read_event_queue.should be_a_valid_successful_say_event

      sleep @config['media_assertion_timeout']
  
      @tropo2.hangup.should eql true
      @tropo2.read_event_queue.should be_a_valid_hangup_event

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
  
      @tropo2.read_event_queue.should be_a_valid_call_event  
      @tropo2.answer.should eql true
    
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
    
    it "Should say an audio URL and get a stop event" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 2
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      @tropo2.read_event_queue.should be_a_valid_call_event  
      @tropo2.answer.should eql true
  
      @tropo2.say('http://dl.dropbox.com/u/25511/Voxeo/troporocks.mp3', :url).should eql true
      
      @tropo2.read_event_queue.should be_a_valid_stopped_say_event
      
      @tropo2.read_event_queue.should be_a_valid_hangup_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should error on a say and return a complete event" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 2
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      @tropo2.read_event_queue.should be_a_valid_call_event  
      @tropo2.answer.should eql true
  
      begin
        @tropo2.say('')
      rescue => error
        error.class.should eql Punchblock::Transport::TransportError
      end
      
      @tropo2.read_event_queue.should be_a_valid_hangup_event
    
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end