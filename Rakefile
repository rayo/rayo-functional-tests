require 'rubygems'
require 'bundler'

begin
  Bundler.setup(:default, :development)
rescue Bundler::BundlerError => e
  $stderr.puts e.message
  $stderr.puts "Run `bundle install` to install missing gems"
  exit e.status_code
end

require 'rspec/core'
require 'rspec/core/rake_task'
require 'ci/reporter/rake/rspec'

task :hudson => ["ci:setup:rspec", :spec]

RSpec::Core::RakeTask.new(:spec) do |spec|
  spec.pattern = FileList['spec/**/*_spec.rb']
end

%w{answer-hangup ask cdr conference dial dtmf input jmx join output record redirect reject say scenarios transfer}.each do |command|
  RSpec::Core::RakeTask.new(command.to_sym) do |spec|
    spec.pattern = FileList["spec/**/tropo2-#{command}_spec.rb"]
  end
end

task :default => :spec

