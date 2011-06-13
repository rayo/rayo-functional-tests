$LOAD_PATH.unshift(File.join(File.dirname(__FILE__), '..', 'lib'))
$LOAD_PATH.unshift(File.dirname(__FILE__))
%w{
  timeout
  rspec
  yaml
  thread
  awesome_print
  drb
  logger
  punchblock
  rspec-tropo2
  }.each { |lib| require lib }
  # /Users/jsgoecke/Dropbox/Development/punchblock/lib/punchblock
  # /Users/jsgoecke/Dropbox/Development/rspec-tropo2/lib/rspec-tropo2

Thread.abort_on_exception = true

RSpec.configure do |config|
  config.before(:all) do
    @config = YAML.load(File.open('config/config.yml'))

    @tropo2 = Tropo2Utilities::Tropo2Driver.new({ :username         => @config['tropo2_server']['jid'],
                                                  :password         => @config['tropo2_server']['password'],
                                                  :wire_logger      => Logger.new(@config['tropo2_server']['wire_log']),
                                                  :transport_logger => Logger.new(@config['tropo2_server']['transport_log']),
                                                  :log_level        => Logger::DEBUG,
                                                  :queue_timeout    => @config['tropo2_queue']['connection_timeout'] })
                               
    @tropo1 = Tropo2Utilities::Tropo1Driver.new(@config['tropo1']['druby_uri'])
    
    status = @tropo2.read_event_queue
    abort 'Could not connect to Prism XMPP Server. Aborting!' if status != 'CONNECTED'
    @tropo2.start_event_dispatcher
  end
  
  config.after(:each) do
    @tropo2.calls = {}
    @tropo2.read_event_queue(@config['tropo2_queue']['last_stanza_timeout']) until @tropo2.event_queue.empty?
  end
  
  config.after(:all) do
    begin
      @tropo1.drb.stop_service
    rescue
      puts 'Upstream error, DRb not running from previous test.'
    end
  end
end