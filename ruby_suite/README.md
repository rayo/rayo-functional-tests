Rayo Server Functional Tester
==============================

Provides an automated functional test framework for testing a Rayo Server. Tropo running on Prism (or the Tropo.com cloud) pitches calls at the Rayo server, as well as receiving calls from it. The purpose is to run assertions not only on the Rayo stanzas, message sequences and behavior, but on the media as well.

Requirements
------------

* Prism 11
* Tropo
* Rayo Server
* Ruby 1.8.7+ or JRuby 1.5.3+
* Rubygems 1.8.1+
* Bundler (http://gembundler.com/)

Installation
------------

	git clone git@github.com:rayo/rayo_functional_tests.git
	cd rayo_functional_tests
	bundle install

Configuration
-------------

* /opt/voxeo/prism/conf/portmappings.properties (both Tropo and rayo-server may run in the same Prism instance):

    * 5060:rayo
    * 5061:tropo

* /opt/voxeo/prism/conf/sipmethod.xml (ensure both 5060 and 5061 are configured to listen)
* /opt/voxeo/prism/apps/tropo/WEB-INF/classes/tropo.xml (in the `<mediaServer/>` section you should have `<bangSyntax>false</bangSyntax>`)
* rayo_functional_tests/config/config.yml.sample (rename to config.yml with changes for your environment)
* Deploy Tropo script to $PRISM_HOME/apps/tropo/scripts (rayo_functional_tests/support/tropo_script/rayo_testing.rb)

Running
-------

	cd rayo_functional_tests
	rake spec
	rake hudson (for basic output that supports Hudson CI server)
	rake ask
	rake conference
	rake dial
	rake misc
	rake say
	...etc...

Screencast
----------

* [Screencast @ Blip.tv](http://blip.tv/file/5114210)
* Password: thisisozone!
