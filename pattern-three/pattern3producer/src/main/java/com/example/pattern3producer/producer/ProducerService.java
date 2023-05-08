package com.example.pattern3producer.producer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.exceptions.OrderingException;
import com.example.exceptions.doughs.OutOfDoughException;
import com.example.exceptions.doughs.UnavailableDoughException;
import com.example.exceptions.sauces.OutOfSauceException;
import com.example.exceptions.sauces.UnavailableSauceException;
import com.example.exceptions.toppings.IllegalToppingException;
import com.example.exceptions.toppings.UnavailableToppingException;
import com.example.objects.NonretryableError;
import com.example.objects.PizzaDough;
import com.example.objects.PizzaSauce;
import com.example.objects.PizzaTopping;
import com.example.objects.Redirect;
import com.example.objects.RetryableError;
import com.example.pattern3producer.repository.RedirectEntity;
import com.example.pattern3producer.repository.RedirectRepository;
import com.example.pattern3producer.sim.BoobyTrap;
import com.example.pattern3producer.util.CommonUtils;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import jakarta.annotation.PreDestroy;

@Component
public class ProducerService {
    private ArrayList<String> REQUIRED_PROPS = new ArrayList<String>(Arrays.asList("BOOTSTRAP_SERVERS", "KAFKA_KEY", "KAFKA_SECRET", "SCHEMA_REGISTRY_URL", "SCHEMA_REGISTRY_KEY", "SCHEMA_REGISTRY_SECRET"));
    private String clientId = (System.getenv("CLIENT_ID") != null) ? System.getenv("CLIENT_ID") : "pattern-3-producer";
    private String configType = (System.getenv("CONFIG_TYPE") != null) ? System.getenv("CONFIG_TYPE") : "FILE"; 
    private String configFile = (System.getenv("CONFIG") != null) ? System.getenv("CONFIG_FILE") : "producer.properties";
    private String RETRY_APPLICATION_ID = "pattern-3-retrier";
    public static String TOPIC = "pizza-assembly";
    private String DLQ_TOPIC = TOPIC+"-dlq";
    private String RETRY_TOPIC = TOPIC+"-retry";
    private String REDIRECT_TOPIC = TOPIC+"-redirect";
    private KafkaProducer<String, Object> producer;
    public boolean ready = false;
    
    @Autowired
    RedirectRepository retryRepository;

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
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, false);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, clientId);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);    
        props.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, CustomSubjectNameStrategy.class.getName());
        producer = new KafkaProducer<>(props);
        producer.initTransactions();
        ready = true;
    }

    public void send(PizzaDough dough, BoobyTrap boobyTrap) {
        Optional<RedirectEntity> optionalId = retryRepository.findById((String) dough.getPizza());
        if (!optionalId.isPresent()) {
            try {
                producer.beginTransaction();
                boobyTrap.disarm(dough);
                ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(TOPIC, (String) dough.getPizza(), dough);
                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get();
                String message = "Produced '%s' to topic '%s', offset '%s'";
                System.out.println(String.format(message, dough.getClass().getName(), TOPIC, metadata.offset()));
                Map<TopicPartition, OffsetAndMetadata> offsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(TOPIC, metadata.partition()), new OffsetAndMetadata(metadata.offset())); }
                };
                producer.sendOffsetsToTransaction(offsetCommitMap, new ConsumerGroupMetadata("downstream-consumer"));
                producer.commitTransaction();
            } catch (ExecutionException|InterruptedException futureException) {
                futureException.printStackTrace();
                producer.abortTransaction();
            } catch (OutOfDoughException doughException) {
                producer.abortTransaction();
                retryRepository.save(new RedirectEntity((String) dough.getPizza()));
                handleRetryableError(dough, doughException);
            } catch (UnavailableDoughException doughException) {
                producer.abortTransaction();
                handleNonRetryableError(dough, doughException);
            }
        } else {
            String format = "A dependant message for pizza '%s' is staged for retry.";
            handleRetryableError(dough, new OrderingException(String.format(format, dough.getPizza())));
        }
    }
    public void send(PizzaSauce sauce, BoobyTrap boobyTrap) {
        Optional<RedirectEntity> optionalId = retryRepository.findById((String) sauce.getPizza());
        if (!optionalId.isPresent()) {
            try {
                producer.beginTransaction();
                boobyTrap.disarm(sauce);
                ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(TOPIC, (String) sauce.getPizza(), sauce);
                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get();
                String message = "Produced '%s' to topic '%s', offset '%s'";
                System.out.println(String.format(message, sauce.getClass().getName(), TOPIC, metadata.offset()));
                Map<TopicPartition, OffsetAndMetadata> offsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(TOPIC, metadata.partition()), new OffsetAndMetadata(metadata.offset())); }
                };
                producer.sendOffsetsToTransaction(offsetCommitMap, new ConsumerGroupMetadata("downstream-consumer"));
                producer.commitTransaction();
            } catch (ExecutionException|InterruptedException futureException) {
                futureException.printStackTrace();
                producer.abortTransaction();
            } catch (OutOfSauceException sauceException) {
                producer.abortTransaction();
                retryRepository.save(new RedirectEntity((String) sauce.getPizza()));
                handleRetryableError(sauce, sauceException);
            } catch (UnavailableSauceException sauceException) {
                producer.abortTransaction();
                handleNonRetryableError(sauce, sauceException);
            }
        } else {
            String format = "A dependant message for pizza '%s' is staged for retry.";
            handleRetryableError(sauce, new OrderingException(String.format(format, sauce.getPizza())));
        } 
    }
    public void send(PizzaTopping topping, BoobyTrap boobyTrap) {
        Optional<RedirectEntity> optionalId = retryRepository.findById((String) topping.getPizza());
        if (!optionalId.isPresent()) {
            try {
                producer.beginTransaction();
                boobyTrap.disarm(topping);
                ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(TOPIC, (String) topping.getPizza(), topping);
                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get();
                String message = "Produced '%s' to topic '%s', offset '%s'";
                System.out.println(String.format(message, topping.getClass().getName(), TOPIC, metadata.offset()));
                Map<TopicPartition, OffsetAndMetadata> offsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(TOPIC, metadata.partition()), new OffsetAndMetadata(metadata.offset())); }
                };
                producer.sendOffsetsToTransaction(offsetCommitMap, new ConsumerGroupMetadata("downstream-consumer"));
                producer.commitTransaction();
            } catch (ExecutionException|InterruptedException futureException) {
                futureException.printStackTrace();
                producer.abortTransaction();
            } catch (IllegalToppingException toppingException) {
                producer.abortTransaction();
                retryRepository.save(new RedirectEntity((String) topping.getPizza()));
                handleRetryableError(topping, toppingException);
            } catch (UnavailableToppingException toppingException) {
                producer.abortTransaction();
                handleNonRetryableError(topping, toppingException);
            }
        } else {
            String format = "A dependant message for pizza '%s' is staged for retry.";
            handleRetryableError(topping, new OrderingException(String.format(format, topping.getPizza())));
        }
    }
    private void handleRetryableError(PizzaDough unavailablePizzaDough, Exception exception) {
        RetryableError retryableError = new RetryableError();
        retryableError.setException(exception.getClass().getName());
        retryableError.setMessage(exception.getMessage());
        retryableError.setObject(unavailablePizzaDough.getClass().getName());
        retryableError.setContent(unavailablePizzaDough.toString());
        try {
            producer.beginTransaction();
            ProducerRecord<String, Object> retryRecord = new ProducerRecord<String, Object>(RETRY_TOPIC, (String) unavailablePizzaDough.getPizza(), retryableError);
            Future<RecordMetadata> retryFuture = producer.send(retryRecord);
            RecordMetadata retryMetadata = retryFuture.get();
            String retryMessage = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(retryMessage, retryableError.getClass().getName(), RETRY_TOPIC, retryMetadata.offset()));

            ProducerRecord<String, Object> redirectRecord = new ProducerRecord<String, Object>(REDIRECT_TOPIC, (String) unavailablePizzaDough.getPizza(), new Redirect(unavailablePizzaDough.getPizza()));
            Future<RecordMetadata> redirectFuture = producer.send(redirectRecord);
            RecordMetadata redirectMetadata = redirectFuture.get();
            String redirectMessage = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(redirectMessage, (String) unavailablePizzaDough.getPizza(), REDIRECT_TOPIC, redirectMetadata.offset()));
            if (retryMetadata != null && redirectMetadata != null) {
                Map<TopicPartition, OffsetAndMetadata> retryOffsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(RETRY_TOPIC, retryMetadata.partition()), new OffsetAndMetadata(retryMetadata.offset())); }
                };
                producer.sendOffsetsToTransaction(retryOffsetCommitMap, new ConsumerGroupMetadata(RETRY_APPLICATION_ID));
                Map<TopicPartition, OffsetAndMetadata> redirectOffsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(REDIRECT_TOPIC, redirectMetadata.partition()), new OffsetAndMetadata(redirectMetadata.offset())); }
                };
                producer.sendOffsetsToTransaction(redirectOffsetCommitMap, new ConsumerGroupMetadata(RETRY_APPLICATION_ID));
                producer.commitTransaction();
            } else {
                producer.abortTransaction();
            }
        } catch (ExecutionException|InterruptedException futureException) {
            futureException.printStackTrace();
            producer.abortTransaction();
        } catch (ProducerFencedException|OutOfOrderSequenceException|AuthorizationException transactionException) {
            producer.close();
        } catch (KafkaException kafkaException) {
            producer.abortTransaction();
            // TODO can this retry like so?
            // handleNonRetryableError(unavailablePizzaDough, exception);
        }
    }
    private void handleRetryableError(PizzaSauce unavailablePizzaSauce, Exception exception) {
        RetryableError retryableError = new RetryableError();
        retryableError.setException(exception.getClass().getName());
        retryableError.setMessage(exception.getMessage());
        retryableError.setObject(unavailablePizzaSauce.getClass().getName());
        retryableError.setContent(unavailablePizzaSauce.toString());
        try {
            producer.beginTransaction();
            ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(RETRY_TOPIC, (String) unavailablePizzaSauce.getPizza(), retryableError);
            Future<RecordMetadata> retryFuture = producer.send(record);
            RecordMetadata retryMetadata = retryFuture.get();
            String retryMessage = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(retryMessage, retryableError.getClass().getName(), RETRY_TOPIC, retryMetadata.offset()));

            ProducerRecord<String, Object> redirectRecord = new ProducerRecord<String, Object>(REDIRECT_TOPIC, (String) unavailablePizzaSauce.getPizza(), new Redirect(unavailablePizzaSauce.getPizza()));
            Future<RecordMetadata> redirectFuture = producer.send(redirectRecord);
            RecordMetadata redirectMetadata = redirectFuture.get();
            String redirectMessage = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(redirectMessage, (String) unavailablePizzaSauce.getPizza(), REDIRECT_TOPIC, redirectMetadata.offset()));
            if (retryMetadata != null && redirectMetadata != null) {
                Map<TopicPartition, OffsetAndMetadata> retryOffsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(RETRY_TOPIC, retryMetadata.partition()), new OffsetAndMetadata(retryMetadata.offset())); }
                };
                producer.sendOffsetsToTransaction(retryOffsetCommitMap, new ConsumerGroupMetadata(RETRY_APPLICATION_ID));
                Map<TopicPartition, OffsetAndMetadata> redirectOffsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(REDIRECT_TOPIC, redirectMetadata.partition()), new OffsetAndMetadata(redirectMetadata.offset())); }
                };
                producer.sendOffsetsToTransaction(redirectOffsetCommitMap, new ConsumerGroupMetadata(RETRY_APPLICATION_ID));
                producer.commitTransaction();
            } else {
                producer.abortTransaction();
            }
        } catch (ExecutionException|InterruptedException futureException) {
            futureException.printStackTrace();
            producer.abortTransaction();
        } catch (ProducerFencedException|OutOfOrderSequenceException|AuthorizationException transactionException) {
            producer.close();
        } catch (KafkaException kafkaException) {
            producer.abortTransaction();
        }
    }
    private void handleRetryableError(PizzaTopping illegalPizzaTopping, Exception exception) {
        RetryableError retryableError = new RetryableError();
        retryableError.setException(exception.getClass().getName());
        retryableError.setMessage(exception.getMessage());
        retryableError.setObject(illegalPizzaTopping.getClass().getName());
        retryableError.setContent(illegalPizzaTopping.toString());
        try {
            producer.beginTransaction();
            ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(RETRY_TOPIC, (String) illegalPizzaTopping.getPizza(), retryableError);
            Future<RecordMetadata> retryFuture = producer.send(record);
            RecordMetadata retryMetadata = retryFuture.get();
            String mretryMessage = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(mretryMessage, retryableError.getClass().getName(), RETRY_TOPIC, retryMetadata.offset()));

            ProducerRecord<String, Object> redirectRecord = new ProducerRecord<String, Object>(REDIRECT_TOPIC, (String) illegalPizzaTopping.getPizza(), new Redirect(illegalPizzaTopping.getPizza()));
            Future<RecordMetadata> redirectFuture = producer.send(redirectRecord);
            RecordMetadata redirectMetadata = redirectFuture.get();
            String redirectMessage = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(redirectMessage, (String) illegalPizzaTopping.getPizza(), REDIRECT_TOPIC, redirectMetadata.offset()));
            if (retryMetadata != null && redirectMetadata != null) {
                Map<TopicPartition, OffsetAndMetadata> retryOffsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(RETRY_TOPIC, retryMetadata.partition()), new OffsetAndMetadata(retryMetadata.offset())); }
                };
                producer.sendOffsetsToTransaction(retryOffsetCommitMap, new ConsumerGroupMetadata(RETRY_APPLICATION_ID));
                Map<TopicPartition, OffsetAndMetadata> redirectOffsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                    { put(new TopicPartition(REDIRECT_TOPIC, redirectMetadata.partition()), new OffsetAndMetadata(redirectMetadata.offset())); }
                };
                producer.sendOffsetsToTransaction(redirectOffsetCommitMap, new ConsumerGroupMetadata(RETRY_APPLICATION_ID));
                producer.commitTransaction();
            } else {
                producer.abortTransaction();
            }
        } catch (ExecutionException|InterruptedException futureException) {
            futureException.printStackTrace();
            producer.abortTransaction();
        } catch (ProducerFencedException|OutOfOrderSequenceException|AuthorizationException transactionException) {
            producer.close();
        } catch (KafkaException kafkaException) {
            producer.abortTransaction();
        }
    }
    private void handleNonRetryableError(PizzaDough unavailablePizzaDough, Exception exception) {
        NonretryableError nonretryableError = new NonretryableError();
        nonretryableError.setException(exception.getClass().getName());
        nonretryableError.setMessage(exception.getMessage());
        nonretryableError.setObject(unavailablePizzaDough.getClass().toString());
        nonretryableError.setContent(unavailablePizzaDough.toString());
        try {
            producer.beginTransaction();
            ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(DLQ_TOPIC, (String) unavailablePizzaDough.getPizza(), nonretryableError);
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            String message = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(message, nonretryableError.getClass().toString(), DLQ_TOPIC, metadata.offset()));
            Map<TopicPartition, OffsetAndMetadata> offsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                { put(new TopicPartition(DLQ_TOPIC, metadata.partition()), new OffsetAndMetadata(metadata.offset())); }
            };
            producer.sendOffsetsToTransaction(offsetCommitMap, new ConsumerGroupMetadata("downstream-consumer"));
            producer.commitTransaction();
            retryRepository.save(new RedirectEntity((String) unavailablePizzaDough.getPizza()));
        } catch (ExecutionException|InterruptedException futureException) {
            futureException.printStackTrace();
            producer.abortTransaction();
        }
    }
    private void handleNonRetryableError(PizzaSauce unavailablePizzaSauce, Exception exception) {
        NonretryableError nonretryableError = new NonretryableError();
        nonretryableError.setException(exception.getClass().getName());
        nonretryableError.setMessage(exception.getMessage());
        nonretryableError.setObject(unavailablePizzaSauce.getClass().toString());
        nonretryableError.setContent(unavailablePizzaSauce.toString());
        try {
            producer.beginTransaction();
            ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(DLQ_TOPIC, (String) unavailablePizzaSauce.getPizza(), nonretryableError);
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            String message = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(message, nonretryableError.getClass().toString(), DLQ_TOPIC, metadata.offset()));
            Map<TopicPartition, OffsetAndMetadata> offsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                { put(new TopicPartition(DLQ_TOPIC, metadata.partition()), new OffsetAndMetadata(metadata.offset())); }
            };
            producer.sendOffsetsToTransaction(offsetCommitMap, new ConsumerGroupMetadata("downstream-consumer"));
            producer.commitTransaction();
            retryRepository.save(new RedirectEntity((String) unavailablePizzaSauce.getPizza()));
        } catch (ExecutionException|InterruptedException futureException) {
            futureException.printStackTrace();
            producer.abortTransaction();
        }
    }
    private void handleNonRetryableError(PizzaTopping unavailablePizzaTopping, Exception exception) {
        NonretryableError nonretryableError = new NonretryableError();
        nonretryableError.setException(exception.getClass().getName());
        nonretryableError.setMessage(exception.getMessage());
        nonretryableError.setObject(unavailablePizzaTopping.getClass().toString());
        nonretryableError.setContent(unavailablePizzaTopping.toString());
        try {
            producer.beginTransaction();
            ProducerRecord<String, Object> record = new ProducerRecord<String, Object>(DLQ_TOPIC, (String) unavailablePizzaTopping.getPizza(), nonretryableError);
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get();
            String message = "Produced '%s' to topic '%s', offset '%s'";
            System.out.println(String.format(message, nonretryableError.getClass().toString(), DLQ_TOPIC, metadata.offset()));
            Map<TopicPartition, OffsetAndMetadata> offsetCommitMap = new HashMap<TopicPartition, OffsetAndMetadata>() {
                { put(new TopicPartition(DLQ_TOPIC, metadata.partition()), new OffsetAndMetadata(metadata.offset())); }
            };
            producer.sendOffsetsToTransaction(offsetCommitMap, new ConsumerGroupMetadata("downstream-consumer"));
            producer.commitTransaction();
            retryRepository.save(new RedirectEntity((String) unavailablePizzaTopping.getPizza()));
        } catch (ExecutionException|InterruptedException futureException) {
            futureException.printStackTrace();
            producer.abortTransaction();
        }
    }

    @PreDestroy
    private void closeProducer() {
        if (this.producer != null) {
            producer.close();
        }
    }
}
