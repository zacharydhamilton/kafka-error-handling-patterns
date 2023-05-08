package com.example.pattern3retrier.retrier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
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

import com.example.objects.PizzaDough;
import com.example.objects.PizzaSauce;
import com.example.objects.PizzaTopping;
import com.example.objects.RetryableError;
import com.example.pattern3retrier.util.CommonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;

@Service
public class RetryService {
    private ArrayList<String> REQUIRED_PROPS = new ArrayList<String>(Arrays.asList("BOOTSTRAP_SERVERS", "KAFKA_KEY", "KAFKA_SECRET", "SCHEMA_REGISTRY_URL", "SCHEMA_REGISTRY_KEY", "SCHEMA_REGISTRY_SECRET"));
    private String clientId = (System.getenv("CLIENT_ID") != null) ? System.getenv("CLIENT_ID") : "pattern-3-retrier";
    private String configType = (System.getenv("CONFIG_TYPE") != null) ? System.getenv("CONFIG_TYPE") : "FILE"; 
    private String configFile = (System.getenv("CONFIG") != null) ? System.getenv("CONFIG_FILE") : "streams.properties";
    private String TOPIC = "pizza-assembly";
    private String SOURCE_TOPIC = TOPIC+"-retry";
    private String SINK_TOPIC = TOPIC;
    private String REDIRECT_TOPIC = TOPIC+"-redirect";

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
        streamsConfig.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once");
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
        GenericAvroSerde genericAvroSerde = createGenericAvroSerde(streamsConfig);
        final KStream<String, GenericRecord> retryableErrors = builder.stream(SOURCE_TOPIC, Consumed.with(Serdes.String(), genericAvroSerde));
        final KStream<String, GenericRecord> modifiedErrorEvents = retryableErrors.map(new KeyValueMapper<String, GenericRecord, KeyValue<String, GenericRecord>>() {
            public KeyValue<String, GenericRecord> apply(String key, GenericRecord value) {
                RetryableError retryableError = null;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    retryableError = mapper.readValue(String.valueOf(value), RetryableError.class);
                } catch (JsonProcessingException jsonException) {
                    jsonException.printStackTrace();
                }
                String type = (String) retryableError.getObject();
                KeyValue<String, GenericRecord> retryRecord = new KeyValue<String, GenericRecord>(key, null);
                Schema schema;
                Decoder decoder;
                DatumReader<Object> reader;
                try {
                    switch (type) {
                        case "PizzaDough":
                            schema = PizzaDough.SCHEMA$;
                            decoder = new DecoderFactory().jsonDecoder(schema, (String) retryableError.getContent());
                            reader = new GenericDatumReader<Object>(schema);
                            PizzaDough dough = (PizzaDough) reader.read(null, decoder);
                            retryRecord = new KeyValue<String, GenericRecord>(key, dough);
                            break;
                        case "PizzaSauce":
                            schema = PizzaSauce.SCHEMA$;
                            decoder = new DecoderFactory().jsonDecoder(schema, (String) retryableError.getContent());
                            reader = new GenericDatumReader<Object>(schema);
                            PizzaSauce sauce = (PizzaSauce) reader.read(null, decoder);
                            retryRecord = new KeyValue<String, GenericRecord>(key, sauce);
                            break;
                        case "PizzaTopping":
                            schema = PizzaTopping.SCHEMA$;
                            decoder = new DecoderFactory().jsonDecoder(schema, (String) retryableError.getContent());
                            reader = new GenericDatumReader<Object>(schema);
                            PizzaDough topping = (PizzaDough) reader.read(null, decoder);
                            retryRecord = new KeyValue<String, GenericRecord>(key, topping);
                            break;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return retryRecord;
            }
        });
        final KStream<String, String> tombstones = modifiedErrorEvents.map(new KeyValueMapper<String, GenericRecord, KeyValue<String, String>>() {
            public KeyValue<String, String> apply(String key, GenericRecord value) {
                return new KeyValue<String, String>(key, null);
            }
        });
        modifiedErrorEvents.to(SINK_TOPIC, Produced.with(Serdes.String(), genericAvroSerde));
        tombstones.to(REDIRECT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
        return new KafkaStreams(builder.build(), streamsConfig);
    }

    private GenericAvroSerde createGenericAvroSerde(Properties streamsConfig) {
        GenericAvroSerde genericRetrySerde = new GenericAvroSerde();
        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, streamsConfig.getProperty("schema.registry.url"));
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, streamsConfig.getProperty("basic.auth.credentials.source"));
        serdeConfig.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, streamsConfig.getProperty("basic.auth.user.info"));
        genericRetrySerde.configure(serdeConfig, false);
        return genericRetrySerde;
    }
}
