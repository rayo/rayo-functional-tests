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

    * 5060:tropo2
    * 5061:tropo

* voxeo/prism/conf/sipmethod.xml (ensure both 5060 and 5061 are configured to listen)
* tropo2_functional_tester/config/config.yml.sample (rename to config.yml with changes for your environment)

* Deploy script to $PRISM_HOME/apps/tropo/scripts (tropo2_functional_tester/support/tropo1_script/tropo2_testing.rb)
* tropo2_functional_tester/tropo1_script/tropo2_testing.rb (change this line as needed: drb_server_address = $drb_server_address || '98.207.5.162:8787')

Running
-------

	cd tropo2_functional_tester
	rspec spec/*_spec.rb --format doc (for verbose output)
	rake format=junit
	rake hudson (for basic output that supports Hudson CI server)
	rake ask
	rake conference
	rake dial
	rake misc
	rake say

Screencast
----------

* [Screencast @ Blip.tv](http://blip.tv/file/5114210)
* Password: thisisozone!

Example
-------

	Tropo2AutomatedFunctionalTesting
	  Call accept, answer and hangup handling
	    Should receive a call arrives and then hangup
	    Should answer and hangup
	    Should throw an error if we try to answer a call that is hungup
	    Should accept and hangup
	    Should answer a call and let the farside hangup
	Tropo2AutomatedFunctionalTesting
	  Ask command
	    Should ask something with ASR and get the utterance back
	    Should ask with an SSML as a prompt
	    Should ask with a GRXML grammar
	    Should ask with an SSML prompt and a GRXML grammar
	    Should ask and get a NOINPUT event
	    Should ask and get a NOMATCH event with min_confidence set to 1 (PENDING: https://github.com/tropo/tropo2/issues/30)
	    Should ask and get a STOP if the farside hangs up before the command complete (PENDING: https://github.com/tropo/tropo2/issues/32)
	Tropo2AutomatedFunctionalTesting
	  Conference command
	    Should put one caller in conference and then hangup
	    Should put two callers into a conference and then hangup (PENDING: The DSL needs to handle two calls at a time now, for the first time. Will add.)
	    Should put two callers into a conference, validate media and hangup (PENDING: The DSL needs to handle two calls at a time now, for the first time. Will add.)
	Tropo2AutomatedFunctionalTesting
	  Dial command
	    Should place an outbound call, receive a ring event, receive an answer event and then hangup
	    Should place an outbound call and then receive a reject event
	Tropo2AutomatedFunctionalTesting
	  Redirect command
	    Should redirect a call (PENDING: https://github.com/tropo/punchblock/issues/22)
	Tropo2AutomatedFunctionalTesting
	  Reject command
	    Should reject with a declined reason
	    Should reject with a busy reason
	    Should reject with a error reason
	Tropo2AutomatedFunctionalTesting
	  Say command
	    Should say something with TTS
	    Should say an audio URL
	    Should say SSML
	    Should say some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop (PENDING: https://github.com/tropo/punchblock/issues/10)
	    Should say an audio URL and get a stop event
	Tropo2AutomatedFunctionalTesting
	  Transfer verb
	    Should answer a call and then transfer it
	    Should try to transfer but get a timeout
	Finished in 199.55 seconds
	28 examples, 0 failures, 6 pending

Project Files
-------------

* spec/tropo2-functional_spec.rb - Provides the RSpec tests to run
* support/tropo1_script/tropo2_testing.rb - Tropo1 Ruby Script
