package com.github.loki4j.logback;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.StringUtils;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Abstract class that provides basic Loki4j batch encoding functionality
 */
public abstract class AbstractLoki4jEncoder extends ContextAwareBase implements Loki4jEncoder {

    private static final String STATIC_STREAM_KEY = "STATIC_STREAM_KEY";
    private static final String REGEX_STARTER = "regex:";
    
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

    private final AtomicReference<HashMap<String, LogRecordStream>> streams = new AtomicReference<>(new HashMap<>());

    private final AtomicInteger nanoCounter = new AtomicInteger(0);

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
    private volatile boolean staticLabels = false;

    /**
     * Max seen timestamp at the moment.
     * We can not send an event with timestamp less than this,
     * just to avoid 'out of order' from Loki.
     */
    private volatile long maxTimestampMs = 0;

    private Pattern compiledLabelPairSeparator;
    private Pattern compiledLabelKeyValueSeparator;

    private PatternLayout labelPatternLayout;
    private PatternLayout messagePatternLayout;

    private boolean started = false;

    public void start() {
        // init with default label pattern if not set in config
        var resolvedLblPat = label.pattern == null
            ? "level=%level,host=" + context.getProperty(CoreConstants.HOSTNAME_KEY)
            : label.pattern;
        // check nopex flag
        var labelPattern = label.nopex
            ? resolvedLblPat + "%nopex"
            : resolvedLblPat;

        // check if label pair separator is RegEx or literal string
        compiledLabelPairSeparator = label.pairSeparator.startsWith(REGEX_STARTER)
            ? Pattern.compile(label.pairSeparator.substring(REGEX_STARTER.length()))
            : Pattern.compile(Pattern.quote(label.pairSeparator));
        // label key-value separator supports only literal strings
        compiledLabelKeyValueSeparator = Pattern.compile(Pattern.quote(label.keyValueSeparator));

        labelPatternLayout = initPatternLayout(labelPattern);
        labelPatternLayout.start();

        messagePatternLayout = initPatternLayout(message.pattern);
        messagePatternLayout.start();

        this.started = true;
    }

    public void stop() {
        this.started = false;
        messagePatternLayout.stop();
        labelPatternLayout.stop();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public LogRecordStream eventToStream(ILoggingEvent e) {
        return stream(labelPatternLayout.doLayout(e).intern());
    }

    public String eventToMessage(ILoggingEvent e) {
        return messagePatternLayout.doLayout(e);
    }

    public int timestampToNanos(long timestampMs) {
        final long nextMs = timestampMs % 1000; // nextMs=nnn

        if (maxTimestampMs > timestampMs)
            // nnn_999 - can not track the order of events for the previous milliseconds
            return (int)nextMs * 1000 + 999;

        var nanos = nanoCounter.updateAndGet(i -> { // counter structure: i=ccc_xxx
            if (maxTimestampMs == timestampMs) {
                if (i % 1000 < 999)
                    // ccc_xxx+1 - next event in current millisecond
                    return i + 1;
                else
                    // ccc_999 - 999 events already passed
                    // can not track the order of events for the current millisecond anymore
                    return i;
            } else {
                // nnn_000 - advance the counter to the next millisecond
                return (int)nextMs * 1000;
            }
        });
        maxTimestampMs = timestampMs;
        //System.out.println("ts: " + timestampMs + ", ns: " + x);
        return nanos;
    }

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    private LogRecordStream stream(String input) {
        final var streamKey = staticLabels ? STATIC_STREAM_KEY : input;
        return streams
            .updateAndGet(m -> {
                if (!m.containsKey(streamKey)) {
                    var nm = new HashMap<>(m);
                    nm.put(streamKey, LogRecordStream.create(
                        m.size(), extractStreamKVPairs(input)));
                    return nm;
                } else {
                    return m;
                }
            })
            .get(streamKey);
    }

    String[] extractStreamKVPairs(String stream) {
        var pairs = compiledLabelPairSeparator.split(stream);
        var result = new String[pairs.length * 2];
        var pos = 0;
        for (int i = 0; i < pairs.length; i++) {
            if (StringUtils.isBlank(pairs[i])) continue;

            var kv = compiledLabelKeyValueSeparator.split(pairs[i]);
            if (kv.length == 2) {
                result[pos] = kv[0];
                result[pos + 1] = kv[1];
                pos += 2;
            } else {
                throw new IllegalArgumentException(String.format(
                    "Unable to split '%s' in '%s' to label key-value pairs, pairSeparator=%s, keyValueSeparator=%s",
                    pairs[i], stream, label.pairSeparator, label.keyValueSeparator));
            }
        }
        return Arrays.copyOf(result, pos);
    }

    public void setLabel(LabelCfg label) {
        this.label = label;
    }

    public void setMessage(MessageCfg message) {
        this.message = message;
    }

    public boolean getSortByTime() {
        return sortByTime;
    }
    public void setSortByTime(boolean sortByTime) {
        this.sortByTime = sortByTime;
    }

    public boolean getStaticLabels() {
        return staticLabels;
    }
    public void setStaticLabels(boolean staticLabels) {
        this.staticLabels = staticLabels;
    }

}
