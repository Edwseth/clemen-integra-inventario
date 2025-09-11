package com.willyes.clemenintegra.shared.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateParserTest {

    @Test
    void parseStartAcceptsIsoAndSlashFormats() {
        LocalDateTime iso = DateParser.parseStart("2025-09-10");
        assertEquals(LocalDateTime.of(2025, 9, 10, 0, 0, 0), iso);

        LocalDateTime slash = DateParser.parseStart("10/09/2025");
        assertEquals(LocalDateTime.of(2025, 9, 10, 0, 0, 0), slash);
    }
}
