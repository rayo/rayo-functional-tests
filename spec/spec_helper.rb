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

Thread.abort_on_exception = true

RSpec.configure do |config|
  config.filter_run :focus => true
  config.run_all_when_everything_filtered = true

  config.before :all do
    @config = YAML.load File.open('config/config.yml')

    @tropo2 = Tropo2Utilities::Tropo2Driver.new :username         => ENV['TROPO2_JID'] || @config['tropo2_server']['jid'],
                                                :password         => ENV['TROPO2_PASSWORD'] || @config['tropo2_server']['password'],
                                                :wire_logger      => Logger.new(@config['tropo2_server']['wire_log']),
                                                :transport_logger => Logger.new(@config['tropo2_server']['transport_log']),
                                                :log_level        => Logger::DEBUG,
                                                :queue_timeout    => @config['tropo2_queue']['connection_timeout']

    @config['tropo2_server']['sip_uri'] = ENV['TROPO2_SIP_URI'] if ENV['TROPO2_SIP_URI']

    drb_server_host_and_port = [@config['tropo1']['druby_host'], ENV['TROPO1_DRB_PORT'] || @config['tropo1']['druby_port']].join ':'

    @config['tropo1']['session_url'] += "&vars=drb_server_address%3D#{drb_server_host_and_port}"

    @drb_server_uri = "druby://#{drb_server_host_and_port}"

    @tropo1 = Tropo2Utilities::Tropo1Driver.new @drb_server_uri, @config['tropo1']['latch_timeout']

    status = @tropo2.read_queue(@tropo2.event_queue)
    abort 'Could not connect to Prism XMPP Server. Aborting!' if status != 'CONNECTED'
    @tropo2.start_event_dispatcher
  end

  config.before :each do
    @tropo1.reset!
  end

  config.after :each do
    @tropo2.calls = {}
    @tropo2.read_event_queue(@config['tropo2_queue']['last_stanza_timeout']) until @tropo2.event_queue.empty?
  end

  config.after :all do
    begin
      @tropo1.drb.stop_service
    rescue
      puts 'Upstream error, DRb not running from previous test.'
    end
  end
end
