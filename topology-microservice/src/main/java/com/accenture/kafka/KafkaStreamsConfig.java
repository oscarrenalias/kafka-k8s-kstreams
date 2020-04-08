package net.renalias.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

//@Configuration
@EnableKafkaStreams
@Configuration(proxyBeanMethods = false)
public class KafkaStreamsConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamsConfig.class);

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs(final KafkaProperties kafkaProperties,
            @Value("${spring.application.name}") final String appName) {

        log.debug("Initializing KafkaStreamsConfiguration");
        final Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, appName);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        return new KafkaStreamsConfiguration(props);
    }

    private static class MyMapper implements KeyValueMapper<String, String, KeyValue<String,String>> {        
        @Override
        public KeyValue<String,String> apply(String key, String value) {
            log.info("Processing message: key = " + key + ", value = " + value);
            return(new KeyValue(key, value.toUpperCase()));
        }
    }

    @Bean
    public KStream<String, String> processData(final StreamsBuilder streamsBuilder) {
        final KStream<String, String> stream = streamsBuilder.stream("data");
        stream.map(
            new MyMapper()
        ).to("mapped-data", Produced.with(Serdes.String(), Serdes.String()));
        // end

        return(stream);
    }
}