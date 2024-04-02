package app.getxray.xray.junit.customjunitxml;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

public class BasicTestExample {

    @Test
    @Order(1)
    public void someBasicTest() {
    }

    @Test
    @Order(2)
    public void anotherBasicTest() {
    }
}

