package com.citytravel.citytravel.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptPasswordButNeverReturnIt() throws Exception {
        User user = objectMapper.readValue(
                "{\"username\":\"tester\",\"password\":\"secret123\"}", User.class);

        assertThat(user.getPassword()).isEqualTo("secret123");
        assertThat(objectMapper.writeValueAsString(user)).doesNotContain("password", "secret123");
    }
}
