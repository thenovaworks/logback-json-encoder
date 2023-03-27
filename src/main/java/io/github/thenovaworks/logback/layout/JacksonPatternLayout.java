package io.github.thenovaworks.logback.layout;

import ch.qos.logback.classic.pattern.*;
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.PostCompileProcessor;
import ch.qos.logback.core.pattern.color.*;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusManager;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.github.thenovaworks.logback.pattern.ConverterUtils;
import io.github.thenovaworks.logback.pattern.ThrowableSmartConverter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see ch.qos.logback.core.encoder.LayoutWrappingEncoder
 * @see ch.qos.logback.classic.encoder.PatternLayoutEncoder
 * @see ch.qos.logback.classic.PatternLayout
 */
public class JacksonPatternLayout extends LayoutBase<ILoggingEvent> {

    private static final Map<String, String> DEFAULT_CONVERTER_MAP = new HashMap<>();
    private static final Map<String, String> CONVERTER_CLASS_TO_KEY_MAP = new HashMap<>();

    private Converter<ILoggingEvent> head;
    private String pattern;
    protected PostCompileProcessor<ILoggingEvent> postCompileProcessor;

    final JsonFactory factory = new JsonFactory();


    private final Pattern patternForCallable = Pattern.compile("(%F|%C|%M|%L|%file|%class|%method|%line)");

    private boolean compactTraceMode = false;
    private boolean supportCallable = false;

    private List<String> includes;
    private List<String> excludes;
    private boolean prettyPrint = false;

    public JacksonPatternLayout() {
        super();
        this.postCompileProcessor = new EnsureExceptionHandling();
    }

    public void setSupportCompositeConverts() {
        // SupportCompositeConvert
        DEFAULT_CONVERTER_MAP.put("black", BlackCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("red", RedCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("green", GreenCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("yellow", YellowCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("blue", BlueCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("magenta", MagentaCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("cyan", CyanCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("white", WhiteCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("gray", GrayCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldRed", BoldRedCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldGreen", BoldGreenCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldYellow", BoldYellowCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldBlue", BoldBlueCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldMagenta", BoldMagentaCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldCyan", BoldCyanCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("boldWhite", BoldWhiteCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("highlight", HighlightingCompositeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("prefix", PrefixCompositeConverter.class.getName());
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    private JsonGenerator jsonGenerator(final StringWriter writer) throws IOException {
        return factory.createGenerator(writer);
    }


    public Map<String, String> getEffectiveConverterMap() {
        Map<String, String> effectiveMap = new HashMap<>(JacksonPatternLayout.DEFAULT_CONVERTER_MAP);

        final Context context = this.getContext();
        if (context != null) {
            Map<String, String> contextMap = (Map<String, String>) context.getObject("PATTERN_RULE_REGISTRY");
            if (contextMap != null) {
                effectiveMap.putAll(contextMap);
            }
        }
        return effectiveMap;
    }

    public String getPattern() {
        return this.pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String toString() {
        return this.getClass().getName() + "(\"" + this.getPattern() + "\")";
    }

    public String doLayout(ILoggingEvent event) {
        return !this.isStarted() ? "" : this.writeLoopOnConverters(event);
    }

    public void start() {
        if (getPattern() != null && getPattern().length() != 0) {
            try {
                final Parser<ILoggingEvent> parser = new Parser<>(getPattern());
                if (this.getContext() != null) {
                    parser.setContext(this.getContext());
                }

                final Node node = parser.parse();
                this.head = parser.compile(node, this.getEffectiveConverterMap());
                if (this.postCompileProcessor != null) {
                    this.postCompileProcessor.process(this.context, this.head);
                }
                ConverterUtil.setContextForConverters(this.getContext(), this.head);
                ConverterUtil.startConverters(this.head);
                ConverterUtils.setThrowableSmartConverter(this.head, this.includes, this.excludes, this.compactTraceMode);
                final Matcher matcher = patternForCallable.matcher(getPattern());
                if (matcher.find()) {
                    supportCallable = true;
                }
                super.start();
            } catch (ScanException ex) {
                StatusManager sm = this.getContext().getStatusManager();
                sm.add(new ErrorStatus("Failed to parse pattern \"" + this.getPattern() + "\".", this, ex));
            }

        } else {
            this.addError("Empty or null pattern.");
        }
    }

    static {
        DEFAULT_CONVERTER_MAP.putAll(Parser.DEFAULT_COMPOSITE_CONVERTER_MAP);
        DEFAULT_CONVERTER_MAP.put("d", DateConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("date", DateConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(DateConverter.class.getName(), "date");
        DEFAULT_CONVERTER_MAP.put("ms", MicrosecondConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("micros", MicrosecondConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(MicrosecondConverter.class.getName(), "micros");
        DEFAULT_CONVERTER_MAP.put("r", RelativeTimeConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("relative", RelativeTimeConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(RelativeTimeConverter.class.getName(), "relative");
        DEFAULT_CONVERTER_MAP.put("level", LevelConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("le", LevelConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("p", LevelConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(LevelConverter.class.getName(), "level");
        DEFAULT_CONVERTER_MAP.put("t", ThreadConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("thread", ThreadConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(ThreadConverter.class.getName(), "thread");
        DEFAULT_CONVERTER_MAP.put("lo", LoggerConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("logger", LoggerConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("c", LoggerConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(LoggerConverter.class.getName(), "logger");
        DEFAULT_CONVERTER_MAP.put("m", MessageConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("msg", MessageConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("message", MessageConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(MessageConverter.class.getName(), "message");
        DEFAULT_CONVERTER_MAP.put("C", ClassOfCallerConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("class", ClassOfCallerConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(ClassOfCallerConverter.class.getName(), "class");
        DEFAULT_CONVERTER_MAP.put("M", MethodOfCallerConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("method", MethodOfCallerConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(MethodOfCallerConverter.class.getName(), "method");
        DEFAULT_CONVERTER_MAP.put("L", LineOfCallerConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("line", LineOfCallerConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(LineOfCallerConverter.class.getName(), "line");
        DEFAULT_CONVERTER_MAP.put("F", FileOfCallerConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("file", FileOfCallerConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(FileOfCallerConverter.class.getName(), "file");
        DEFAULT_CONVERTER_MAP.put("X", MDCConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("mdc", MDCConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("rEx", RootCauseFirstThrowableProxyConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("rootException", RootCauseFirstThrowableProxyConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(RootCauseFirstThrowableProxyConverter.class.getName(), "rstack");
        DEFAULT_CONVERTER_MAP.put("xEx", ExtendedThrowableProxyConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("xException", ExtendedThrowableProxyConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("xThrowable", ExtendedThrowableProxyConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(ExtendedThrowableProxyConverter.class.getName(), "xstack");
        DEFAULT_CONVERTER_MAP.put("nopex", NopThrowableInformationConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("nopexception", NopThrowableInformationConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(NopThrowableInformationConverter.class.getName(), "nstack");
        DEFAULT_CONVERTER_MAP.put("ex", ThrowableSmartConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("exception", ThrowableSmartConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("throwable", ThrowableSmartConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(ThrowableSmartConverter.class.getName(), "stack");
        // exception
        DEFAULT_CONVERTER_MAP.put("cn", ContextNameConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("contextName", ContextNameConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(ContextNameConverter.class.getName(), "contextName");
        //
        DEFAULT_CONVERTER_MAP.put("caller", CallerDataConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(CallerDataConverter.class.getName(), "caller");
        //
        DEFAULT_CONVERTER_MAP.put("marker", MarkerConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(MarkerConverter.class.getName(), "marker");
        //
        DEFAULT_CONVERTER_MAP.put("kvp", KeyValuePairConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(KeyValuePairConverter.class.getName(), "kvp");
        //
        DEFAULT_CONVERTER_MAP.put("property", PropertyConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("n", LineSeparatorConverter.class.getName());
        //
        DEFAULT_CONVERTER_MAP.put("lsn", LocalSequenceNumberConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(LocalSequenceNumberConverter.class.getName(), "lsn");
        //
        DEFAULT_CONVERTER_MAP.put("sn", SequenceNumberConverter.class.getName());
        DEFAULT_CONVERTER_MAP.put("sequenceNumber", SequenceNumberConverter.class.getName());
        CONVERTER_CLASS_TO_KEY_MAP.put(SequenceNumberConverter.class.getName(), "sequenceNumber");
    }

    private String callableConverters(ILoggingEvent event) {
        final StringBuilder builder = new StringBuilder(100);
        for (Converter<ILoggingEvent> c = this.head; c != null; c = c.getNext()) {
            final String value;
            if (c instanceof LiteralConverter) {
                value = c.convert(event).trim();
                if ("".equals(value) || "[".equals(value) || "]".equals(value)) {
                    continue;
                }
                builder.append(value);
            }
            if (c instanceof MethodOfCallerConverter || c instanceof FileOfCallerConverter || c instanceof LineOfCallerConverter) {
                builder.append(c.convert(event));
            }
        }
        return builder.toString();
    }

    public void setCompactTraceMode(boolean compactTraceMode) {
        this.compactTraceMode = compactTraceMode;
    }

    protected String writeLoopOnConverters(ILoggingEvent event) {
        try (final StringWriter writer = new StringWriter(); final JsonGenerator generator = jsonGenerator(writer)) {
            if (this.prettyPrint) {
                generator.useDefaultPrettyPrinter();
            }
            generator.writeStartObject();
            generator.writeFieldName("@timestamp");
            generator.writeNumber(event.getTimeStamp());

            for (Converter<ILoggingEvent> c = this.head; c != null; c = c.getNext()) {
                if (c instanceof LiteralConverter || c instanceof LineSeparatorConverter || c instanceof MethodOfCallerConverter || c instanceof FileOfCallerConverter || c instanceof LineOfCallerConverter) {
                    continue;
                } else if (c instanceof LoggerConverter) {
                    LoggerConverter conv = (LoggerConverter) c;
                    generator.writeFieldName("logger");
                    final String value;
                    if (supportCallable) {
                        final String convertedValue = callableConverters(event);
                        value = conv.convert(event) + convertedValue;
                    } else {
                        value = conv.convert(event);
                    }
                    generator.writeString(value);
                } else if (c instanceof MDCConverter) {
                    MDCConverter conv = (MDCConverter) c;
                    final Map<String, String> mdc = event.getMDCPropertyMap();
                    for (String m : mdc.keySet()) {
                        generator.writeFieldName(m);
                        generator.writeString(conv.convert(event));
                    }
                } else if (c instanceof PropertyConverter) {
                    PropertyConverter conv = (PropertyConverter) c;
                    generator.writeFieldName(conv.getKey());
                    generator.writeString(conv.convert(event));
                } else {
                    final String key = CONVERTER_CLASS_TO_KEY_MAP.get(c.getClass().getName());
                    if (key != null) {
                        generator.writeFieldName(key);
                        generator.writeString(c.convert(event));
                    } else {
                        System.out.println("---JacksonPatternLayout ::: not found key-class: " + c.getClass().getName());
                    }
                }
            } // end for

            generator.writeEndObject();
            generator.writeRaw(CoreConstants.LINE_SEPARATOR);
            generator.close();
            return writer.toString();
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }

    }


}
