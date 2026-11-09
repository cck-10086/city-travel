package com.citytravel.citytravel.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("分页参数收敛")
class PageUtilTest {

    @Test
    @DisplayName("页码小于 1 时回落到第 1 页")
    void shouldFallbackPageNum() {
        assertThat(PageUtil.pageNum(null)).isEqualTo(1);
        assertThat(PageUtil.pageNum(0)).isEqualTo(1);
        assertThat(PageUtil.pageNum(-5)).isEqualTo(1);
        assertThat(PageUtil.pageNum(3)).isEqualTo(3);
    }

    @Test
    @DisplayName("每页条数被限制在 1~100，防止一次拉全表")
    void shouldClampPageSize() {
        assertThat(PageUtil.pageSize(null)).isEqualTo(PageUtil.DEFAULT_PAGE_SIZE);
        assertThat(PageUtil.pageSize(0)).isEqualTo(PageUtil.DEFAULT_PAGE_SIZE);
        assertThat(PageUtil.pageSize(10)).isEqualTo(10);
        assertThat(PageUtil.pageSize(100)).isEqualTo(100);
        // 关键用例：请求 10 万条必须被压到上限
        assertThat(PageUtil.pageSize(100000)).isEqualTo(PageUtil.MAX_PAGE_SIZE);
        assertThat(PageUtil.pageSize(Integer.MAX_VALUE)).isEqualTo(PageUtil.MAX_PAGE_SIZE);
    }
}
