package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.entity.Strategy;
import com.citytravel.citytravel.entity.User;
import com.citytravel.citytravel.service.StrategyService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrategyControllerTest {

    @Test
    void shouldHidePendingStrategyFromAnonymousUser() {
        StrategyService service = mock(StrategyService.class);
        StrategyController controller = new StrategyController();
        ReflectionTestUtils.setField(controller, "strategyService", service);
        Strategy pending = new Strategy();
        pending.setId(5L);
        pending.setStatus(0);
        pending.setAuthorId(1L);
        when(service.getById(5L)).thenReturn(pending);

        Result<Strategy> result = controller.getById(5L, null);

        assertThat(result.getCode()).isEqualTo(404);
        verify(service, never()).incrementViews(5L);
    }

    @Test
    void authorUpdateShouldPreserveOwnershipAndResetReviewStatus() {
        StrategyService service = mock(StrategyService.class);
        StrategyController controller = new StrategyController();
        ReflectionTestUtils.setField(controller, "strategyService", service);

        Strategy existing = new Strategy();
        existing.setId(5L);
        existing.setAuthorId(1L);
        existing.setAuthorName("原作者");
        existing.setStatus(1);
        existing.setViews(20);
        existing.setLikes(3);
        when(service.getById(5L)).thenReturn(existing);

        User author = new User();
        author.setId(1L);
        author.setRole("tourist");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", author);

        Strategy submitted = new Strategy();
        submitted.setTitle("修改后的标题");
        submitted.setContent("修改后的内容");
        submitted.setAuthorId(999L);
        submitted.setStatus(1);
        submitted.setViews(999);
        submitted.setLikes(999);

        controller.update(5L, submitted, session);

        assertThat(submitted.getAuthorId()).isEqualTo(1L);
        assertThat(submitted.getAuthorName()).isEqualTo("原作者");
        assertThat(submitted.getStatus()).isEqualTo(0);
        assertThat(submitted.getViews()).isEqualTo(20);
        assertThat(submitted.getLikes()).isEqualTo(3);
        verify(service).updateById(submitted);
    }
}
