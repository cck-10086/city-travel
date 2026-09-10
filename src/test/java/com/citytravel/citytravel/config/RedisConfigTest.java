package com.citytravel.citytravel.config;

import com.citytravel.citytravel.entity.Attraction;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void shouldSerializeAndDeserializeDomainListWithJavaTime() {
        RedisTemplate<String, Object> template =
                new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        @SuppressWarnings("unchecked")
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();

        Attraction attraction = new Attraction();
        attraction.setId(1L);
        attraction.setName("麦积山石窟");
        attraction.setPrice(new BigDecimal("80.00"));
        attraction.setCreateTime(LocalDateTime.of(2026, 9, 8, 10, 0));
        byte[] serialized = serializer.serialize(Collections.singletonList(attraction));
        Object restored = serializer.deserialize(serialized);

        assertThat(restored).isInstanceOf(List.class);
        Attraction restoredAttraction = (Attraction) ((List<?>) restored).get(0);
        assertThat(restoredAttraction.getName()).isEqualTo("麦积山石窟");
        assertThat(restoredAttraction.getPrice()).isEqualByComparingTo("80.00");
        assertThat(restoredAttraction.getCreateTime()).isEqualTo(attraction.getCreateTime());
    }
}
