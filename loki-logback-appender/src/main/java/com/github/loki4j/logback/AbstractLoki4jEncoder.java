package com.github.loki4j.logback;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.EncoderBase;

/**
 * Abstract class that provides basic Loki4j batch encoding functionality
 */
public abstract class AbstractLoki4jEncoder extends EncoderBase<LogRecordBatch> implements Loki4jEncoder {
    
    private static final byte[] ZERO_BYTES = new byte[0];

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
        String pattern;
        /**
         * Character to use as a separator between labels
         */
        String pairSeparator = ",";
        /**
         * Character to use as a separator between label's name and its value
         */
        String keyValueSeparator = "=";
        /**
         * If true, exception info is not added to label.
         * If false, you should take care of proper formatting
         */
        boolean nopex = true;
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        public void setKeyValueSeparator(String keyValueSeparator) {
            this.keyValueSeparator = keyValueSeparator;
        }
        public void setPairSeparator(String pairSeparator) {
            this.pairSeparator = pairSeparator;
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

    private LabelCfg label = new LabelCfg();

    private MessageCfg message = new MessageCfg();

    /**
     * If true, log records in batch are sorted by timestamp.
     * If false, records will be sent to Loki in arrival order.
     * Turn this on if you see 'entry out of order' error from Loki.
     */
    private boolean sortByTime = false;

    /**
     * If you use only one label for all log records, you can
     * set this flag to true and save some CPU time on grouping records by label.
     */
    private boolean staticLabels = false;

    private PatternLayout labelPatternLayout;
    private PatternLayout messagePatternLayout;

    private final AtomicInteger nanoCounter = new AtomicInteger(0);

    public void start() {
        // init with default label pattern if not set in config
        var resolvedLblPat = label.pattern == null
            ? "level=%level,host=" + context.getProperty(CoreConstants.HOSTNAME_KEY)
            : label.pattern;
        // check nopex flag
        var labelPattern = label.nopex
            ? resolvedLblPat + "%nopex"
            : resolvedLblPat;

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

    public LogRecord eventToRecord(ILoggingEvent e) {
        return LogRecord.create(
            e.getTimeStamp(),
            nanoCounter.updateAndGet(i -> i < 999_999 ? i + 1 : 0),
            labelPatternLayout.doLayout(e).intern(),
            messagePatternLayout.doLayout(e));
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
    public byte[] encode(LogRecordBatch batch) {
        if (batch.isEmpty())
            return ZERO_BYTES;

        if (staticLabels) {
            if (sortByTime) 
                batch.sort(byTime);

            return encodeStaticLabels(batch);
        } else {
            var comp = sortByTime ? byStream.thenComparing(byTime) : byStream; 
            batch.sort(comp);

            return encodeDynamicLabels(batch);
        }
    }

    protected abstract byte[] encodeStaticLabels(LogRecordBatch batch);

    protected abstract byte[] encodeDynamicLabels(LogRecordBatch batch);

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    protected String[] extractStreamKVPairs(String stream) {
        var pairs = stream.split(Pattern.quote(label.pairSeparator));
        var result = new String[pairs.length * 2];
        for (int i = 0; i < pairs.length; i++) {
            var kv = pairs[i].split(Pattern.quote(label.keyValueSeparator));
            if (kv.length == 2) {
                result[i * 2] = kv[0];
                result[i * 2 + 1] = kv[1];
            } else {
                throw new IllegalArgumentException(String.format(
                    "Unable to split '%s' in '%s' to label key-value pairs, pairSeparator=%s, keyValueSeparator=%s",
                    pairs[i], stream, label.pairSeparator, label.keyValueSeparator));
            }
        }
        return result;
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
