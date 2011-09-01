Rayo Server Functional Tester
==============================

Provides an automated functional test framework for testing Rayo Server. Tropo1 running on Prism (or the Tropo.com cloud) pitches calls at the Tropo2 instance, as well as receives calls from the Tropo2 instance. The purpose is to run assertions not only on the Ozone stanzas, message sequences and behavior, but on the media as well.

Requirements
------------

* Prism 10.1
* Tropo1
* Rayo Server
* Ruby 1.8.7+ or JRuby 1.5.3+
* Rubygems 1.8.1+
* Bundler (http://gembundler.com/)

Installation
------------

	gem sources -a http://geminabox.voxeolabs.com/
	git clone git@github.com:rayo/rayo_functional_tests.git
	cd rayo_functional_tests
	bundle install

Configuration
-------------

* /opt/voxeo/prism/conf/portmappings.properties (both Tropo versions may run in the same Prism instance):

    * 5060:rayo
    * 5061:tropo

* /opt/voxeo/prism/conf/sipmethod.xml (ensure both 5060 and 5061 are configured to listen)
* /opt/voxeo/prism/apps/tropo/WEB-INF/classes/tropo.xml (in the `<mediaServer/>` section you should have `<bangSyntax>false</bangSyntax>`)
* rayo_functional_tests/config/config.yml.sample (rename to config.yml with changes for your environment)
* Deploy Tropo1 script to $PRISM_HOME/apps/tropo/scripts (rayo_functional_tests/support/tropo1_script/rayo_testing.rb)

Running
-------

	cd rayo_functional_tests
	bundle exec rspec spec/*_spec.rb --format doc (for verbose output)
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

	RayoAutomatedFunctionalTesting
	  Call accept, answer and hangup handling
	    Should receive a call arrives and then hangup
	    Should answer and hangup
	    Should throw an error if we try to answer a call that is hungup
	    Should accept and hangup
	    Should answer a call and let the farside hangup
	RayoAutomatedFunctionalTesting
	  Ask command
	    Should ask something with ASR and get the utterance back
	    Should ask with an SSML as a prompt
	    Should ask with a GRXML grammar
	    Should ask with an SSML prompt and a GRXML grammar
	    Should ask and get a NOINPUT event
	    Should ask and get a NOMATCH event with min_confidence set to 1 (PENDING: https://github.com/rayo/rayo-server/issues/30)
	    Should ask and get a STOP if the farside hangs up before the command complete (PENDING: https://github.com/rayo/rayo-server/issues/32)
	RayoAutomatedFunctionalTesting
	  Conference command
	    Should put one caller in conference and then hangup
	    Should put two callers into a conference and then hangup (PENDING: The DSL needs to handle two calls at a time now, for the first time. Will add.)
	    Should put two callers into a conference, validate media and hangup (PENDING: The DSL needs to handle two calls at a time now, for the first time. Will add.)
	RayoAutomatedFunctionalTesting
	  Dial command
	    Should place an outbound call, receive a ring event, receive an answer event and then hangup
	    Should place an outbound call and then receive a reject event
	RayoAutomatedFunctionalTesting
	  Redirect command
	    Should redirect a call (PENDING: https://github.com/tropo/punchblock/issues/22)
	RayoAutomatedFunctionalTesting
	  Reject command
	    Should reject with a declined reason
	    Should reject with a busy reason
	    Should reject with a error reason
	RayoAutomatedFunctionalTesting
	  Say command
	    Should say something with TTS
	    Should say an audio URL
	    Should say SSML
	    Should say some audio, wait 2 seconds, pause, wait 2 seconds, resume, wait 2 seconds and then stop (PENDING: https://github.com/tropo/punchblock/issues/10)
	    Should say an audio URL and get a stop event
	RayoAutomatedFunctionalTesting
	  Transfer verb
	    Should answer a call and then transfer it
	    Should try to transfer but get a timeout
	Finished in 199.55 seconds
	28 examples, 0 failures, 6 pending

Project Files
-------------

* spec/rayo-functional_spec.rb - Provides the RSpec tests to run
* support/tropo1_script/rayo_testing.rb - Tropo1 Ruby Script
