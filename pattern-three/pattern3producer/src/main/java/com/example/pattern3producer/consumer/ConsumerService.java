package com.example.pattern3producer.consumer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.objects.Redirect;
import com.example.pattern3producer.producer.ProducerService;
import com.example.pattern3producer.repository.RedirectEntity;
import com.example.pattern3producer.repository.RedirectRepository;
import com.example.pattern3producer.util.CommonUtils;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import jakarta.annotation.PreDestroy;

@Component
public class ConsumerService {
    private ArrayList<String> REQUIRED_PROPS = new ArrayList<String>(Arrays.asList("BOOTSTRAP_SERVERS", "KAFKA_KEY", "KAFKA_SECRET", "SCHEMA_REGISTRY_URL", "SCHEMA_REGISTRY_KEY", "SCHEMA_REGISTRY_SECRET"));
    private String clientId = (System.getenv("CLIENT_ID") != null) ? System.getenv("CLIENT_ID") : "pattern-3-consumer";
    private String configType = (System.getenv("CONFIG_TYPE") != null) ? System.getenv("CONFIG_TYPE") : "FILE"; 
    private String configFile = (System.getenv("CONFIG") != null) ? System.getenv("CONFIG_FILE") : "producer.properties";
    private String TOPIC = ProducerService.TOPIC+"-redirect";
    private KafkaConsumer<String, Redirect> consumer;
    public boolean ready = false;

    @Autowired
    RedirectRepository repository;

    public void initService() throws ConfigException, IOException {
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
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, clientId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // TODO Change this back to earliest
        consumer = new KafkaConsumer<>(props);
        ready = true;
    }

    public void start() {
        consumer.subscribe(Arrays.asList(TOPIC));
        try {
            while (true) {
                ConsumerRecords<String, Redirect> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Redirect> record: records) {
                    String message = "Consumed event from topic '%s' with key '%s' and value '%s'";
                    System.out.println(String.format(message, TOPIC, record.key(), record.value()));
                    if (record.value() == null) {
                        System.out.println(String.format("Releasing message with key %s from in-memory db", record.key()));
                        repository.delete(new RedirectEntity(record.key()));
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            consumer.close();
        }
    }
    @PreDestroy
    private void closeConsumer() {
        if (this.consumer != null) {
            consumer.close();
        }
    }
}
