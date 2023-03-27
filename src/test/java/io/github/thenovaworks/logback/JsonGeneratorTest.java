package io.github.thenovaworks.logback;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonGeneratorTest {

    @Test
    public void ut1001() throws IOException {
        JsonFactory factory = new JsonFactory();
        StringWriter jsonObjectWriter = new StringWriter();
        JsonGenerator generator = factory.createGenerator(jsonObjectWriter);
        generator.useDefaultPrettyPrinter(); // pretty print JSON
        generator.writeStartObject();
        generator.writeFieldName("empid");
        generator.writeString("120");
        generator.writeFieldName("firstName");
        generator.writeString("Ravi");
        generator.writeFieldName("lastName");
        generator.writeString("Chandra");
        generator.writeFieldName("technologies");
        generator.writeStartArray();
        generator.writeString("SAP");
        generator.writeString("Java");
        generator.writeString("Selenium");
        generator.writeEndArray();
        generator.writeEndObject();
        generator.close(); // to close the generator
        System.out.println(jsonObjectWriter.toString());
    }

    @Test
    public void test() {
        final String value = "%contextName %date{HH:mm:ss.SSS} %-5level %mdc{txId} %-4relative [%thread] %logger{40}.%M\\(%F:%L\\) %msg %C";
        final Pattern p = Pattern.compile("(%F|%C|%M|%L|%file|%class|%method|%line)");
        final Matcher matcher = p.matcher(value);
        System.out.println("CHK - " + matcher.find());
    }
}
