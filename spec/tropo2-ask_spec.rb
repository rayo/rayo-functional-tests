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
      #pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say 'yes'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
  
      call.ask({ :text    => 'One', 
                 :choices => 'yes, no' }).should eql true
      
      sleep @config['media_assertion_timeout']
      
      ask_event = call.next_event
      ask_event.should be_a_valid_ask_event
      ask_event.reason.utterance.should eql 'yes'
      
      call.hangup.should eql true
      call.next_event.should be_a_valid_hangup_event
    
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should ask with an SSML as a prompt" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say 'yes'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
  
      call.ask(:text    => '<say-as interpret-as="ordinal">100</say-as>', 
               :choices => 'yes, no').should eql true
      
      sleep 6
      
      ask_event = call.next_event
      ask_event.should be_a_valid_ask_event
      ask_event.reason.utterance.should eql 'yes'
  
      call.hangup.should eql true
      call.next_event.should be_a_valid_hangup_event
    
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should ask with a GRXML grammar" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 3
        say 'clue'
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
    
      call.ask({ :text    => 'One', 
                 :choices => @grxml, 
                 :grammar => 'application/grammar+grxml' }).should eql true

      ask_event = call.next_event
      ask_event.should be_a_valid_ask_event
      ask_event.reason.utterance.should eql 'clue'
      
      call.hangup.should eql true
      call.next_event.should be_a_valid_hangup_event
    
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  
    it "Should ask with an SSML prompt and a GRXML grammar" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep 1
        ask 'clue', { :choices     => 'one hundred, ireland',
                      :onBadChoice => lambda { ozone_testing_server.tropo_result = 'badchoice' },
                      :onChoice    => lambda { |event| ozone_testing_server.result = event.value  } }
        wait #{@config['tropo1']['wait_to_hangup']}
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
  
      call.ask({ :text    => '<say-as interpret-as="ordinal">100</say-as>', 
                 :choices => @grxml,
                 :grammar => 'application/grammar+grxml' }).should eql true
                 
      ask_event = call.next_event
      ask_event.should be_a_valid_ask_event
      ask_event.reason.utterance.should eql 'clue'
      
      call.hangup.should eql true
      call.next_event.should be_a_valid_hangup_event
    
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask and get a NOINPUT event" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 5000
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
    
      call.ask({ :text    => 'Yeap', 
                 :choices => 'yes, no',
                 :timeout => 2000 })
                 
      call.next_event.should be_a_valid_noinput_event
      call.next_event.should be_a_valid_hangup_event
      
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask and get a NOMATCH event with min_confidence set to 1" do
      pending('https://github.com/tropo/tropo2/issues/30')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 1000
        say 'elephant'
        wait #{@config['tropo1']['wait_to_hangup']}
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call = @tropo2.get_call
      call.next_event.should be_a_valid_call_event
      call.answer.should eql true
    
      call.ask({ :text           => 'Yeap', 
                 :choices        => 'red, green',
                 :timeout        => 2000,
                 :min_confidence => '1' })

      call.next_event.should be_a_valid_nomatch_event
      call.next_event.should be_a_valid_hangup_event
      
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask and get a STOP if the farside hangs up before the command complete" do
      pending('https://github.com/tropo/tropo2/issues/32')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 1000
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
  
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
    
      call.ask('Yeap', { :choices => 'red, green' })
      call.next_event.should be_a_valid_stopped_ask_event
      call.next_event.should be_a_valid_hangup_event
      
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
    
    it "Should ask something with an invalid grammar and get an error back" do
      pending('https://github.com/tropo/tropo2/issues/53')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 2000
        hangup
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
      
      call = @tropo2.get_call
      call.call_event.should be_a_valid_call_event
      call.answer.should eql true
  
      lambda { call.ask({ :text => 'One', :choices => '<grammar>' }) }.should raise_error(TransportError)
      
      ap call.next_event.reason.should eql :error
    
      call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
end