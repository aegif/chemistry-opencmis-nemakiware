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
 - Apache HttpClient: `4.5.14` (from `4.2.6`; migrated to HttpClient
   5.x in phase 3)
 - OkHttp: `4.12.0` (from `3.4.1`; later OkHttp 5 in phase 4)
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

Phase 6 modernization notes (2.0.0-nemakiware)
==============================================

Compared with phase 5 (`1.1.4-nemakiware`):

 - Artifact version: `2.0.0-nemakiware` — major bump for ANTLR4 / public
   API binary breaks vs `1.1.x-nemakiware` (not a drop-in jar replace)
 - TCK main (`AbstractCmisTest`, `AbstractCmisTestGroup`,
   `JUnitHelper`) switched from JUnit 4 to JUnit Jupiter API
 - Parent test-scoped `junit:junit` removed; TCK depends on
   `junit-jupiter-api` instead
 - FIT embedded Tomcat upgraded to 11.0.24 (Jakarta EE 11 / Servlet 6.1),
   matching NemakiWare (`tomcat:11.0-jdk21`); compile-time
   `jakarta.servlet-api` is 6.1.0
 - CI (`verify-java21-25.yml`): unit tests, `./scripts/antlr4-compat-check.sh`,
   Workbench package, OSGi bundle check, Android JVM + D8 + API 26
   emulator smoke, required FIT AtomPub/Browser/WebServices 1.1 NonVers
 - Default HTTP invoker: Apache HttpClient 5 (`ApacheClientHttpInvoker`);
   OkHttp and JDK `DefaultHttpInvoker` remain explicit alternatives
 - Toolchain: Mockito Java agent for Surefire/Failsafe, build-helper
   3.6.0, checkstyle 3.6.0, rat 0.16.1, javadoc 3.11.2;
   supplemental-models updated for Jakarta artifacts
 - server-support CMIS query grammars migrated from ANTLR3 to ANTLR4
   4.13.2 (compatibility AST + hand-written CmisQueryWalker)
 - Query compatibility gates: golden AST corpora under
   `query-compat/*.corpus`, `QueryAstCorpusTest`,
   `QuerySemanticSnapshotTest`, and `scripts/antlr4-compat-check.sh`
   (fails if TestParserStrict cases are missing from the corpus, or
   if corpus/overlap floors shrink)
 - OSGi Core `org.osgi.core` 6.0.0; Felix `maven-bundle-plugin` 6.0.2;
   OSGi client embeds HttpClient 5 (default invoker) and OkHttp
 - Android StAX sharing via Woodstox (XmlPull / kxml2 forks removed);
   explicit `stax-api` + `stax2-api` for non-JDK runtimes
 - Maintenance (still `2.0.0-nemakiware`; no micro-bump within the line):
   - Jacoco `0.8.15`, Mockito `5.23.0`, Log4j `2.25.5`
   - Removed one-shot helper scripts
     (`convert-servlet-imports.sh`, `update-servlet-dependencies.sh`,
     `scripts/migrate-junit4-to-jupiter.py`)
 - HttpClient 5 / OkHttp session clients are closed when the binding
   SPI closes (`HttpInvokerSessionResources`)
 - Workbench: Groovy 4.0.26 modules (`groovy`, `groovy-console`,
   `groovy-swing`, `groovy-templates` — not `groovy-all`), Java
   Taskbar icon API, CMIS-1044/1048 UI fixes, Repository Info reload
   on a background worker

Breaking / migration notes (2.0.0-nemakiware)
---------------------------------------------

This line is intentionally a **major** version relative to
`1.1.x-nemakiware`. Existing 1.1.x binaries that implement ANTLR3
`Tree`-based walkers will fail with `NoSuchMethodError` until recompiled
against `CmisTree`. Coordinate a full rebuild of custom extensions.

ANTLR3 → ANTLR4 (source + binary break for custom query walkers):

 - Replace `org.antlr.runtime.tree.Tree` / `CommonTree` with
   `org.apache.chemistry.opencmis.server.support.query.CmisTree` /
   `CmisCommonTree` in `PredicateWalker` / `PredicateWalkerBase`
   implementations and any code calling `QueryUtilBase.parseStatement()`.
 - Do not depend on the ANTLR3 runtime. ANTLR4 parse trees are adapted
   through `CmisQlAstBuilder` / `CmisQlExtAstBuilder` into the
   compatibility AST; walk with `CmisQueryWalker`.
 - Recompile all custom walkers against this release; old binaries
   against ANTLR3 Tree signatures will fail with `NoSuchMethodError`.

Properties (behavioral change, CMIS-1041):

 - `PropertiesImpl.addProperty` throws `IllegalArgumentException` on
   duplicate property IDs (previously overwrote the map entry while
   leaving a stale list entry). Use `replaceProperty` to overwrite.

HTTP invokers:

 - Default is Apache HttpClient 5 `ApacheClientHttpInvoker`
   (`org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker`);
   `httpclient5` is a compile dependency of client-bindings
 - High-load defaults (overridable via session parameters; apply when using
   `ApacheClientHttpInvoker` for AtomPub / Browser — not CXF Web Services):
   - max connections / route: 100 (`...binding.http.maxconnectionsperhost`)
   - max connections total: 200 (`...binding.http.maxconnections`)
   - connection-lease / materialization-permit wait: 60s
     (`...binding.http.connectionrequesttimeout`)
   - response bodies up to 1 MiB are buffered so pooled connections are
     released immediately (`...binding.http.responsebufferlimit`); larger
     bodies stream and must be closed by the caller
   - request bodies are materialized under classloader-wide limits before the
     HTTP connection is obtained (heap up to 1 MiB then temp file via
     `...binding.http.requestmemorylimit`). The concurrency permit is held
     only during buffering; after materialization it is released so other
     uploads can spool while this request is on the wire. Byte budget stays
     charged until the body is discarded. Limits (first materialization in
     the classloader locks concurrent/total settings — later sessions with
     different values are rejected; not a multi-webapp host limit unless
     OpenCMIS is on a shared classloader):
     - temp dir: `...binding.http.tempdir` (JVM default temp dir)
     - max per request body (heap and/or disk): 5 GiB
       (`...binding.http.requestspoolmaxsize`)
     - max concurrent materializations: 32
       (`...binding.http.requestspoolmaxconcurrent`)
     - max total active body bytes (heap + disk): 20 GiB
       (`...binding.http.requestspoolmaxtotalbytes`); when exhausted, new
       bodies wait up to the connection-request timeout for budget instead
       of failing immediately (only a body that alone exceeds the budget
       fails fast)
 - Explicit alternatives (session parameter
   `org.apache.chemistry.opencmis.binding.httpinvoker.class`):
   - `...spi.http.OkHttpHttpInvoker` (OkHttp 5 / okhttp-jvm; recommended on Android)
   - `...spi.http.DefaultHttpInvoker` (JDK HttpURLConnection)
 - Closing a session/binding releases cached Apache / OkHttp clients.

Out of scope for this fork line (do not track as OpenCMIS work):

 - Apache Chemistry `trunk` / 1.2.0-SNAPSHOT re-merge
 - NemakiWare repository Packages / `lib/built-jars` intake
   (OpenCMIS remains a NemakiWare building block; consumption is a
   separate NemakiWare change)

Android client (phase 6+):

 - Removed `com.google.android:android` stub
 - `--release 11` for the android module; CI runs
   `scripts/android-d8-smoke.sh` (D8, min-api 26) and an API 26
   emulator smoke (`scripts/android-emulator-smoke.sh`) that pushes
   the DEX to a running emulator
 - StAX sharing: desktop `XMLConverter` / `XMLUtils` / AtomPub SPI are
   copied into the android fat client; Woodstox replaces former
   kxml2/XmlPull forks so AtomPub stays in sync with the desktop client
 - Runtime deps for Android: `woodstox-core`, `stax-api`, `stax2-api`
   (also shipped under `lib/` in the `pack` ZIP/TAR; CDDL for stax-api
   disclosed in the pack LICENSE/NOTICE)
 - CI runs `AndroidStaxSmokeTest` on the JVM in addition to `package`

CI release gates (verify-java21-25.yml):

 - Unit tests, ANTLR compat script, Workbench package, OSGi bundle
   package + MANIFEST check, Android JVM + D8 + emulator smoke
 - Required FIT (Java 21): AtomPub / Browser / Web Services
   CMIS 1.1 NonVers (`AtomPub11NonVersTckIT`, `Browser11NonVersTckIT`,
   `WebServices11NonVersTckIT`)

Client-Bindings Integration Tests
=================================

The `SimpleReadOnlyTests` and `SimpleReadWriteTests` in `client-bindings`
require a running CMIS server and are excluded from `mvn test` by default.
PR CI covers AtomPub, Browser, and Web Services end-to-end via FIT
(CMIS 1.1 NonVers).

To run the legacy Simple* suites against a NemakiWare server:

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
