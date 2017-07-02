import ch.qos.logback.core.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender(name="CONSOLE", clazz=ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "name=account-experiment date=%date{ISO8601} level=%level message=%msg\n"
    }
}

root(level=INFO, appenderNames=["CONSOLE"])
