package com.example.dlq.producer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.dlq.objects.RoundPeg;
import com.example.dlq.objects.SquarePeg;
import com.example.dlq.util.CommonUtlis;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

@Service
public class ProducerService {
    private ArrayList<String> REQUIRED_PROPS = new ArrayList<String>(Arrays.asList("BOOTSTRAP_SERVERS", "KAFKA_KEY", "KAFKA_SECRET", "SCHEMA_REGISTRY_URL", "SCHEMA_REGISTRY_KEY", "SCHEMA_REGISTRY_SECRET"));
    private String clientId = (System.getenv("CLIENT_ID") != null) ? System.getenv("CLIENT_ID") : "dlq-producer";
    private String configType = (System.getenv("CONFIG_TYPE") != null) ? System.getenv("CONFIG_TYPE") : "FILE"; 
    private String configFile = (System.getenv("CONFIG") != null) ? System.getenv("CONFIG_FILE") : "producer.properties";
    private String TOPIC = "roundpeg";
    private String DLQ_TOPIC = "roundpeg-dlq";
    private KafkaProducer<String, Object> producer;
    private KafkaProducer<String, String> dlqProducer;

    @EventListener(ApplicationStartedEvent.class)
    private void initService() throws ConfigException, IOException {
        Properties props = new Properties();
        if (configType.equals("FILE")) {
            CommonUtlis.addPropsFromFile(props, configFile);
        } else {
            CommonUtlis.preInitChecks(REQUIRED_PROPS);
            props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("BOOTSTRAP_SERVERS"));
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule   required username='"+System.getenv("KAFKA_KEY")+"'   password='"+System.getenv("KAFKA_SECRET")+"';");
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, System.getenv("SCHEMA_REGISTRY_URL"));
            props.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
            props.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, System.getenv("SCHEMA_REGISTRY_KEY")+":"+System.getenv("SCHEMA_REGISTRY_SECRET"));
        }
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, false);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);    
        producer = new KafkaProducer<>(props);
        Properties dlqProps = props;
        dlqProps.replace(ProducerConfig.CLIENT_ID_CONFIG, clientId+"-errors");
        dlqProps.replace(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        dlqProducer = new KafkaProducer<>(dlqProps);
        start();
    }

    private void start() {
        for (int i=0; i<5; i++) {
            UUID uuid = UUID.randomUUID();
            if (i == 3) {
                SquarePeg squarePeg = new SquarePeg();
                squarePeg.setSide(5);
                try {
                    ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(TOPIC, uuid.toString(), squarePeg);
                    Future<RecordMetadata> futureRecordMetadata = producer.send(producerRecord);
                    RecordMetadata recordMetadata = futureRecordMetadata.get();
                    System.out.println(String.format("Produced 'squareped' event to topic '%s' in partition '%s' with offset '%s'", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
                } catch (ExecutionException|InterruptedException exception) {
                    exception.printStackTrace();
                } catch (SerializationException exception) {
                    System.out.println(String.format("Error producing 'squarepeg' event to topic '%s', sending to dlq topic '%s'", TOPIC, DLQ_TOPIC));
                    try {
                        ProducerRecord<String, String> producerErrorRecord = new ProducerRecord<String, String>(DLQ_TOPIC, uuid.toString(), squarePeg.toString());
                        Future<RecordMetadata> futureErrorRecordMetedata = dlqProducer.send(producerErrorRecord);
                        RecordMetadata errorRecordMetadata = futureErrorRecordMetedata.get();
                        System.out.println(String.format("Produced 'squarepeg' event to topic '%s' in partition '%s' with offset '%s'", errorRecordMetadata.topic(), errorRecordMetadata.partition(), errorRecordMetadata.offset()));
                    } catch (ExecutionException|InterruptedException errorException) {
                        errorException.printStackTrace();
                    }
                }
            } else {
                RoundPeg roundPeg = new RoundPeg();
                roundPeg.setDiameter(5);
                try {
                    ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(TOPIC, uuid.toString(), roundPeg);
                    Future<RecordMetadata> futureRecordMetadata = producer.send(producerRecord);
                    System.out.println(String.format("Produced 'roundpeg' event to offset '%s'", futureRecordMetadata.get().partition()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
