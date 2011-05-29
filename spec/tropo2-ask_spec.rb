require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Tropo2AutomatedFunctionalTesting" do
  describe "Ask command" do
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
      pending('https://github.com/tropo/tropo2/issues/18')
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
      pending('https://github.com/tropo/tropo2/issues/18')
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
end