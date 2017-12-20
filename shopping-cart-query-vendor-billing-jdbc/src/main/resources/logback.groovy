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
logger(name="slick.jdbc.JdbcBackend.statement", level=INFO)
//logger(name="slick.jdbc.JdbcBackend.benchmark", level=INFO)
//logger(name="slick.jdbc.StatementInvoker.result", level=INFO)

root(level=INFO, appenderNames=["CONSOLE"])
