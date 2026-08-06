=========================
Apache Chemistry OpenCMIS
=========================

OpenCMIS is a collection of Java libraries, frameworks and tools around
the CMIS (Content Management Interoperability Services) specification.

OpenCMIS supports the specification versions
 - CMIS 1.0 <http://docs.oasis-open.org/cmis/CMIS/v1.0/cmis-spec-v1.0.html>
 - CMIS 1.1 <http://docs.oasis-open.org/cmis/CMIS/v1.1/CMIS-v1.1.html>


Building OpenCMIS
=================

You can build OpenCMIS like this:

    mvn clean install

You need Maven 3 with Java 21 (or higher) for the build.

This fork uses Java 21 as the baseline (`--release 21`) and validates
build compatibility with both Java 21 and Java 25 in GitHub Actions.

Phase 2 modernization notes (1.1.1-nemakiware)
==============================================

Compared with phase 1 (`1.1.0-nemakiware` / Java 17 baseline):

 - Artifact version: `1.1.1-nemakiware` (distinguishable from published
   `1.1.0-nemakiware` packages)
 - Compile / release target: Java 21
 - CI matrix: Java 21 and Java 25
 - Woodstox: `com.fasterxml.woodstox:woodstox-core:7.1.1`
   (replaces `org.codehaus.woodstox:woodstox-core-asl:4.4.1`)
 - Apache HttpClient: `4.5.14` (from `4.2.6`; HttpClient 5.x deferred)
 - OkHttp: `4.12.0` (from `3.4.1`)
 - SLF4J: `2.0.17`

NemakiWare consumption of this artifact is intentionally out of scope
for phase 2; publish via `./scripts/deploy-required-jars.sh` when ready.

Client-Bindings Integration Tests
=================================

The `SimpleReadOnlyTests` and `SimpleReadWriteTests` in `client-bindings`
require a running CMIS server and are excluded from `mvn test` by default.

To run them against a NemakiWare server:

    mvn -f chemistry-opencmis-client/chemistry-opencmis-client-bindings/pom.xml \
      verify \
      -Dopencmis.integration.tests.skip=false \
      -Dopencmis.test.username=admin \
      -Dopencmis.test.password=admin \
      -Dopencmis.test.repository=bedroom \
      -Dopencmis.test.atompub.url=http://localhost:8080/core/atom \
      -Dopencmis.test.webservices.url=http://localhost:8080/core/services/


GitHub Packages (NemakiWare fork)
=================================

This fork can publish the jars required by NemakiWare to GitHub Packages
without building WAR modules that fail on modern JDK module constraints.

Required module deploy command:

    ./scripts/deploy-required-jars.sh

The script enables the `github-packages` Maven profile and deploys only:
 - chemistry-opencmis-commons-api
 - chemistry-opencmis-commons-impl
 - chemistry-opencmis-client-api
 - chemistry-opencmis-client-bindings
 - chemistry-opencmis-client-impl
 - chemistry-opencmis-server-support
 - chemistry-opencmis-server-bindings
 - chemistry-opencmis-test-tck

Maven credentials are read from `~/.m2/settings.xml` server id `github`.
Example:

    <settings>
      <servers>
        <server>
          <id>github</id>
          <username>YOUR_GITHUB_USERNAME</username>
          <password>YOUR_GITHUB_TOKEN</password>
        </server>
      </servers>
    </settings>

GitHub Actions workflow:
 - `.github/workflows/publish-required-jars.yml`
 - runs on `workflow_dispatch` or pushed `v*` tags
 - uses `GITHUB_TOKEN` to deploy packages


License (see also package specific LICENSE files)
=================================================

Collective work: Copyright 2010-2017 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Dependencies with "Weak Copyleft" or dual licenses
==================================================

OpenCMIS uses some libraries with open source licenses that require reciprocal
licensing when modified. These libraries are included in unmodified binary
form and can be redistributed under terms that are compatible with the
Apache License.

Some libraries used by OpenCMIS are dual-licensed under different open source
licenses. These libraries are redistributed under the license whose terms
are compatible with the Apache License.

See LICENSE file included in all Apache Chemistry OpenCMIS packages for 
full licensing details.
