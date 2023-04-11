package io.github.thenovaworks.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;

import java.nio.charset.Charset;

/**
 * NoLayoutEncoder can be used as a data ingest.
 */
public class MessageOnlyEncoder extends EncoderBase<ILoggingEvent> {

    private Charset charset;

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    private byte[] convertToBytes(final String message) {
        return this.charset == null ? message.getBytes() : message.getBytes(this.charset);
    }

    public byte[] encode(ILoggingEvent event) {
        final String raw = event.getMessage() + CoreConstants.LINE_SEPARATOR;
        return this.convertToBytes(raw);
    }

    public byte[] headerBytes() {
        return null;
    }

    public byte[] footerBytes() {
        return null;
    }

}
