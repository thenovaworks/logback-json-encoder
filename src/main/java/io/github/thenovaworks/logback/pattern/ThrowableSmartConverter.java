package io.github.thenovaworks.logback.pattern;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.status.ErrorStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ThrowableSmartConverter extends ThrowableHandlingConverter {

    protected static final int BUILDER_CAPACITY = 2048;
    int lengthOption;
    List<EventEvaluator<ILoggingEvent>> evaluatorList = null;

    int errorCount = 0;

    private List<String> excludes;
    private List<String> includes;

    private boolean compactTraceMode = false;
    private final int COMPACT_MAX_LINE = 30;

    public ThrowableSmartConverter() {
        super();
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public void setCompactTraceMode(boolean compactTraceMode) {
        this.compactTraceMode = compactTraceMode;
    }

    public void start() {
        String lengthStr = this.getFirstOption();
        if (lengthStr == null) {
            this.lengthOption = Integer.MAX_VALUE;
        } else {
            lengthStr = lengthStr.toLowerCase();
            if ("full".equals(lengthStr)) {
                this.lengthOption = Integer.MAX_VALUE;
            } else if ("short".equals(lengthStr)) {
                this.lengthOption = 1;
            } else {
                try {
                    this.lengthOption = Integer.parseInt(lengthStr);
                } catch (NumberFormatException var9) {
                    this.addError("Could not parse [" + lengthStr + "] as an integer");
                    this.lengthOption = Integer.MAX_VALUE;
                }
            }
        }
        List<String> optionList = this.getOptionList();
        if (optionList != null && optionList.size() > 1) {
            int optionListSize = optionList.size();
            for (int i = 1; i < optionListSize; ++i) {
                String evaluatorOrIgnoredStackTraceLine = (String) optionList.get(i);
                Context context = this.getContext();
                Map<String, EventEvaluator<?>> evaluatorMap = (Map) context.getObject("EVALUATOR_MAP");
                EventEvaluator<ILoggingEvent> ee = (EventEvaluator) evaluatorMap.get(evaluatorOrIgnoredStackTraceLine);
                if (ee != null) {
                    this.addEvaluator(ee);
                }
            }
        }

        if (compactTraceMode) {
            if (this.excludes == null) {
                this.excludes = new ArrayList<>();
            }
            this.excludes.addAll(Arrays.asList("$$FastClassByCGLIB$$", "$$EnhancerBySpringCGLIB$$", "sun.reflect.NativeMethodAccessorImpl.invoke", "sun.reflect.DelegatingMethodAccessorImpl.invoke", "sun.reflect.GeneratedMethodAccessor", "java.lang.reflect.Method.invoke", "com.sun.", "sun.net.", "net.sf.cglib.proxy.MethodProxy.invoke", "org.springframework.cglib"));
            this.lengthOption = COMPACT_MAX_LINE;
        }
        super.start();
    }

    private void addEvaluator(EventEvaluator<ILoggingEvent> ee) {
        if (this.evaluatorList == null) {
            this.evaluatorList = new ArrayList<>();
        }
        this.evaluatorList.add(ee);
    }

    public void stop() {
        this.evaluatorList = null;
        super.stop();
    }

    public String convert(ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp == null) {
            return "";
        } else {
            if (this.evaluatorList != null) {
                boolean printStack = true;
                for (int i = 0; i < this.evaluatorList.size(); ++i) {
                    EventEvaluator<ILoggingEvent> ee = (EventEvaluator) this.evaluatorList.get(i);
                    try {
                        if (ee.evaluate(event)) {
                            printStack = false;
                            break;
                        }
                    } catch (EvaluationException var8) {
                        ++this.errorCount;
                        if (this.errorCount < 4) {
                            this.addError("Exception thrown for evaluator named [" + ee.getName() + "]", var8);
                        } else if (this.errorCount == 4) {
                            ErrorStatus errorStatus = new ErrorStatus("Exception thrown for evaluator named [" + ee.getName() + "].", this, var8);
                            errorStatus.add(new ErrorStatus("This was the last warning about this evaluator's errors.We don't want the StatusManager to get flooded.", this));
                            this.addStatus(errorStatus);
                        }
                    }
                }
                if (!printStack) {
                    return "";
                }
            }
            return this.throwableProxyToString(tp);
        }
    }

    protected String throwableProxyToString(IThrowableProxy ex) {
        final StringBuilder builder = new StringBuilder(ThrowableSmartConverter.BUILDER_CAPACITY);
        this.recursiveAppend(builder, (String) null, 1, ex);
        return builder.toString();
    }

    private void recursiveAppend(final StringBuilder builder, String prefix, int indent, IThrowableProxy tp) {
        if (tp != null) {
            this.subjoinFirstLine(builder, prefix, indent, tp);
            builder.append(CoreConstants.LINE_SEPARATOR);
            this.subjoinSTEPArray(builder, indent, tp);
            IThrowableProxy[] suppressed = tp.getSuppressed();
            if (suppressed != null) {
                IThrowableProxy[] var6 = suppressed;
                int var7 = suppressed.length;

                for (int var8 = 0; var8 < var7; ++var8) {
                    IThrowableProxy current = var6[var8];
                    this.recursiveAppend(builder, "Suppressed: ", indent + 1, current);
                }
            }
            this.recursiveAppend(builder, "Caused by: ", indent, tp.getCause());
        }
    }

    private void subjoinFirstLine(StringBuilder buf, String prefix, int indent, IThrowableProxy tp) {
        ThrowableProxyUtil.indent(buf, indent - 1);
        if (prefix != null) {
            buf.append(prefix);
        }

        this.subjoinExceptionMessage(buf, tp);
    }

    private static boolean stringMatcher(List<String> list, Predicate<String> predicate) {
        for (final String v : list) {
            if (predicate.test(v)) {
                return true;
            }
        }
        return false;
    }

    private void subjoinExceptionMessage(StringBuilder buf, IThrowableProxy tp) {
        if (tp.isCyclic()) {
            buf.append("[CIRCULAR REFERENCE: ").append(tp.getClassName()).append(": ").append(tp.getMessage()).append(']');
        } else {
            buf.append(tp.getClassName()).append(": ").append(tp.getMessage());
        }
    }

    protected void subjoinSTEPArray(StringBuilder buf, int indent, IThrowableProxy tp) {
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        int commonFrames = tp.getCommonFrames();
        boolean unrestrictedPrinting = this.lengthOption > stepArray.length;
        int maxIndex = unrestrictedPrinting ? stepArray.length : this.lengthOption;
        if (commonFrames > 0 && unrestrictedPrinting) {
            maxIndex -= commonFrames;
        }

        int ignoredCount = 0;

        for (int i = 0; i < maxIndex; ++i) {
            final StackTraceElementProxy element = stepArray[i];
            final String line = element.toString();
            boolean added = true;
            if (this.includes != null) {
                added = this.includeStackTraceLine(line);
                if (added && this.excludes != null) {
                    added = this.excludeStackTraceLine(line);
                }
            } else if (this.excludes != null) {
                added = this.excludeStackTraceLine(line);
            }
            if (added) {
                ThrowableProxyUtil.indent(buf, indent);
                this.printStackLine(buf, ignoredCount, element);
                ignoredCount = 0;
                buf.append(CoreConstants.LINE_SEPARATOR);
            } else {
                ++ignoredCount;
                if (maxIndex < stepArray.length) {
                    ++maxIndex;
                }
            }
        }

        if (ignoredCount > 0) {
            this.printIgnoredCount(buf, ignoredCount);
            buf.append(CoreConstants.LINE_SEPARATOR);
        }

        if (commonFrames > 0 && unrestrictedPrinting) {
            ThrowableProxyUtil.indent(buf, indent);
            buf.append("... ").append(tp.getCommonFrames()).append(" common frames omitted").append(CoreConstants.LINE_SEPARATOR);
        }

    }

    private void printStackLine(StringBuilder buf, int ignoredCount, StackTraceElementProxy element) {
        buf.append(element);
        if (ignoredCount > 0) {
            this.printIgnoredCount(buf, ignoredCount);
        }

    }

    private void printIgnoredCount(StringBuilder buf, int ignoredCount) {
        buf.append(" [").append(ignoredCount).append(" skipped]");
    }

    private boolean includeStackTraceLine(final String line) {
        if (this.includes == null) {
            return true;
        }
        return stringMatcher(this.includes, line::contains);
    }

    private boolean excludeStackTraceLine(final String line) {
        return !stringMatcher(this.excludes, line::contains);
    }
}
