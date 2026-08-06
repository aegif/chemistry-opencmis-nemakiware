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

Phase 3 modernization notes (1.1.2-nemakiware)
==============================================

Compared with phase 2 (`1.1.1-nemakiware`):

 - Artifact version: `1.1.2-nemakiware`
 - Apache HttpClient: `org.apache.httpcomponents.client5:httpclient5:5.5.1`
   (replaces HttpClient 4.5.14 / `org.apache.httpcomponents:httpclient`)
 - Session parameter class name `ApacheClientHttpInvoker` is unchanged
 - Default HTTP invoker remains JDK `DefaultHttpInvoker`

NemakiWare consumption remains out of scope for phase 3.

Phase 4 modernization notes (1.1.3-nemakiware)
==============================================

Compared with phase 3 (`1.1.2-nemakiware`):

 - Artifact version: `1.1.3-nemakiware`
 - GitHub Actions: `actions/checkout@v5`, `actions/setup-java@v5`
 - Maven plugins: assembly 3.7.1, antrun 3.1.0, resources 3.3.1,
   source 3.3.1, remote-resources 3.2.0, buildnumber 3.2.1,
   exec 3.5.0, release 3.1.1, surefire/failsafe 3.5.4
 - OkHttp: `okhttp-jvm:5.4.0` (from `okhttp:4.12.0`; OkHttp 5
   multiplatform JVM artifact)
 - Test stack: JUnit Jupiter / Vintage `5.14.4` (existing JUnit 4 tests
   keep running via Vintage; new invoker smoke tests use Jupiter)
 - Apache HttpClient 5 invoker closes `CloseableHttpResponse` with the
   response stream

NemakiWare consumption remains out of scope for phase 4.

Phase 5 modernization notes (1.1.4-nemakiware)
==============================================

Compared with phase 4 (`1.1.3-nemakiware`):

 - Artifact version: `1.1.4-nemakiware`
 - Unit/integration test sources migrated from JUnit 4 / TestCase to
   JUnit Jupiter 5.14.4 (`@Test`, `@BeforeEach` / `@AfterEach`,
   `Assertions` / `Assumptions`, `assertThrows`)
 - `junit-vintage-engine` removed from the parent test classpath
 - `junit:junit` remained briefly for TCK main sources (removed in
   phase 6)

NemakiWare consumption remains out of scope for phase 5.

Phase 6 modernization notes (1.1.6-nemakiware)
==============================================

Compared with phase 5 (`1.1.4-nemakiware`):

 - Artifact version: `1.1.6-nemakiware` (6a was `1.1.5-nemakiware`)
 - TCK main (`AbstractCmisTest`, `AbstractCmisTestGroup`,
   `JUnitHelper`) switched from JUnit 4 to JUnit Jupiter API
 - Parent test-scoped `junit:junit` removed; TCK depends on
   `junit-jupiter-api` instead
 - FIT embedded Tomcat upgraded from 7.0.75 to 10.1.39 (Jakarta)

NemakiWare consumption remains out of scope for phase 6.

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
