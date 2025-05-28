package com.willyes.clemenintegra.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {

    public static String asJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
