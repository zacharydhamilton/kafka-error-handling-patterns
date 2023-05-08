package com.example.pattern2retrier.retrier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.objects.RoundPeg;
import com.example.objects.SquarePeg;

import com.example.pattern2retrier.util.CommonUtils;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

@Service
public class RetryService {
    private ArrayList<String> REQUIRED_PROPS = new ArrayList<String>(Arrays.asList("BOOTSTRAP_SERVERS", "KAFKA_KEY", "KAFKA_SECRET", "SCHEMA_REGISTRY_URL", "SCHEMA_REGISTRY_KEY", "SCHEMA_REGISTRY_SECRET"));
    private String clientId = (System.getenv("CLIENT_ID") != null) ? System.getenv("CLIENT_ID") : "pattern-2-retrier";
    private String configType = (System.getenv("CONFIG_TYPE") != null) ? System.getenv("CONFIG_TYPE") : "FILE"; 
    private String configFile = (System.getenv("CONFIG") != null) ? System.getenv("CONFIG_FILE") : "streams.properties";
    private String SOURCE_TOPIC = "roundpeg-retry";
    private String SINK_TOPIC = "roundpeg";

    @EventListener(ApplicationStartedEvent.class)
    private void initService() throws ConfigException, IOException {
        Properties streamsConfig = new Properties();
        if (configType.equals("FILE")) {
            CommonUtils.addPropsFromFile(streamsConfig, configFile);
        } else {
            CommonUtils.preInitChecks(REQUIRED_PROPS);
            streamsConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("BOOTSTRAP_SERVERS"));
            streamsConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            streamsConfig.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule   required username='"+System.getenv("KAFKA_KEY")+"'   password='"+System.getenv("KAFKA_SECRET")+"';");
            streamsConfig.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            streamsConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, System.getenv("SCHEMA_REGISTRY_URL"));
            streamsConfig.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
            streamsConfig.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, System.getenv("SCHEMA_REGISTRY_KEY")+":"+System.getenv("SCHEMA_REGISTRY_SECRET"));
        }
        streamsConfig.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        streamsConfig.put(StreamsConfig.APPLICATION_ID_CONFIG, clientId);
        streamsConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfig.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class.getName());
        streamsConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        final KafkaStreams streams = buildStream(streamsConfig);
        streams.start();
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private KafkaStreams buildStream(Properties streamsConfig) {
        final StreamsBuilder builder = new StreamsBuilder();
        GenericAvroSerde genericRetrySerde = createGenericRetrySerde(streamsConfig);
        SpecificAvroSerde<RoundPeg> roundPegSerde = createSquarePegSerde(streamsConfig);
        final KStream<String, GenericRecord> squarePegRetriesStream = builder.stream(SOURCE_TOPIC, Consumed.with(Serdes.String(), genericRetrySerde));
        final KStream<String, RoundPeg> roundPegTargetStream = squarePegRetriesStream.map(new KeyValueMapper<String, GenericRecord, KeyValue<String, RoundPeg>>() {
            public KeyValue<String, RoundPeg> apply(String key, GenericRecord value) {
                System.out.println(String.format("key: %s, value: %s", key, value.toString()));
                RoundPeg roundPeg = new RoundPeg();
                roundPeg.setDiameter(5);
                return new KeyValue<>(key, roundPeg);
            }
        });
        roundPegTargetStream.to(SINK_TOPIC, Produced.with(Serdes.String(), roundPegSerde));
        return new KafkaStreams(builder.build(), streamsConfig);
    }
    private GenericAvroSerde createGenericRetrySerde(Properties streamsConfig) {
        GenericAvroSerde genericRetrySerde = new GenericAvroSerde();
        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, streamsConfig.getProperty("schema.registry.url"));
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, streamsConfig.getProperty("basic.auth.credentials.source"));
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, streamsConfig.getProperty("basic.auth.user.info"));
        genericRetrySerde.configure(serdeConfig, false);
        return genericRetrySerde;
    }
    private SpecificAvroSerde<RoundPeg> createSquarePegSerde(Properties streamsConfig) {
        SpecificAvroSerde<RoundPeg> roundPegSerde = new SpecificAvroSerde<>();
        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, streamsConfig.getProperty("schema.registry.url"));
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, streamsConfig.getProperty("basic.auth.credentials.source"));
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, streamsConfig.getProperty("basic.auth.user.info"));
        roundPegSerde.configure(serdeConfig, false);
        return roundPegSerde;
    }
}
