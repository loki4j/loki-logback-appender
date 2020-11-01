package com.github.loki4j.logback;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.loki4j.common.LogRecord;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;

/**
 * Abstract class that provides basic Loki4j batch encoding functionality
 */
public abstract class AbstractLoki4jEncoder extends EncoderBase<LogRecord[]> implements Loki4jEncoder {
    
    protected static final byte[] ZERO_BYTES = new byte[0];

    protected static final Comparator<LogRecord> byTime = (e1, e2) -> {
        var tsCmp = Long.compare(e1.timestampMs, e2.timestampMs);
        return tsCmp == 0 ? Integer.compare(e1.nanos, e2.nanos) : tsCmp;
    };

    protected static final Comparator<LogRecord> byStream = (e1, e2) -> {
        return String.CASE_INSENSITIVE_ORDER.compare(e1.stream, e2.stream);
    };

    public static final class LabelCfg {
        /**
         * Logback pattern to use for log record's label
         */
        String pattern = "host=${HOSTNAME},level=%level";
        /**
         * Character to use as a separator between labels
         */
        char pairSeparator = ',';
        /**
         * Character to use as a separator between label's name and its value
         */
        char keyValueSeparator = '=';
        /**
         * If true, exception info is not added to label.
         * If false, you should take care of proper formatting
         */
        boolean nopex = true;
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        public void setKeyValueSeparator(String keyValueSeparator) {
            this.keyValueSeparator = keyValueSeparator.trim().charAt(0);
        }
        public void setPairSeparator(String pairSeparator) {
            this.pairSeparator = pairSeparator.trim().charAt(0);
        }
        public void setNopex(boolean nopex) {
            this.nopex = nopex;
        }
    }

    public static final class MessageCfg {
        /**
         * Logback pattern to use for log record's message
         */
        String pattern = "l=%level c=%logger{20} t=%thread | %msg %ex";
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }

    protected final Charset charset = Charset.forName("UTF-8");

    protected LabelCfg label = new LabelCfg();

    protected MessageCfg message = new MessageCfg();

    /**
     * If true, log records in batch are sorted by timestamp.
     * If false, records will be sent to Loki in arrival order.
     * Turn this on if you see 'entry out of order' error from Loki.
     */
    protected boolean sortByTime = false;

    /**
     * If you use only one label for all log records, you can
     * set this flag to true and save some CPU time on grouping records by label.
     */
    protected boolean staticLabels = false;

    private PatternLayout labelPatternLayout;
    private PatternLayout messagePatternLayout;

    private final AtomicInteger nanoCounter = new AtomicInteger(0);

    public void start() {
        var labelPattern = label.nopex ? label.pattern + "%nopex" : label.pattern;
        labelPatternLayout = initPatternLayout(labelPattern);
        labelPatternLayout.start();

        messagePatternLayout = initPatternLayout(message.pattern);
        messagePatternLayout.start();

        super.start();
    }

    public void stop() {
        super.stop();

        messagePatternLayout.stop();
        labelPatternLayout.stop();
    }

    public LogRecord eventToRecord(ILoggingEvent e, LogRecord r) {
        r.timestampMs = e.getTimeStamp();
        r.nanos = nanoCounter.updateAndGet(i -> i < 999_999 ? i + 1 : 0);
        r.stream = labelPatternLayout.doLayout(e).intern();
        r.streamHashCode = r.stream.hashCode();
        r.message = messagePatternLayout.doLayout(e);
        return r;
    }

    @Override
    public byte[] headerBytes() {
        return ZERO_BYTES;
    }

    @Override
    public byte[] footerBytes() {
        return ZERO_BYTES;
    }

    @Override
    public byte[] encode(LogRecord[] batch) {
        if (batch.length < 1)
            return ZERO_BYTES;

        if (staticLabels) {
            if (sortByTime) 
                Arrays.sort(batch, byTime);

            return encodeStaticLabels(batch);
        } else {
            var comp = sortByTime ? byStream.thenComparing(byTime) : byStream; 
            Arrays.sort(batch, comp);

            return encodeDynamicLabels(batch);
        }
    }

    protected abstract byte[] encodeStaticLabels(LogRecord[] batch);

    protected abstract byte[] encodeDynamicLabels(LogRecord[] batch);

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(pattern);
        return patternLayout;
    }


    public void setLabel(LabelCfg label) {
        this.label = label;
    }

    public void setMessage(MessageCfg message) {
        this.message = message;
    }

    public void setSortByTime(boolean sortByTime) {
        this.sortByTime = sortByTime;
    }

    public void setStaticLabels(boolean staticLabels) {
        this.staticLabels = staticLabels;
    }
}
