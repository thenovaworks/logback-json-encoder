# logback-json-encoder

Provides a logback encoder and layout to ingest logs in JSON format supported by Jackson.

Originally written to extend the existing "ch.qos.logback.classic.encoder.PatternLayoutEncoder" feature to support
output in JSON format.

The newly written "JsonPatternLayoutEncoder" encoder outputs in JSON format using Jackson features, sets the same
settings as the existing ones, and controls the structure of the output better with some additional setting options.

<br>

## Background

It's important that the output log outputs to a stream in json format, row by line, to set alarm metrics based on specific metrics in the application log.

This will allow you to set alarms and configure automated notifications based on precise metrics through log collectors and analyzers such as CloudWatch, ElasticSearch, and StackDriver.


<br> 

## What to do First?

You only need to add dependency to use `logback-json-encoder`.

- Maven

```xml

<project>
    <properties>
        <logback.version>1.4.6</logback.version>
        <jackson.version>2.14.2</jackson.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.github.thenovaworks</groupId>
            <artifactId>logback-json-encoder</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
``` 

- Gradle

```gradle
dependencies {
    implementation 'io.github.thenovaworks:logback-json-encoder:1.0.0'
}
```

- It should work with Java 17 or higher.

<br>

## Usage

The ConsoleAppender's JSON output layout format.
Just set `io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder` as the encoder class and it will output in
JSON format that supports almost all layout patterns
of <a href="https://logback.qos.ch/manual/layouts.html#conversionWord">PatternLayout-Syntax</a>.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %-5level %-4relative [%thread] %logger{40} %msg</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### FILE

This is an example configuration for RollingFileAppender.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %-5level %-4relative [%thread] %logger{40} %msg</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/logs/myapp.log.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxHistory>20</maxHistory>
            <maxFileSize>100MB</maxFileSize>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

<br>

## Advanced Options

### contextName

```xml
<configuration>
    <contextName>${HOSTNAME}</contextName>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %contextName %-5level %logger{40} %msg</pattern>
        </encoder>
    </appender>
</configuration>
```

### property
```xml
<configuration>
    <property name="appName" value="myapp" scope="context"/>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %property{appName} %-5level %logger{40} %msg</pattern>
        </encoder>
    </appender>
</configuration>
```


### MDCFilter

```java
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.UUID;

public class MDCFilter extends OncePerRequestFilter {

    public static final String DEFAULT_REQUEST_TOKEN_HEADER = "Request_Token";
    public static final String DEFAULT_RESPONSE_TOKEN_HEADER = "Response_Token";
    public static final String REQUEST_IDENTIFYING_MDC_TOKEN_KEY = "txId";

    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws java.io.IOException, ServletException {
        try {
            String token = request.getHeader(DEFAULT_REQUEST_TOKEN_HEADER);
            if (Objects.isNull(token) || token.isBlank()) {
                token = UUID.randomUUID().toString().toUpperCase().replace("-", "");
            }
            MDC.put(REQUEST_IDENTIFYING_MDC_TOKEN_KEY, token);

            response.addHeader(DEFAULT_RESPONSE_TOKEN_HEADER, token);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_IDENTIFYING_MDC_TOKEN_KEY);
        }
    }
}
```

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg</pattern>
        </encoder>
    </appender>
</configuration>
```


### Caller for call-trace

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %caller{5}</pattern>
        </encoder>
    </appender>
</configuration>
```


### StackTrace

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %ex{full}</pattern>
        </encoder>
    </appender>
</configuration>
```


### Includes filter packages for StackTrace

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %ex{full}</pattern>
            <include>io.github.thenovaworks</include>
            <include>org.springframework</include>
            <include>org.mybatis</include>
            <include>software.amazon.awssdk</include>
            <include>com.amazonaws</include>
        </encoder>
    </appender>
</configuration>
```


### Excludes filter packages for StackTrace

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %ex{full}</pattern>
            <exclude>$$FastClassByCGLIB$$</exclude>
            <exclude>$$EnhancerBySpringCGLIB$$</exclude>
            <exclude>sun.reflect</exclude>
            <exclude>java.lang.reflect.Method.invoke</exclude>
            <exclude>org.springframework.cglib</exclude>
            <exclude>org.junit</exclude>
        </encoder>
    </appender>
</configuration>
```


### CompactTraceMode for StackTrace
 
```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %ex{full}</pattern>
            <compactTraceMode>true</compactTraceMode>
        </encoder>
    </appender>
</configuration>
```


### SupportCompositeConvert

It's not recommends 

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %ex{full}</pattern>
            <supportCompositeConvert>true</supportCompositeConvert>
        </encoder>
    </appender>
</configuration>
```


### PrettyPrint for JSON outputs

It is recommended to use only in Test.

```xml
<configuration>
    <appender class="...YourSomeAppender">
        <encoder class="io.github.thenovaworks.logback.encoder.JsonPatternLayoutEncoder">
            <pattern>%date{HH:mm:ss.SSS} %mdc{txId} %-5level %logger{40} %msg %ex{full}</pattern>
            <prettyPrint>true</prettyPrint>
        </encoder>
    </appender>
</configuration>
```
>
