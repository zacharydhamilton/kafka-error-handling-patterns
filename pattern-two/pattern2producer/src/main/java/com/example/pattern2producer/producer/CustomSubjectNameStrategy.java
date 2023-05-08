package com.example.pattern2producer.producer;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;

public class CustomSubjectNameStrategy implements SubjectNameStrategy {
    @Override
    public void configure(Map<String, ?> config) {}

    @Override
    public String subjectName(String topic, boolean isKey, ParsedSchema schema) {
        /*
         * If either a DLQ or Retry topic, use RecordNameStrategy and return fully-qualified class name
         * as the subject name
         */
        if (topic.contains("-retry") || topic.contains("-dlq")) {
            if (schema == null) {
                return null;
            }
            return getRecordName(schema, isKey);
        /*
         * If not a DLQ or Retry topic, and topic name is not null, return a subject name corresponding to 
         * TopicNameStrategy 
         */
        } else if (topic != null) {
            return isKey ? topic + "-key" : topic + "-value";
        } 
        return null;
    }
    protected String getRecordName(ParsedSchema schema, boolean isKey) {
        String name = schema.name();
        if (name != null) {
            return name;
        }
        if (isKey) {
            throw new SerializationException("In configuration "
                + AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY + " = "
                + getClass().getName() + ", the message key must only be a record schema");
          } else {
            throw new SerializationException("In configuration "
                + AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY + " = "
                + getClass().getName() + ", the message value must only be a record schema");
          }
    }
}

