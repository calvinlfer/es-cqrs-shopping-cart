import ch.qos.logback.core.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender(name="CONSOLE", clazz=ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date{ISO8601} [%level] %logger %X{akkaSource} message=%msg\n"
    }
}

logger(name="akka", level=DEBUG)
logger(name="com.outworkers.phantom", level=INFO)
logger(name="org.apache.zookeeper", level=WARN)
logger(name="akka.persistence.cassandra.query.EventsByTagStage", level=INFO)

root(level=INFO, appenderNames=["CONSOLE"])
