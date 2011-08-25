%w{
  timeout
  rspec
  yaml
  thread
  awesome_print
  logger
  punchblock
  rspec-rayo
}.each { |lib| require lib }

Thread.abort_on_exception = true

Dir[File.dirname(__FILE__) + "/support/**/*.rb"].each {|f| require f}

$config = YAML.load File.open('config/config.yml')

def random_jid
  "user#{(1..1000).to_a.sort_by { rand }.first}@127.0.0.1"
end

$tropo2 = RSpecRayo::RayoDriver.new :username         => ENV['TROPO2_JID'] || $config['tropo2_server']['jid'] || random_jid,
                                    :password         => ENV['TROPO2_PASSWORD'] || $config['tropo2_server']['password'],
                                    :wire_logger      => Logger.new($config['tropo2_server']['wire_log']),
                                    :transport_logger => Logger.new($config['tropo2_server']['transport_log']),
                                    :log_level        => Logger::DEBUG,
                                    :queue_timeout    => $config['tropo2_queue']['connection_timeout'],
                                    :write_timeout    => $config['tropo2_server']['write_timeout']

$config['max_calls_per_test'] ||= 5
$config['media_server_port_limit'] ||= 10
$config['call_pruning_timeout'] ||= 15

$config['tropo2_server']['sip_uri'] ||= ENV['TROPO2_SIP_URI'] || random_jid

drb_server_host_and_port = [$config['tropo1']['druby_host'], ENV['TROPO1_DRB_PORT'] || $config['tropo1']['druby_port']].join ':'

$config['tropo1']['session_url'] += "&vars=drb_server_address%3D#{drb_server_host_and_port}"

$drb_server_uri = "druby://#{drb_server_host_and_port}"

$tropo1 = RSpecRayo::Tropo1Driver.new $drb_server_uri, $config['tropo1']['latch_timeout']
$tropo1.config = $config

module Punchblock
  module Component
    class ComponentNode
      def next_event(timeout = nil)
        Timeout::timeout(timeout || $config['tropo2_queue']['connection_timeout']) { event_queue.pop }
      end
    end
  end
end

status = $tropo2.read_queue $tropo2.event_queue
abort 'Could not connect to Prism XMPP Server. Aborting!' unless status == 'CONNECTED'
$tropo2.start_event_dispatcher

RSpec.configure do |config|
  config.filter_run :focus => true
  config.run_all_when_everything_filtered = true

  config.before :all do
    @config, @tropo1, @tropo2, @drb_server_uri = $config, $tropo1, $tropo2, $drb_server_uri
    @tropo1.start_drb
  end

  config.after :each do
    begin
      session_limit = @config['media_server_port_limit'] - @config['max_calls_per_test']
      checks = 0
      begin
        sleep 1 if checks > 0
        checks += 1
      end while active_sessions > session_limit && checks < @config['call_pruning_timeout']
      active_sessions.should <= session_limit
      @tropo1.reset!
      @call.last_event?(@config['tropo2_queue']['last_stanza_timeout']).should == true if @call
      check_no_remaining_calls unless ENV['TROPO2_CONCURRENT_TESTS']
      @tropo2.read_event_queue(@config['tropo2_queue']['last_stanza_timeout']) until @tropo2.event_queue.empty?
    ensure
      @tropo2.cleanup_calls
    end
  end

  config.after :all do
    begin
      @tropo1.stop_drb
    rescue
      puts 'Upstream error, DRb not running from previous test.'
    end
  end
end
