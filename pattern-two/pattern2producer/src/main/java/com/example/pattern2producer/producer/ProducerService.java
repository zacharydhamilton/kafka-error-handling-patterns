package com.example.pattern2producer.producer;

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

import com.example.objects.NonretryableError;
import com.example.objects.RetryableError;
import com.example.objects.RoundPeg;
import com.example.objects.SquarePeg;
import com.example.pattern2producer.util.CommonUtils;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;

@Service
public class ProducerService {
    private ArrayList<String> REQUIRED_PROPS = new ArrayList<String>(Arrays.asList("BOOTSTRAP_SERVERS", "KAFKA_KEY", "KAFKA_SECRET", "SCHEMA_REGISTRY_URL", "SCHEMA_REGISTRY_KEY", "SCHEMA_REGISTRY_SECRET"));
    private String clientId = (System.getenv("CLIENT_ID") != null) ? System.getenv("CLIENT_ID") : "pattern-2-producer";
    private String configType = (System.getenv("CONFIG_TYPE") != null) ? System.getenv("CONFIG_TYPE") : "FILE"; 
    private String configFile = (System.getenv("CONFIG") != null) ? System.getenv("CONFIG_FILE") : "producer.properties";
    private String TOPIC = "roundpeg";
    private String DLQ_TOPIC = TOPIC+"-dlq";
    private String RETRY_TOPIC = TOPIC+"-retry";
    private String NORMAL_MESSAGE_FORMAT = "Produced '%s' event to topic '%s' in partition '%s' with offset '%s'";
    private String RETRYABLE_ERROR_MESSAGE_FORMAT = "Error producing '%s' event to topic '%s'. This is retryable, sending to retry topic '%s'";
    private String RETRYABLE_MESSAGE_FORMAT = "Produced '%s' event to retry topic '%s' in partition '%s' with offset '%s'";
    private String NONRETRYABLE_ERROR_MESSAGE_FORMAT = "Error producing '%s' event to topic '%s'. This is non-retryable, sending to dlq topic '%s'";
    private String NONRETRYABLE_MESSAGE_FORMAT = "Produced '%s' event to dlq topic '%s' in partition '%s' with offset '%s'";

    private KafkaProducer<String, Object> producer;

    @EventListener(ApplicationStartedEvent.class)
    private void initService() throws ConfigException, IOException {
        Properties props = new Properties();
        if (configType.equals("FILE")) {
            CommonUtils.addPropsFromFile(props, configFile);
        } else {
            CommonUtils.preInitChecks(REQUIRED_PROPS);
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
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, CustomSubjectNameStrategy.class.getName());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);    
        producer = new KafkaProducer<>(props);
        start();
    }

    private void start() {
        for (int i=0; i<10000; i++) {
            UUID uuid = UUID.randomUUID();
            // Non-retryable error
            if (i % 45 == 0) {
                SquarePeg squarePeg = new SquarePeg();
                squarePeg.setSide(5);
                try {
                    ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(TOPIC, uuid.toString(), squarePeg);
                    Future<RecordMetadata> futureRecordMetadata = producer.send(producerRecord);
                    RecordMetadata recordMetadata = futureRecordMetadata.get();
                    System.out.println(String.format(NORMAL_MESSAGE_FORMAT, squarePeg.getClass().getName(), recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
                } catch (ExecutionException|InterruptedException futureException) {
                    futureException.printStackTrace();
                } catch (SerializationException serializationException) {
                    System.out.println(String.format(NONRETRYABLE_ERROR_MESSAGE_FORMAT, squarePeg.getClass().getName(), TOPIC, DLQ_TOPIC));
                    NonretryableError nonretryableError = new NonretryableError();
                    nonretryableError.setException(serializationException.getClass().getName());
                    nonretryableError.setMessage(serializationException.getMessage());
                    nonretryableError.setObject(squarePeg.getClass().getName());
                    nonretryableError.setContent(squarePeg.toString());
                    try {
                        ProducerRecord<String, Object> producerErrorRecord = new ProducerRecord<String, Object>(DLQ_TOPIC, uuid.toString(), nonretryableError);
                        Future<RecordMetadata> futureErrorRecordMetadata = producer.send(producerErrorRecord);
                        RecordMetadata errorRecordMetadata = futureErrorRecordMetadata.get();
                        System.out.println(String.format(NONRETRYABLE_MESSAGE_FORMAT, nonretryableError.getClass().getName(), DLQ_TOPIC, errorRecordMetadata.partition(), errorRecordMetadata.offset()));
                    } catch (ExecutionException|InterruptedException futureExecution) {
                        futureExecution.printStackTrace();
                    }
                }
            // Retryable error
            } else if (i % 10 == 0) {
                SquarePeg squarePeg = new SquarePeg();
                squarePeg.setSide(5);
                try {
                    ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(TOPIC, uuid.toString(), squarePeg);
                    Future<RecordMetadata> futureRecordMetadata = producer.send(producerRecord);
                    RecordMetadata recordMetadata = futureRecordMetadata.get();
                    System.out.println(String.format(NORMAL_MESSAGE_FORMAT, squarePeg.getClass().getName(), recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
                } catch (ExecutionException|InterruptedException exception) {
                    exception.printStackTrace();
                } catch (SerializationException serializationException) {
                    System.out.println(String.format(RETRYABLE_ERROR_MESSAGE_FORMAT, squarePeg.getClass().getName(), TOPIC, RETRY_TOPIC));
                    RetryableError retryableError = new RetryableError();
                    retryableError.setException(serializationException.getClass().getName());
                    retryableError.setMessage(serializationException.getMessage());
                    retryableError.setObject(squarePeg.getClass().getName());
                    retryableError.setContent(squarePeg.toString());
                    try {
                        ProducerRecord<String, Object> producerRetryRecord = new ProducerRecord<String, Object>(RETRY_TOPIC, uuid.toString(), retryableError);
                        Future<RecordMetadata> futureRetryMetadata = producer.send(producerRetryRecord);
                        RecordMetadata retryMetadata = futureRetryMetadata.get();
                        System.out.println(String.format(RETRYABLE_MESSAGE_FORMAT, retryableError.getClass().getName(), RETRY_TOPIC, retryMetadata.partition(), retryMetadata.offset())); 
                    } catch (ExecutionException|InterruptedException futureException) {
                        futureException.printStackTrace();
                    } 
                }
            // Normal events
            } else {
                RoundPeg roundPeg = new RoundPeg();
                roundPeg.setDiameter(5);
                try {
                    ProducerRecord<String, Object> producerRecord = new ProducerRecord<String, Object>(TOPIC, uuid.toString(), roundPeg);
                    Future<RecordMetadata> futureRecordMetadata = producer.send(producerRecord);
                    RecordMetadata recordMetadata = futureRecordMetadata.get();
                    System.out.println(String.format("Produced '%s' event to topic '%s' in partition '%s' with offset '%s'", roundPeg.getClass().getName(), recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
                } catch (ExecutionException|InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }
}
