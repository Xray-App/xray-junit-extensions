package com.idera.xray.junit.customjunitxml;

import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import com.idera.xray.junit.customjunitxml.annotations.TestRail;

public class TRailEnabledTestExamples {

    @Test
    @TestRail(id = "123")
    public void annotatedTestRailWithCustomId() {
        fail("this should have id: 123");
    }

    @Test
    @TestRail(id = "C123")
    public void annotatedTestRailWithCustomIdWithPrefix() {
        fail("this should have id: 123");
    }

}
