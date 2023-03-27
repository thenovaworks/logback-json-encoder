package io.github.thenovaworks.logback.pattern;

import ch.qos.logback.core.pattern.Converter;

import java.util.List;

public final class ConverterUtils {
    public static <E> void setThrowableSmartConverter(
            Converter<E> head, List<String> includes,
            List<String> excludes,
            boolean compactTraceMode) {
        for (Converter<E> c = head; c != null; c = c.getNext()) {
            if (c instanceof ThrowableSmartConverter) {
                ThrowableSmartConverter converter = (ThrowableSmartConverter) c;
                converter.setIncludes(includes);
                converter.setExcludes(excludes);
                converter.setCompactTraceMode(compactTraceMode);
            }
        }
    }
}
