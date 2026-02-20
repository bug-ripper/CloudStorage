package com.denisbondd111.storageservice.kafka;

import com.denisbondd111.common.constant.KafkaTopics;
import com.denisbondd111.common.event.FileDeletedEvent;
import com.denisbondd111.common.event.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void fileUploaded(FileUploadedEvent event) {
        kafkaTemplate.send(KafkaTopics.FILE_UPLOADED, event.fileId(), event);
    }

    public void fileDeleted(FileDeletedEvent event) {
        kafkaTemplate.send(KafkaTopics.FILE_DELETED, event.fileId(), event);
    }
}
