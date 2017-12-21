import ch.qos.logback.core.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender(name="CONSOLE", clazz=ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date{ISO8601} [%level] %logger %X{akkaSource} message=%msg\n"
    }
}

logger(name="akka", level=DEBUG)
logger(name="org.apache.zookeeper", level=WARN)
logger(name="akka.persistence.cassandra.query.EventsByTagStage", level=INFO)

// http://slick.lightbend.com/doc/3.2.1/config.html#logging
logger(name="slick.basic.BasicBackend.action", level=DEBUG)
logger(name="slick.jdbc.JdbcBackend.statement", level=DEBUG)
logger(name="slick.jdbc.JdbcBackend.benchmark", level=DEBUG)
//logger(name="slick.jdbc.JdbcBackend.parameter", level=DEBUG)

root(level=INFO, appenderNames=["CONSOLE"])
