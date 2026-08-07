/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.commons;

/**
 * Session parameter constants.
 * 
 * <table border="2" rules="all" cellpadding="4" summary="Session Parameters">
 * <tr>
 * <th>Constant</th>
 * <th>Description</th>
 * <th>Binding</th>
 * <th>Value</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * <tr>
 * <td colspan="6"><b>General settings</b></td>
 * </tr>
 * <tr>
 * <td>{@link #BINDING_TYPE}</td>
 * <td>Defines the binding to use for the session</td>
 * <td>all</td>
 * <td>"atompub", "webservices", "browser", "local", "custom"</td>
 * <td>yes</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #BINDING_SPI_CLASS}</td>
 * <td>Binding implementation class</td>
 * <td>all</td>
 * <td>class name</td>
 * <td>Custom binding: yes<br>
 * all other binding: no</td>
 * <td>depends on {@link #BINDING_TYPE}</td>
 * </tr>
 * <tr>
 * <td>{@link #REPOSITORY_ID}</td>
 * <td>Repository ID</td>
 * <td>all</td>
 * <td>repository id</td>
 * <td>SessionFactory.createSession(): yes<br>
 * SessionFactory.getRepositories(): no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #LOCALE_ISO639_LANGUAGE}</td>
 * <td>Language code sent to server</td>
 * <td>all</td>
 * <td>ISO 639 code</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #LOCALE_ISO3166_COUNTRY}</td>
 * <td>Country code sent to server if language code is set</td>
 * <td>all</td>
 * <td>ISO 3166 code</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <tr>
 * <td>{@link #OBJECT_FACTORY_CLASS}</td>
 * <td>Object factory implementation class</td>
 * <td>all</td>
 * <td>class name</td>
 * <td>no</td>
 * <td>org.apache.chemistry.opencmis.client.runtime.repository.ObjectFactoryImpl
 * </td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>Authentication settings</b></td>
 * </tr>
 * <tr>
 * <td>{@link #USER}</td>
 * <td>User name (used by the standard authentication provider)</td>
 * <td>all</td>
 * <td>user name</td>
 * <td>depends on the server</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #PASSWORD}</td>
 * <td>Password (used by the standard authentication provider)</td>
 * <td>all</td>
 * <td>password</td>
 * <td>depends on the server</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #AUTHENTICATION_PROVIDER_CLASS}</td>
 * <td>Authentication Provider class</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>class name</td>
 * <td>no</td>
 * <td>org.apache.chemistry.opencmis.client.bindings.spi.
 * StandardAuthenticationProvider</td>
 * </tr>
 * <tr>
 * <td>{@link #AUTH_HTTP_BASIC}</td>
 * <td>Switch to turn HTTP basic authentication on or off</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>depends on {@link #BINDING_TYPE}</td>
 * </tr>
 * <tr>
 * <td>{@link #AUTH_HTTP_BASIC_CHARSET}</td>
 * <td>Charset to encode HTTP basic authentication username and password</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>character set name</td>
 * <td>no</td>
 * <td>UTF-8</td>
 * </tr>
 * <tr>
 * <td>{@link #AUTH_SOAP_USERNAMETOKEN}</td>
 * <td>Switch to turn UsernameTokens on or off</td>
 * <td>Web Services</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>HTTP and network settings</b></td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_INVOKER_CLASS}</td>
 * <td>HTTP invoker class</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>class name</td>
 * <td>no</td>
 * <td>org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker
 * </td>
 * </tr>
 * <tr>
 * <td>{@link #COMPRESSION}</td>
 * <td>Switch to turn HTTP response compression on or off</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>false</td>
 * </tr>
 * <tr>
 * <td>{@link #CLIENT_COMPRESSION}</td>
 * <td>Switch to turn HTTP request compression on or off</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>false</td>
 * </tr>
 * <tr>
 * <td>{@link #COOKIES}</td>
 * <td>Switch to turn cookie support on or off</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>false</td>
 * </tr>
 * <tr>
 * <td>{@link #HEADER}</td>
 * <td>HTTP header</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>header header</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #USER_AGENT}</td>
 * <td>User agent header</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>user agent string</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #PROXY_USER}</td>
 * <td>Proxy user (used by the standard authentication provider)</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>user name</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #PROXY_PASSWORD}</td>
 * <td>Proxy password (used by the standard authentication provider)</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>password</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #CSRF_HEADER}</td>
 * <td>CSRF Header</td>
 * <td>AtomPub, Web Services, Browser</td>
 * <td>header name</td>
 * <td>no</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #CONNECT_TIMEOUT}</td>
 * <td>HTTP connect timeout (also used as connection-lease wait for Apache
 * HttpClient 5 when {@link #HTTP_CONNECTION_REQUEST_TIMEOUT} is unset)</td>
 * <td>AtomPub, Browser, Web Services (CXF / WebSphere)</td>
 * <td>time in milliseconds</td>
 * <td>no</td>
 * <td>JVM default (Apache invoker defaults connection-lease wait to 60s)</td>
 * </tr>
 * <tr>
 * <td>{@link #READ_TIMEOUT}</td>
 * <td>HTTP read timeout</td>
 * <td>AtomPub, Browser, Web Services (CXF / WebSphere); also JDK/OkHttp
 * invokers</td>
 * <td>time in milliseconds</td>
 * <td>no</td>
 * <td>JVM default</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_CONNECTION_REQUEST_TIMEOUT}</td>
 * <td>Max wait for a free pooled HTTP connection <b>and</b> for a free
 * classloader-wide request-body materialization permit (Apache HttpClient
 * 5)</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>time in milliseconds</td>
 * <td>no</td>
 * <td>60000</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_MAX_CONNECTIONS}</td>
 * <td>Max total pooled HTTP connections (Apache HttpClient 5)</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>positive integer</td>
 * <td>no</td>
 * <td>200</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_MAX_CONNECTIONS_PER_HOST}</td>
 * <td>Max pooled HTTP connections per route (Apache HttpClient 5)</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>positive integer</td>
 * <td>no</td>
 * <td>100</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_RESPONSE_BUFFER_LIMIT}</td>
 * <td>Eagerly buffer response bodies up to this size and release the
 * connection immediately (Apache HttpClient 5). Larger bodies stream.</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>size in bytes</td>
 * <td>no</td>
 * <td>1048576 (1 MiB)</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_REQUEST_BODY_MODE}</td>
 * <td>How Apache HttpClient 5 sends request bodies:
 * {@code auto} (default) and {@code stream} write once to the wire (known
 * length via {@code StreamableOutput}, otherwise chunked; gzip applied
 * on the fly); {@code materialize} buffers to heap/disk for a known
 * Content-Length and classloader-wide spool limits.</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>{@code auto} | {@code stream} | {@code materialize}</td>
 * <td>no</td>
 * <td>{@code auto}</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_REQUEST_MEMORY_LIMIT}</td>
 * <td>Max request body bytes kept in heap before spilling to a temp file
 * when {@link #HTTP_REQUEST_BODY_MODE} is {@code materialize}. Preserves
 * Content-Length without loading large uploads entirely into memory.</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>size in bytes</td>
 * <td>no</td>
 * <td>1048576 (1 MiB)</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_TEMP_DIR}</td>
 * <td>Directory for request-body temp files when spooling
 * (Apache HttpClient 5)</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>directory path</td>
 * <td>no</td>
 * <td>JVM default temp directory</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_REQUEST_SPOOL_MAX_SIZE}</td>
 * <td>Max size of a single materialized request body (heap and/or disk)
 * (Apache HttpClient 5)</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>size in bytes</td>
 * <td>no</td>
 * <td>5368709120 (5 GiB)</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_REQUEST_SPOOL_MAX_CONCURRENT}</td>
 * <td>Max concurrent request-body materializations (heap and/or disk)
 * classloader-wide (Apache HttpClient 5). The concurrency permit is held only
 * while the body is being buffered; after materialization the permit is
 * released and only the byte budget remains charged through HTTP send /
 * cleanup. Locked by the first body materialization; later sessions with a
 * different value are rejected. Not a multi-webapp JVM/host limit unless
 * OpenCMIS is on a shared classloader.</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>positive integer</td>
 * <td>no</td>
 * <td>32</td>
 * </tr>
 * <tr>
 * <td>{@link #HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES}</td>
 * <td>Max total bytes across all active materialized request bodies (heap
 * and/or disk) classloader-wide (Apache HttpClient 5). When the budget is
 * exhausted, new bodies wait up to {@link #HTTP_CONNECTION_REQUEST_TIMEOUT}
 * for active bodies to release bytes before failing; a body that alone
 * exceeds the budget fails immediately. Locked together with
 * {@link #HTTP_REQUEST_SPOOL_MAX_CONCURRENT} on first materialization; later
 * sessions with a different value are rejected.</td>
 * <td>AtomPub / Browser with {@code ApacheClientHttpInvoker}</td>
 * <td>size in bytes</td>
 * <td>no</td>
 * <td>21474836480 (20 GiB)</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>Cache settings</b></td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_CLASS}</td>
 * <td>Cache implementation class</td>
 * <td>all</td>
 * <td>class name</td>
 * <td>no</td>
 * <td>org.apache.chemistry.opencmis.client.runtime.cache.CacheImpl</td>
 * </tr>
 * <tr>
 * <td>{@link #TYPE_DEFINITION_CACHE_CLASS}</td>
 * <td>Type definition cache implementation class</td>
 * <td>all</td>
 * <td>class name</td>
 * <td>no</td>
 * <td>
 * org.apache.chemistry.opencmis.client.bindings.impl.TypeDefinitionCacheImpl</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_SIZE_OBJECTS}</td>
 * <td>Object cache size</td>
 * <td>all</td>
 * <td>number of object entries</td>
 * <td>no</td>
 * <td>1000</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_TTL_OBJECTS}</td>
 * <td>Object cache time-to-live</td>
 * <td>all</td>
 * <td>time in milliseconds</td>
 * <td>no</td>
 * <td>7200000 (2 hours)</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_SIZE_PATHTOID}</td>
 * <td>Path-to-id cache size</td>
 * <td>all</td>
 * <td>number of path to object link entries</td>
 * <td>no</td>
 * <td>1000</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_TTL_PATHTOID}</td>
 * <td>Path-to-id cache time-to-live</td>
 * <td>all</td>
 * <td>time in milliseconds</td>
 * <td>no</td>
 * <td>180000 (30 minutes)</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_PATH_OMIT}</td>
 * <td>Turn off path-to-id cache</td>
 * <td>all</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>false</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_SIZE_REPOSITORIES}</td>
 * <td>Repository info cache size</td>
 * <td>all</td>
 * <td>number of repository info entries</td>
 * <td>no</td>
 * <td>10</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_SIZE_TYPES}</td>
 * <td>Type definition cache size</td>
 * <td>all</td>
 * <td>number of type definition entries</td>
 * <td>no</td>
 * <td>100</td>
 * </tr>
 * <tr>
 * <td>{@link #CACHE_SIZE_LINKS}</td>
 * <td>AtomPub link cache size</td>
 * <td>AtomPub</td>
 * <td>number of link entries</td>
 * <td>no</td>
 * <td>400</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>AtomPub Binding settings</b></td>
 * </tr>
 * <tr>
 * <td>{@link #ATOMPUB_URL}</td>
 * <td>AtomPub service document URL</td>
 * <td>AtomPub</td>
 * <td>URL</td>
 * <td>yes</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>Web Services Binding settings</b></td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_REPOSITORY_SERVICE}</td>
 * <td>Repository Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_REPOSITORY_SERVICE} or
 * {@link #WEBSERVICES_REPOSITORY_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_REPOSITORY_SERVICE_ENDPOINT}</td>
 * <td>Repository Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_REPOSITORY_SERVICE} or
 * {@link #WEBSERVICES_REPOSITORY_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_NAVIGATION_SERVICE}</td>
 * <td>Navigation Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_NAVIGATION_SERVICE} or
 * {@link #WEBSERVICES_NAVIGATION_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_NAVIGATION_SERVICE_ENDPOINT}</td>
 * <td>Navigation Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_NAVIGATION_SERVICE} or
 * {@link #WEBSERVICES_NAVIGATION_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_OBJECT_SERVICE}</td>
 * <td>Object Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_OBJECT_SERVICE} or
 * {@link #WEBSERVICES_OBJECT_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_OBJECT_SERVICE_ENDPOINT}</td>
 * <td>Object Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_OBJECT_SERVICE} or
 * {@link #WEBSERVICES_OBJECT_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_VERSIONING_SERVICE}</td>
 * <td>Versioning Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_VERSIONING_SERVICE} or
 * {@link #WEBSERVICES_VERSIONING_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_VERSIONING_SERVICE_ENDPOINT}</td>
 * <td>Versioning Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_VERSIONING_SERVICE} or
 * {@link #WEBSERVICES_VERSIONING_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_DISCOVERY_SERVICE}</td>
 * <td>Discovery Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_DISCOVERY_SERVICE} or
 * {@link #WEBSERVICES_DISCOVERY_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_DISCOVERY_SERVICE_ENDPOINT}</td>
 * <td>Discovery Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_DISCOVERY_SERVICE} or
 * {@link #WEBSERVICES_DISCOVERY_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_RELATIONSHIP_SERVICE}</td>
 * <td>Relationship Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_RELATIONSHIP_SERVICE} or
 * {@link #WEBSERVICES_RELATIONSHIP_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_RELATIONSHIP_SERVICE_ENDPOINT}</td>
 * <td>Relationship Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_DISCOVERY_SERVICE} or
 * {@link #WEBSERVICES_RELATIONSHIP_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_MULTIFILING_SERVICE}</td>
 * <td>Multifiling Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_MULTIFILING_SERVICE} or
 * {@link #WEBSERVICES_MULTIFILING_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_MULTIFILING_SERVICE_ENDPOINT}</td>
 * <td>Multifiling Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_MULTIFILING_SERVICE} or
 * {@link #WEBSERVICES_MULTIFILING_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_POLICY_SERVICE}</td>
 * <td>Policy Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_POLICY_SERVICE} or
 * {@link #WEBSERVICES_POLICY_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_POLICY_SERVICE_ENDPOINT}</td>
 * <td>Policy Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_POLICY_SERVICE} or
 * {@link #WEBSERVICES_POLICY_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_ACL_SERVICE}</td>
 * <td>ACL Service WSDL URL</td>
 * <td>Web Services</td>
 * <td>WSDL URL</td>
 * <td>either {@link #WEBSERVICES_ACL_SERVICE} or
 * {@link #WEBSERVICES_ACL_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_ACL_SERVICE_ENDPOINT}</td>
 * <td>ACL Service endpoint URL</td>
 * <td>Web Services</td>
 * <td>Endpoint URL</td>
 * <td>either {@link #WEBSERVICES_ACL_SERVICE} or
 * {@link #WEBSERVICES_ACL_SERVICE_ENDPOINT} must be set</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_MEMORY_THRESHOLD}</td>
 * <td>Documents smaller than the threshold are kept in main memory, larger
 * documents are written to a temporary file</td>
 * <td>Web Services</td>
 * <td>size in bytes</td>
 * <td>no</td>
 * <td>4194304 (4MB)</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_REPSONSE_MEMORY_THRESHOLD}</td>
 * <td>Web Service responses (XML SOAP parts) smaller than the threshold are
 * kept in main memory, larger responses are written to a temporary file</td>
 * <td>Web Services</td>
 * <td>size in bytes</td>
 * <td>no</td>
 * <td>(JAX-WS implementation default)</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_TEMP_DIRECTORY}</td>
 * <td>Sets the path for temp files to an existing directory</td>
 * <td>Web Services</td>
 * <td>path to temp directory</td>
 * <td>no</td>
 * <td>(JAX-WS implementation default)</td>
 * </tr>
 * <tr>
 * <td>{@link #WEBSERVICES_TEMP_ENCRYPT}</td>
 * <td>Defines whether temp files should be encrypted or not</td>
 * <td>Web Services</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"false"</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>Browser Binding</b></td>
 * </tr>
 * <tr>
 * <td>{@link #BROWSER_URL}</td>
 * <td>Browser binding service document URL</td>
 * <td>Browser</td>
 * <td>URL</td>
 * <td>yes</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td>{@link #BROWSER_SUCCINCT}</td>
 * <td>Defines if properties should be sent in the succinct format</td>
 * <td>Browser</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"true"</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>Local Binding</b></td>
 * </tr>
 * <tr>
 * <td>{@link #LOCAL_FACTORY}</td>
 * <td>Class name of the local service factory (if client and server reside in
 * the same JVM)</td>
 * <td>Local</td>
 * <td>class name</td>
 * <td>yes</td>
 * <td>-</td>
 * </tr>
 * <tr>
 * <td colspan="6"><b>Workarounds</b></td>
 * </tr>
 * <tr>
 * <td>{@link #INCLUDE_OBJECTID_URL_PARAM_ON_CHECKOUT}</td>
 * <td>Defines if the object ID should be added to the check out URL<br>
 * (Workaround for SharePoint 2010)</td>
 * <td>AtomPub</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"false"</td>
 * </tr>
 * <tr>
 * <td>{@link #INCLUDE_OBJECTID_URL_PARAM_ON_MOVE}</td>
 * <td>Defines if the object ID should be added to the move URL<br>
 * (Workaround for SharePoint 2010)</td>
 * <td>AtomPub</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"false"</td>
 * </tr>
 * <tr>
 * <td>{@link #OMIT_CHANGE_TOKENS}</td>
 * <td>Defines if the change token should be omitted for updating calls<br>
 * (Workaround for SharePoint 2010 and SharePoint 2013)</td>
 * <td>all</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"false"</td>
 * </tr>
 * <tr>
 * <td>{@link #ADD_NAME_ON_CHECK_IN}</td>
 * <td>Defines if the document name should be added to the properties on check
 * in if no properties are updated<br>
 * (Workaround for SharePoint 2010 and SharePoint 2013)</td>
 * <td>AtomPub</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"false"</td>
 * </tr>
 * <tr>
 * <td>{@link #LATEST_VERSION_WITH_VERSION_SERIES_ID}</td>
 * <td>Defines if getObjectOfLatestVersion should use the version series ID
 * instead of the object ID<br>
 * (Workaround for SharePoint 2010 and SharePoint 2013)</td>
 * <td>AtomPub</td>
 * <td>"true", "false"</td>
 * <td>no</td>
 * <td>"false"</td>
 * </tr>
 * </table>
 */
public final class SessionParameter {

    // ---- general parameter ----
    public static final String USER = "org.apache.chemistry.opencmis.user";
    public static final String PASSWORD = "org.apache.chemistry.opencmis.password";

    // --- binding parameter ----
    /** Predefined binding types (see {@code BindingType}). */
    public static final String BINDING_TYPE = "org.apache.chemistry.opencmis.binding.spi.type";

    /** Class name of the binding class. */
    public static final String BINDING_SPI_CLASS = "org.apache.chemistry.opencmis.binding.spi.classname";

    /**
     * Forces OpenCMIS to use the specified CMIS version and ignore the CMIS
     * version reported by the repository.
     */
    public static final String FORCE_CMIS_VERSION = "org.apache.chemistry.opencmis.cmisversion";

    /** URL of the AtomPub service document. */
    public static final String ATOMPUB_URL = "org.apache.chemistry.opencmis.binding.atompub.url";

    /** WSDL URLs for Web Services. */
    public static final String WEBSERVICES_REPOSITORY_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.RepositoryService";
    public static final String WEBSERVICES_NAVIGATION_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.NavigationService";
    public static final String WEBSERVICES_OBJECT_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.ObjectService";
    public static final String WEBSERVICES_VERSIONING_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.VersioningService";
    public static final String WEBSERVICES_DISCOVERY_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.DiscoveryService";
    public static final String WEBSERVICES_RELATIONSHIP_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.RelationshipService";
    public static final String WEBSERVICES_MULTIFILING_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.MultiFilingService";
    public static final String WEBSERVICES_POLICY_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.PolicyService";
    public static final String WEBSERVICES_ACL_SERVICE = "org.apache.chemistry.opencmis.binding.webservices.ACLService";

    /** Endpoint URLs for Web Services. */
    public static final String WEBSERVICES_REPOSITORY_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.RepositoryService.endpoint";
    public static final String WEBSERVICES_NAVIGATION_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.NavigationService.endpoint";
    public static final String WEBSERVICES_OBJECT_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.ObjectService.endpoint";
    public static final String WEBSERVICES_VERSIONING_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.VersioningService.endpoint";
    public static final String WEBSERVICES_DISCOVERY_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.DiscoveryService.endpoint";
    public static final String WEBSERVICES_RELATIONSHIP_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.RelationshipService.endpoint";
    public static final String WEBSERVICES_MULTIFILING_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.MultiFilingService.endpoint";
    public static final String WEBSERVICES_POLICY_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.PolicyService.endpoint";
    public static final String WEBSERVICES_ACL_SERVICE_ENDPOINT = "org.apache.chemistry.opencmis.binding.webservices.ACLService.endpoint";

    public static final String WEBSERVICES_MEMORY_THRESHOLD = "org.apache.chemistry.opencmis.binding.webservices.memoryThreshold";
    public static final String WEBSERVICES_REPSONSE_MEMORY_THRESHOLD = "org.apache.chemistry.opencmis.binding.webservices.responseMemoryThreshold";

    public static final String WEBSERVICES_TEMP_DIRECTORY = "org.apache.chemistry.opencmis.binding.webservices.tempDirectory";
    public static final String WEBSERVICES_TEMP_ENCRYPT = "org.apache.chemistry.opencmis.binding.webservices.tempEncrypt";

    public static final String WEBSERVICES_PORT_PROVIDER_CLASS = "org.apache.chemistry.opencmis.binding.webservices.portprovider.classname";

    public static final String WEBSERVICES_JAXWS_IMPL = "org.apache.chemistry.opencmis.binding.webservices.jaxws.impl";

    /** URL of the Browser Binding entry point. */
    public static final String BROWSER_URL = "org.apache.chemistry.opencmis.binding.browser.url";
    public static final String BROWSER_SUCCINCT = "org.apache.chemistry.opencmis.binding.browser.succinct";
    public static final String BROWSER_DATETIME_FORMAT = "org.apache.chemistry.opencmis.binding.browser.datetimeformat";

    /** Factory class name for the local binding. */
    public static final String LOCAL_FACTORY = "org.apache.chemistry.opencmis.binding.local.classname";

    // --- authentication ---

    /** Class name of the authentication provider. */
    public static final String AUTHENTICATION_PROVIDER_CLASS = "org.apache.chemistry.opencmis.binding.auth.classname";

    /**
     * Toggle for HTTP basic authentication. Evaluated by the standard
     * authentication provider.
     */
    public static final String AUTH_HTTP_BASIC = "org.apache.chemistry.opencmis.binding.auth.http.basic";
    public static final String AUTH_HTTP_BASIC_CHARSET = "org.apache.chemistry.opencmis.binding.auth.http.basic.charset";

    /**
     * Toggle for OAuth Bearer token authentication. Evaluated by the standard
     * authentication provider.
     */
    public static final String AUTH_OAUTH_BEARER = "org.apache.chemistry.opencmis.binding.auth.http.oauth.bearer";

    /**
     * Toggle for WS-Security UsernameToken authentication. Evaluated by the
     * standard authentication provider.
     */
    public static final String AUTH_SOAP_USERNAMETOKEN = "org.apache.chemistry.opencmis.binding.auth.soap.usernametoken";

    // --- OAuth ---

    public static final String OAUTH_CLIENT_ID = "org.apache.chemistry.opencmis.oauth.clientId";
    public static final String OAUTH_CLIENT_SECRET = "org.apache.chemistry.opencmis.oauth.clientSecret";
    public static final String OAUTH_CODE = "org.apache.chemistry.opencmis.oauth.code";
    public static final String OAUTH_TOKEN_ENDPOINT = "org.apache.chemistry.opencmis.oauth.tokenEndpoint";
    public static final String OAUTH_REDIRECT_URI = "org.apache.chemistry.opencmis.oauth.redirectUri";

    public static final String OAUTH_ACCESS_TOKEN = "org.apache.chemistry.opencmis.oauth.accessToken";
    public static final String OAUTH_REFRESH_TOKEN = "org.apache.chemistry.opencmis.oauth.refreshToken";
    public static final String OAUTH_EXPIRATION_TIMESTAMP = "org.apache.chemistry.opencmis.oauth.expirationTimestamp";
    public static final String OAUTH_DEFAULT_TOKEN_LIFETIME = "org.apache.chemistry.opencmis.oauth.defaultTokenLifetime";

    // --- client certificates ---

    public static final String CLIENT_CERT_KEYFILE = "org.apache.chemistry.opencmis.clientcerts.keyfile";
    public static final String CLIENT_CERT_PASSPHRASE = "org.apache.chemistry.opencmis.clientcerts.passphrase";

    // --- connection ---

    public static final String HTTP_INVOKER_CLASS = "org.apache.chemistry.opencmis.binding.httpinvoker.classname";

    public static final String COMPRESSION = "org.apache.chemistry.opencmis.binding.compression";
    public static final String CLIENT_COMPRESSION = "org.apache.chemistry.opencmis.binding.clientcompression";

    public static final String COOKIES = "org.apache.chemistry.opencmis.binding.cookies";

    public static final String HEADER = "org.apache.chemistry.opencmis.binding.header";

    public static final String CONNECT_TIMEOUT = "org.apache.chemistry.opencmis.binding.connecttimeout";
    public static final String READ_TIMEOUT = "org.apache.chemistry.opencmis.binding.readtimeout";

    /**
     * Apache HttpClient 5: max milliseconds to wait for a free pooled connection
     * and for a free classloader-wide request-body materialization permit.
     */
    public static final String HTTP_CONNECTION_REQUEST_TIMEOUT = "org.apache.chemistry.opencmis.binding.http.connectionrequesttimeout";

    /**
     * Apache HttpClient 5: max total connections in the pool.
     */
    public static final String HTTP_MAX_CONNECTIONS = "org.apache.chemistry.opencmis.binding.http.maxconnections";

    /**
     * Apache HttpClient 5: max connections per route (host).
     */
    public static final String HTTP_MAX_CONNECTIONS_PER_HOST = "org.apache.chemistry.opencmis.binding.http.maxconnectionsperhost";

    /**
     * Apache HttpClient 5: eagerly buffer response bodies up to this many bytes
     * so pooled connections are released immediately. Larger responses stream.
     */
    public static final String HTTP_RESPONSE_BUFFER_LIMIT = "org.apache.chemistry.opencmis.binding.http.responsebufferlimit";

    /**
     * Apache HttpClient 5: request body send mode.
     * <ul>
     * <li>{@code auto} (default) — stream to the wire (known length via
     * {@code StreamableOutput} uses {@code Content-Length}; otherwise chunked
     * {@code writeTo}), matching JDK {@code DefaultHttpInvoker} for large ECM
     * uploads. Same wire behaviour as {@code stream}</li>
     * <li>{@code stream} — always stream (never spool)</li>
     * <li>{@code materialize} — always buffer to heap/disk for Content-Length
     * and classloader-wide spool limits (use when a proxy/server requires
     * Content-Length or when spool back-pressure is desired)</li>
     * </ul>
     */
    public static final String HTTP_REQUEST_BODY_MODE = "org.apache.chemistry.opencmis.binding.http.requestbodymode";

    /** Value for {@link #HTTP_REQUEST_BODY_MODE}: stream when possible. */
    public static final String HTTP_REQUEST_BODY_MODE_AUTO = "auto";

    /** Value for {@link #HTTP_REQUEST_BODY_MODE}: always stream. */
    public static final String HTTP_REQUEST_BODY_MODE_STREAM = "stream";

    /** Value for {@link #HTTP_REQUEST_BODY_MODE}: always materialize. */
    public static final String HTTP_REQUEST_BODY_MODE_MATERIALIZE = "materialize";

    /**
     * Apache HttpClient 5: max request body bytes kept in heap before spilling
     * to a temporary file when materializing (preserves Content-Length for
     * large uploads).
     */
    public static final String HTTP_REQUEST_MEMORY_LIMIT = "org.apache.chemistry.opencmis.binding.http.requestmemorylimit";

    /**
     * Apache HttpClient 5: directory for request-body temp files.
     */
    public static final String HTTP_TEMP_DIR = "org.apache.chemistry.opencmis.binding.http.tempdir";

    /**
     * Apache HttpClient 5: max size of a single materialized request body
     * (heap and/or disk).
     */
    public static final String HTTP_REQUEST_SPOOL_MAX_SIZE = "org.apache.chemistry.opencmis.binding.http.requestspoolmaxsize";

    /**
     * Apache HttpClient 5: max concurrent request-body materializations
     * (heap and/or disk; classloader-wide; locked on first materialization).
     */
    public static final String HTTP_REQUEST_SPOOL_MAX_CONCURRENT = "org.apache.chemistry.opencmis.binding.http.requestspoolmaxconcurrent";

    /**
     * Apache HttpClient 5: max total bytes across all active materialized
     * request bodies (heap and/or disk; classloader-wide; locked with
     * {@link #HTTP_REQUEST_SPOOL_MAX_CONCURRENT} on first materialization).
     * When exhausted, new bodies wait up to
     * {@link #HTTP_CONNECTION_REQUEST_TIMEOUT} for budget instead of failing
     * immediately.
     */
    public static final String HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES = "org.apache.chemistry.opencmis.binding.http.requestspoolmaxtotalbytes";

    public static final String PROXY_USER = "org.apache.chemistry.opencmis.binding.proxyuser";
    public static final String PROXY_PASSWORD = "org.apache.chemistry.opencmis.binding.proxypassword";

    public static final String CSRF_HEADER = "org.apache.chemistry.opencmis.binding.csrfheader";

    public static final String USER_AGENT = "org.apache.chemistry.opencmis.binding.useragent";

    // --- cache ---

    public static final String CACHE_SIZE_OBJECTS = "org.apache.chemistry.opencmis.cache.objects.size";
    public static final String CACHE_TTL_OBJECTS = "org.apache.chemistry.opencmis.cache.objects.ttl";
    public static final String CACHE_SIZE_PATHTOID = "org.apache.chemistry.opencmis.cache.pathtoid.size";
    public static final String CACHE_TTL_PATHTOID = "org.apache.chemistry.opencmis.cache.pathtoid.ttl";
    public static final String CACHE_PATH_OMIT = "org.apache.chemistry.opencmis.cache.path.omit";

    public static final String CACHE_SIZE_REPOSITORIES = "org.apache.chemistry.opencmis.binding.cache.repositories.size";
    public static final String CACHE_SIZE_TYPES = "org.apache.chemistry.opencmis.binding.cache.types.size";
    public static final String CACHE_SIZE_LINKS = "org.apache.chemistry.opencmis.binding.cache.links.size";

    // --- session control ---

    public static final String LOCALE_ISO639_LANGUAGE = "org.apache.chemistry.opencmis.locale.iso639";
    public static final String LOCALE_ISO3166_COUNTRY = "org.apache.chemistry.opencmis.locale.iso3166";
    public static final String LOCALE_VARIANT = "org.apache.chemistry.opencmis.locale.variant";

    public static final String OBJECT_FACTORY_CLASS = "org.apache.chemistry.opencmis.objectfactory.classname";
    public static final String CACHE_CLASS = "org.apache.chemistry.opencmis.cache.classname";
    public static final String TYPE_DEFINITION_CACHE_CLASS = "org.apache.chemistry.opencmis.cache.types.classname";

    public static final String REPOSITORY_ID = "org.apache.chemistry.opencmis.session.repository.id";

    // --- workarounds ---

    public static final String INCLUDE_OBJECTID_URL_PARAM_ON_CHECKOUT = "org.apache.chemistry.opencmis.workaround.includeObjectIdOnCheckout";
    public static final String INCLUDE_OBJECTID_URL_PARAM_ON_MOVE = "org.apache.chemistry.opencmis.workaround.includeObjectIdOnMove";
    public static final String OMIT_CHANGE_TOKENS = "org.apache.chemistry.opencmis.workaround.omitChangeTokens";
    public static final String ADD_NAME_ON_CHECK_IN = "org.apache.chemistry.opencmis.workaround.addNameOnCheckIn";
    public static final String LATEST_VERSION_WITH_VERSION_SERIES_ID = "org.apache.chemistry.opencmis.workaround.getLatestVersionWithVersionSeriesId";

    // utility class
    private SessionParameter() {
    }
}
