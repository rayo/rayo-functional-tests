Tropo2 Functional Tester
========================

Provides an automated functional test framework for testing Tropo2. Tropo1 running on Prism (or the Tropo.com cloud) pitches calls at the Tropo2 instance, as well as receives calls from the Tropo2 instance. The purpose is to run assertions not only on the Ozone stanzas, message sequences and behavior, but on the media as well.

Requirements
------------

* Prism 10.1
* Tropo1
* Tropo2/Ozone
* Ruby 1.8.7+ or JRuby 1.5.3+
* Rubygems 1.8.1+
* Bundler (http://gembundler.com/)

Installation
------------

	gem sources -a http://geminabox.voxeolabs.com/
	git clone git@github.com:tropo/tropo2_functional_tester.git
	cd tropo2_functional_tester
	bundle install

Configuration
-------------

* voxeo/prism/conf/portmappings.properties (both Tropo versions may run in the same Prism instance):

	5060:tropo2
	5061:tropo

* voxeo/prism/conf/sipmethod.xml (ensure both 5060 and 5061 are configured to listen)
* tropo2_functional_tester/config/config.yml.sample (rename to config.yml with changes for your environment)
* tropo2_functional_tester/tropo1_script/tropo2_testing.rb
* Deploy script to ~/voxeo/prism/apps/tropo/scripts (tropo2_functional_tester/tropo1_script/tropo2_testing.rb)

Running
-------

	cd tropo2_functional_tester
	rspec spec/tropo2-functional_spec.rb --format doc

Screencast
----------

* [Screencast @ Blip.tv](http://blip.tv/file/5114210)
* Password: thisisozone!

Example
-------

	rspec spec/tropo2-functional_spec.rb --format doc
	Tropo2AutomatedFunctionalTesting
	"Connecting to Tropo2 server with: usera@10.0.1.11 1"
	"Connected to Tropo2 Server"
	"Starting Distributed Ruby Service to host Tropo scripts."
	  connection handling
	    EventMachine Should be connected to the Tropo2 XMPP service
	  Answer and hangup handling
	    Should receive an offer when a call arrives
	    Should accept and answer a call
	    Should answer and then hangup a call
	  Say verb
	    Should say something with TTS
	  Ask verb
	    Should ask something with ASR and get the utterance back
	Finished in 31.15 seconds
	7 examples, 0 failures, 0 pending

Project Files
-------------

* spec/tropo2-functional_spec.rb - Provides the RSpec tests to run
* support/tropo1_script/tropo2_testing.rb - Tropo1 Ruby Script
