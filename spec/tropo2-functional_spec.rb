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
  
  describe "Dial" do
    it "Should place an outbound call" do
      pending('https://github.com/tropo/punchblock/issues/20')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      
      # Request an outbound call
      dial_event = @tropo2.dial :to => @config['tropo1']['call_destination']
      ap dial_event
      ap @tropo2.read_event_queue
      ap @tropo2.read_event_queue
      ap @tropo2.read_event_queue
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should place an outbound call to multiple destinations" do
      pending('https://github.com/tropo/punchblock/issues/20')
    end
    
    it "Should place an outbound call with a Caller ID set" do
      pending('https://github.com/tropo/punchblock/issues/20')
    end
  end
  
  describe "Reject and redirect" do
    it "Should reject a call" do
      pending('More testing')
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
      reject_event = @tropo2.reject
      ap reject_event
      reject_event.should be_a_valid_answer_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should redirect a call" do
      pending('Additional testing')
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      # Read the Tropo2 queue and validate the offer
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
      
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        answer
        wait #{@config['tropo1']['wait_to_hangup']}
      TROPO_SCRIPT_CONTENT
      
      # Send an answer to Tropo2
      redirect_event = @tropo2.redirect @config['tropo1']['call_destination']
      ap redirect_event
      ap @tropo2.read_event_queue
      ap @tropo2.read_event_queue
      ap @tropo2.read_event_queue
      redirect_event.should be_a_valid_answer_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
  
  describe "Say verb" do
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
    
      say_event = @tropo2.say '<say-as interpret-as="ordinal">100</say-as>'
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
  
  describe "Ask verb" do
    before(:all) do
      @grxml = <<-GRXML
        <grammar xmlns="http://www.w3.org/2001/06/grammar" root="MAINRULE">
            <rule id="MAINRULE"> 
                <one-of>
                    <item>
                        <item repeat="0-1"> need a</item>
                        <item repeat="0-1"> i need a</item>
                            <one-of> 
                                <item> clue </item>
                            </one-of>
                        <tag> out.concept = "clue";</tag> 
                    </item>
                    <item>
                        <item repeat="0-1"> have an</item>
                        <item repeat="0-1"> i have an</item>
                            <one-of> 
                                <item> answer </item>
                            </one-of>
                        <tag> out.concept = "answer";</tag> 
                    </item>
                    </one-of>
            </rule> 
        </grammar>
      GRXML
    end
    
    it "Should ask something with ASR and get the utterance back" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say 'yes'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
    
      ask_event = @tropo2.ask('One',{ :choices => 'yes, no' })
      ask_event.should be_a_valid_ask_event
    
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      # Validate the media worked properly
      ask_event.attributes[:utterance].should eql 'yes'
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask with an SSML as a prompt" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say 'yes'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
    
      @tropo2.ask_nonblocking('<say-as interpret-as="ordinal">100</say-as>', :choices => 'yes, no')
      sleep 6
      ask_event = @tropo2.read_event_queue
      ask_event.should be_a_valid_ask_event
    
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      # Validate the media worked properly
      ask_event.attributes[:utterance].should eql 'yes'
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask with a GRXML grammar" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 3
        say 'clue'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
      
      ask_event = @tropo2.ask('One', { :choices => @grxml, 
                                       :grammar => 'application/grammar+grxml' })
      ask_event.should be_a_valid_ask_event
    
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      # Validate the media worked properly
      ask_event.attributes[:utterance].should eql 'clue'
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask with an SSML prompt and a GRXML grammar" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 1
        ask 'clue', { :choices     => 'one hundred, ireland',
                      :onBadChoice => lambda { ozone_testing_server.tropo_result = 'badchoice' },
                      :onChoice    => lambda { |event| ozone_testing_server.result = event.value  } }
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
    
      ask_event = @tropo2.ask('<say-as interpret-as="ordinal">100</say-as>', { :choices => @grxml, 
                                                                               :grammar => 'application/grammar+grxml' })
      ask_event.should be_a_valid_ask_event
    
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      # Validate the media worked properly
      ask_event.attributes[:utterance].should eql 'clue'
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should timeout on an ask if a timeout is specified" do
      pending('https://github.com/tropo/punchblock/issues/16')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
      
      time = Time.now
      ask_event = @tropo2.ask('Yeap', { :choices => 'yes, no', 
                                        :timeout => 3 })
      seconds_elapsed = Time.now - time
      ap seconds_elapsed
      ap ask_event
      ask_event.should be_a_valid_ask_event
      
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
  
  describe "Conference verb" do
    it "Should put one caller in conference and then hangup" do
      pending('https://github.com/tropo/tropo2/issues/14')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
      
      conference_event = @tropo2.conference '1234'
      ap conference_event
      
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should put two callers into a conference and then hangup" do
      pending('https://github.com/tropo/tropo2/issues/14')
    end
    
    it "Should put two callers into a conference, validate media and hangup" do
      pending('https://github.com/tropo/tropo2/issues/14')
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