package app.getxray.xray.junit.customjunitxml;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

public class SimpleTestExample {

    @Test
    @Order(1)
    public void someSimpleTest() {
    }

    @Test
    @Order(2)
    public void anotherSimpleTest() {
    }
}

