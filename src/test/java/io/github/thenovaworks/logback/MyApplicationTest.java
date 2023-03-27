package io.github.thenovaworks.logback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class MyApplicationTest {
    final Logger logger = LoggerFactory.getLogger(MyApplicationTest.class);

    @BeforeEach
    public void initTxID() {
        String uuid = UUID.randomUUID().toString();
        MDC.put("txId", uuid);
    }

    @Test
    public void ut1001() {
        logger.debug("This is a check-001.");
        String uuid = UUID.randomUUID().toString();
        try {
            logger.info("This is a check-002.");
            if (uuid != null) {
                logger.warn("This is a check-003.");
                throw new IllegalArgumentException("uuid check");
            }
        } catch (RuntimeException e) {
            logger.error("This is a check-004. ERR: {}", e.getMessage(), e);
        }
    }

    @Test
    public void ut1002() {
        logger.debug("This is a check-101.");
        try {
            logger.info("This is a check-102.");
        } catch (RuntimeException e) {
            logger.error("This is a check-103. ERR: {}", e.getMessage(), e);
        }
    }
}
