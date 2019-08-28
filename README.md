# rest-microservice-tests
Tests of selected micro- and fullstack REST frameworks. 

Selection based on TechEmpower's [Web Framework Benchmarks](https://www.techempower.com/benchmarks/#section=test&amp;runid=a0d523de-091b-4008-b15d-bd4c8aa25066&amp;hw=ph&amp;test=plaintext&amp;l=xan9tr-3&amp;a=2)

N.B. Act Framework is (as of 2019-08-29) incompatible with JDK11, and the JDK HttpClient only available for JDK>=9 thus the two sub-modules

For testing first start
*  [ActRestServer]() in the JDK8 sub-module 
*  [RestServer]() in the JDK11 sub-module
then run

[RestClient]() in the JDK11 sub-module

or try one of the server (e.g. via the paths `/hello`, `/hello/Tom`, `/byteBuffer`) based-on
*  [Spark](http://sparkjava.com/) via [http://localhost:8080/byteBuffer](http://localhost:8080/byteBuffer)
*  [Javalin](https://javalin.io/) via  [http://localhost:8081/byteBuffer](http://localhost:8081/byteBuffer)
*  [Rapidoid](https://www.rapidoid.org/) via [http://localhost:8082/byteBuffer](http://localhost:8082/byteBuffer)
*  [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux) via [http://localhost:8084/byteBuffer](http://localhost:8084/byteBuffer)
*  [Act Framework](http://actframework.org) via [http://localhost:8085/byteBuffer](http://localhost:8085/byteBuffer)

You can have a look on the formatting impact of the JSON/BINARY serialiser via e.g.
* GSON: [http://localhost:8085/byteBuffer/gson](http://localhost:8085/byteBuffer/gson)
* Jackson: [http://localhost:8085/byteBuffer/jackson](http://localhost:8085/byteBuffer/jackson)
* FastJson: [http://localhost:8085/byteBuffer/fastjson](http://localhost:8085/byteBuffer/fastjson)
* Native: [http://localhost:8085/byteBuffer/native](http://localhost:8085/byteBuffer/native)

or limited in size to, for example, 80 samples:
* via URL path: [http://localhost:8085/byteBuffer/fastjson/80](http://localhost:8085/byteBuffer/fastjson/80)
* via URL arguments: [http://localhost:8085/byteBuffer?serialiser=fastjson&size=80](http://localhost:8085/byteBuffer?serialiser=fastjson&size=80)

Some typical results for transferring 1000 x 1 MB/GET (JDK 11.0.4, cpu: Intel(R) Core(TM) i7-8565U CPU @ 1.80GHz)::

     start run #0
     Result: spark    -   JSON via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within 10.55 seconds ->    90.4 MB/s - hash = 14000
     Result: spark    -   JSON via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within  8.88 seconds ->   107.4 MB/s - hash = 14000
     Result: spark    -   JSON via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within 10.33 seconds ->    92.3 MB/s - hash = 14000
     Result: spark    -   JSON via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within 11.51 seconds ->    82.8 MB/s - hash = 14000
     Result: javalin  -   JSON via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within 28.04 seconds ->    34.0 MB/s - hash = 14000
     Result: javalin  -   JSON via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within 25.49 seconds ->    37.4 MB/s - hash = 14000
     Result: javalin  -   JSON via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within 29.68 seconds ->    32.1 MB/s - hash = 14000
     Result: javalin  -   JSON via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within 28.29 seconds ->    33.7 MB/s - hash = 14000
     Result: rapidoid -   JSON via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within 10.87 seconds ->    87.7 MB/s - hash = 14000
     Result: rapidoid -   JSON via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within 10.09 seconds ->    94.5 MB/s - hash = 14000
     Result: rapidoid -   JSON via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within 11.75 seconds ->    81.2 MB/s - hash = 14000
     Result: rapidoid -   JSON via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within 10.87 seconds ->    87.7 MB/s - hash = 14000
     Result: WebFlux  -   JSON via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within 13.62 seconds ->    70.0 MB/s - hash = 14000
     Result: WebFlux  -   JSON via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within 13.19 seconds ->    72.3 MB/s - hash = 14000
     Result: WebFlux  -   JSON via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within 14.62 seconds ->    65.2 MB/s - hash = 14000
     Result: WebFlux  -   JSON via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within 13.91 seconds ->    68.6 MB/s - hash = 14000
     Result: ACT      -   JSON via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within 14.37 seconds ->    66.4 MB/s - hash = 14000
     Result: ACT      -   JSON via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within 11.18 seconds ->    85.3 MB/s - hash = 14000
     Result: ACT      -   JSON via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within 15.28 seconds ->    62.4 MB/s - hash = 14000
     Result: ACT      -   JSON via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within 15.13 seconds ->    63.0 MB/s - hash = 14000
     Result: spark    - BINARY via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within  1.02 seconds ->   934.1 MB/s - hash = 14000
     Result: spark    - BINARY via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within  1.00 seconds ->   956.5 MB/s - hash = 14000
     Result: spark    - BINARY via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within  0.94 seconds ->  1011.3 MB/s - hash = 14000
     Result: spark    - BINARY via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within  0.86 seconds ->  1103.8 MB/s - hash = 14000
     Result: javalin  - BINARY via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within 12.15 seconds ->    78.5 MB/s - hash = 14000
     Result: javalin  - BINARY via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within 11.69 seconds ->    81.6 MB/s - hash = 14000
     Result: javalin  - BINARY via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within 11.68 seconds ->    81.6 MB/s - hash = 14000
     Result: javalin  - BINARY via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within 11.91 seconds ->    80.1 MB/s - hash = 14000
     Result: rapidoid - BINARY via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within  1.10 seconds ->   869.3 MB/s - hash = 14000
     Result: rapidoid - BINARY via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within  0.93 seconds ->  1027.7 MB/s - hash = 14000
     Result: rapidoid - BINARY via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within  0.92 seconds ->  1033.2 MB/s - hash = 14000
     Result: rapidoid - BINARY via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within  0.78 seconds ->  1222.7 MB/s - hash = 14000
     Result: WebFlux  - BINARY via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within  1.04 seconds ->   918.8 MB/s - hash = 14000
     Result: WebFlux  - BINARY via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within  0.97 seconds ->   988.3 MB/s - hash = 14000
     Result: WebFlux  - BINARY via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within  1.03 seconds ->   929.5 MB/s - hash = 14000
     Result: WebFlux  - BINARY via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within  0.81 seconds ->  1181.8 MB/s - hash = 14000
     Result: ACT      - BINARY via           JDK_HTTP_CLIENT: streamed 1000000000 bytes within  2.04 seconds ->   468.2 MB/s - hash = 14000
     Result: ACT      - BINARY via       OK_HTTP_CLIENT_GSON: streamed 1000000000 bytes within  1.99 seconds ->   479.0 MB/s - hash = 14000
     Result: ACT      - BINARY via   OK_HTTP_CLIENT_FASTJSON: streamed 1000000000 bytes within  1.96 seconds ->   487.6 MB/s - hash = 14000
     Result: ACT      - BINARY via       WEBFLUX_HTTP_CLIENT: streamed 1000000000 bytes within  1.86 seconds ->   511.9 MB/s - hash = 14000
     Result: TCP ref  -   JSON via             TCP_REFERENCE: streamed 1000000000 bytes within  7.76 seconds ->   123.0 MB/s - hash = 14000
     Result: TCP ref  - BINARY via             TCP_REFERENCE: streamed 1000000000 bytes within  0.29 seconds ->  3288.5 MB/s - hash = 14000
     run #0 - done


