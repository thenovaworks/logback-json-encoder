package io.github.thenovaworks.logback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;

public class MyIngestTest {
    final Logger logger = LoggerFactory.getLogger(MyIngestTest.class);
    final Logger collector = LoggerFactory.getLogger("DataCollector.userdata.ingest");

    @Test
    public void test_ingest_log() {
        logger.info("hello");
        logger.info("world");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final Random rand1 = new Random();
        final Random rand2 = new Random();
        final Random rand3 = new Random();
        final Random rand4 = new Random();
        final Random rand5 = new Random();
        final Random rand6 = new Random();
        for (int i = 1; i <= 200; i++) {
            final String name = Long.toString(Math.abs(rand1.nextLong() % 3656158440062976L), 36);
            try {
                User user = new User();
                user.setId(String.format("I100%d", i));
                user.setName(name);
                user.setBirthday(LocalDate.of(rand2.nextInt(2001 - 1976 + 1) + 1976, rand3.nextInt(12 - 1 + 1) + 1, rand4.nextInt(30 - 1 + 1) + 1));
                user.setHeight(rand5.nextInt(190 - 165 + 1) + 165);
                user.setWeight(rand6.nextInt(100 - 60 + 1) + 60);
                user.setTimestamp(Instant.now().toEpochMilli());
                String row = mapper.writeValueAsString(user);
                collector.trace(row);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}
