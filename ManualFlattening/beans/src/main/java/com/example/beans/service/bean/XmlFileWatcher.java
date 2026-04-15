package com.example.beans.service.bean;

import com.example.beans.job.BeanJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.*;

/**
 * When the file is modified, the system automatically reloads entity definitions into memory
 * without requiring an application restart or manual API call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XmlFileWatcher {

    private final BeanJob beanJob;

    @Value("${app.entities-config.path}")
    private String xmlFilePath;

    //Run this method after Spring Boot fully starts to ensure everything is ready before watching the file
    @EventListener(ApplicationReadyEvent.class)
    public void watchFile() {
        new Thread(() -> {
            try {
                Path file = Paths.get(xmlFilePath);
                //watch the directory, not the file directly
                Path dir = file.getParent();

                WatchService watcher = FileSystems.getDefault().newWatchService();
                //Notify me when any file in this folder is modified
                dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

                log.info("Watching XML file: {}", file);

                //when edits
                while (true) {
                    //waits (blocks) until a file change happens
                    WatchKey key = watcher.take();

                    //Loop through all file change events
                    for (WatchEvent<?> event : key.pollEvents()) {
                        //many files could change in the folder we only care about: entities-config.xml
                        if (event.context().toString().equals(file.getFileName().toString())) {

                            log.info("XML changed → reloading...");
                            Thread.sleep(500); // small delay
                            beanJob.reloadbeans();
                        }
                    }
                //continue listening for future changes
                    key.reset();
                }

            } catch (Exception e) {
                log.error("Watcher error", e);
            }
        }).start();
    }
}