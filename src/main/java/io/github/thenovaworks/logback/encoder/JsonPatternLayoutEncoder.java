package io.github.thenovaworks.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;
import io.github.thenovaworks.logback.layout.JacksonPatternLayout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://logback.qos.ch/manual/layouts.html#conversionWord">PatternLayou-conversionWord</a>
 * @see ch.qos.logback.classic.PatternLayout
 * @see ch.qos.logback.classic.encoder.PatternLayoutEncoder
 * @see ch.qos.logback.core.joran.util.beans.BeanDescriptionFactory
 */
public class JsonPatternLayoutEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {

    // private final static byte[] EMPTY = "".getBytes(StandardCharsets.UTF_8);
    // return JsonEncoder.EMPTY;

    private boolean supportCompositeConvert = false;
    private boolean compactTraceMode = false;
    private boolean prettyPrint = false;
    private List<String> includes;

    private List<String> excludes;

    public JsonPatternLayoutEncoder() {
        super();
    }

    public void setCompactTraceMode(boolean compactTraceMode) {
        this.compactTraceMode = compactTraceMode;
    }

    public void setSupportCompositeConvert(boolean supportCompositeConvert) {
        this.supportCompositeConvert = supportCompositeConvert;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public void addInclude(String include) {
        if (this.includes == null) {
            this.includes = new ArrayList<>();
        }
        this.includes.add(include);
    }

    public void addExclude(final String exclude) {
        if (this.excludes == null) {
            this.excludes = new ArrayList<>();
        }
        this.excludes.add(exclude);
    }


    public byte[] encode(final ILoggingEvent event) {
        final String encoded = this.layout.doLayout(event);
        return getCharset() != null ? encoded.getBytes(getCharset()) : encoded.getBytes(StandardCharsets.UTF_8);
    }

    public void start() {
        final JacksonPatternLayout patternLayout = new JacksonPatternLayout();
        patternLayout.setContext(this.context);
        patternLayout.setPattern(this.getPattern());
        patternLayout.setCompactTraceMode(this.compactTraceMode);
        patternLayout.setPrettyPrint(this.prettyPrint);
        patternLayout.setIncludes(this.includes);
        patternLayout.setExcludes(this.excludes);
        if (this.supportCompositeConvert) {
            patternLayout.setSupportCompositeConverts();
        }
        patternLayout.start();
        this.layout = patternLayout;
        super.start();
    }
}
