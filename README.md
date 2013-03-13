Rayo Server Functional Tester
==============================

Provides an automated functional test framework for testing a Rayo server. The Rayo server is instructed to call itself. The purpose is to run assertions not only on the Rayo stanzas, message sequences and behavior, but on the media as well.

Requirements
------------

* Prism 11 + rayo-server or FreeSWITCH with mod_rayo
* Maven

Usage
-----

1. Wipe out your Maven repository folder
```
rm -rf ~/.m2/repository
```

2. Clone and build Moho's trunk:
```
git clone git@github.com:voxeolabs/moho.git
cd moho
mvn clean install -Dmaven.test.skip=true
```

3. Clone and build Rayo's trunk:
```
git clone git@github.com:rayo/rayo-server.git
cd rayo-server
mvn clean install -Dmaven.test.skip=true
```

4. Clone Rayo Functional Tests:
```
git clone git@github.com:rayo/rayo-functional-tests.git
```

5. Start the rayo server

6. Run all tests, an individual test class or an individual test method
```
cd rayo-functional-tests
mvn clean test
mvn clean test -Dtest=OutputTest
mvn clean test -Dtest=OutputTest#testOutputCompleteReceived
```
