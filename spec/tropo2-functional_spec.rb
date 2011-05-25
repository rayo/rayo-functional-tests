require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

# startCallRecording 'http://tropo-audiofiles-to-s3.heroku.com/post_audio_to_s3?file_name=ozone2_testing.wav'

describe "Tropo2AutomatedFunctionalTesting" do  
  before(:all) do
    @config = YAML.load(File.open('config/config.yml'))

    ap "Starting Tropo2Driver to manage events over XMPP."
    @tropo2 = Tropo2Utilities::Tropo2Driver.new({ :username         => @config['tropo2_server']['jid'],
                                                  :password         => @config['tropo2_server']['password'],
                                                  :wire_logger      => Logger.new(@config['tropo2_server']['wire_log']),
                                                  :transport_logger => Logger.new(@config['tropo2_server']['transport_log']),
                                                  :log_level        => Logger::DEBUG })
                                 
    ap "Starting Tropo1Driver to host scripts via DRb and launch calls via HTTP."
    @tropo1 = Tropo2Utilities::Tropo1Driver.new(@config['tropo1']['druby_uri'])
    
    status = @tropo2.read_event_queue(@config['tropo2_queue']['connection_timeout'])
    status.should eql 'CONNECTED'
    ap "Connected to the Tropo2 XMPP Server"
    ap "Starting tests..."
  end
  
  after(:each) do
    @tropo2.read_event_queue(@config['tropo2_queue']['last_stanza_timeout']) until @tropo2.event_queue.empty?
  end
  
  describe "Call answer and hangup handling" do
    it "Should a call arrives and then hangup" do
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 3000
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
  
    it "Should accept, answer and hangup" do 
      @tropo1.script_content = <<-TROPO_SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        say 'Hello world'
        wait 3000
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
        
    it "Should answer a call and let the farside hangup" do
      pending('This bug being fixed: https://evolution.voxeo.com/ticket/1423272')
    end
  end
  
  describe "Say verb" do
    it "Should say something with TTS" do
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        ask 'One', { :choices     => 'yes, no',
                     :onBadChoice => lambda { ozone_testing_server.tropo_result = 'badchoice' },
                     :onChoice    => lambda { |event| ozone_testing_server.result = event.value  } }
        wait 3000
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
    
    it "Should say some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop" do
      pending('Punchblock object model')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        wait 30000
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
    
      answer_event = @tropo2.answer(call_event)
      answer_event.should be_a_valid_answer_event
      
      # @blather_stream.send_data generator.say_audio :url => @config['audio_url']
      # say_reply = read_queue
      # jid = say_reply[:stanza]['iq']['ref']['jid']
      # 
      # sleep 2
      # 
      # @blather_stream.send_data generator.pause 'say', jid
      # pause_reply = read_queue
      # 
      # sleep 2
      # 
      # @blather_stream.send_data generator.resume 'say', jid
      # resume_reply = read_queue
      # 
      # sleep 2
      # 
      # @blather_stream.send_data generator.stop 'say', jid
      # stop_reply = read_queue
      # say = read_queue
      # 
      # @blather_stream.send_data generator.hangup
      # hangup_reply = read_queue
      # hangup = read_queue
      # 
      # validate_answer answer_reply, answer
      # validate_stopped_say say_reply, say
      # validate_control pause_reply
      # validate_control resume_reply
      # validate_control stop_reply
      # validate_hangup hangup_reply, hangup
      hangup_event = @tropo2.hangup(call_event)
      hangup_event.should be_a_valid_hangup_event
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
  
  describe "Ask verb" do
    it "Should ask something with ASR and get the utterance back" do
      pending('https://github.com/tropo/punchblock/issues/8')
      @tropo1.script_content = <<-SCRIPT_CONTENT
        call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
        sleep #{@config['media_assertion_timeout']}.to_i
        say 'yes'
        wait 3000
      SCRIPT_CONTENT
      @tropo1.place_call @config['tropo1']['session_url']
    
      call_event = @tropo2.read_event_queue
      call_event.should be_a_valid_call_event
        
      answer_event = @tropo2.answer
      answer_event.should be_a_valid_answer_event
    
      ask_event = @tropo2.ask('One', 'yes, no')
      ap ask_event
      ask_event.should be_a_valid_ask_event
    
      hangup_event = @tropo2.hangup
      hangup_event.should be_a_valid_hangup_event
      
      # Validate the media worked properly
      #@tropo1.result.should eql 'yes'
      
      @tropo2.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should eql true
    end
  end
  
  describe "Transfer verb" do
    it "Should answer a call and then transfer it" do
      pending('Punchblock object model')
  #     pending('Reported issue to Jose')
  #     @tropo_connector.script_content = <<-SCRIPT_CONTENT
  #       call 'sip:' + '#{@config['tropo2_server']['sip_uri']}'
  #       wait 30000
  #     SCRIPT_CONTENT
  #     
  #     launch_call
  #     offer = @ozone_queue.pop
  #     
  #     generator = OzoneStanzas::StanzaGenerator.new(:client_jid => offer[:stanza]['iq']['to'], :call_id => offer[:stanza]['iq']['from'])
  #     @blather_stream.send_data generator.answer
  #     answer_reply, answer = read_queue, read_queue
  #     
  #     @tropo_connector.script_content = <<-SCRIPT_CONTENT
  #       answer
  #       wait 30000
  #     SCRIPT_CONTENT
  #     
  #     @blather_stream.send_data generator.transfer :to => @config['tropo1']['call_destination']
  #     transfer_reply, transfer = read_queue, read_queue
  #     
  #     ap transfer_reply
  #     ap transfer
    end
  end
end