package io.github.tickatch.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 페이징 메타데이터.
 *
 * <p>Spring Data Page의 메타 정보를 담는 DTO
 *
 * @author Tickatch
 * @since 0.0.1
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 현재 페이지 번호 (0부터 시작) */
    private final int page;

    /** 페이지 크기 */
    private final int size;

    /** 전체 요소 수 */
    private final long totalElements;

    /** 전체 페이지 수 */
    private final int totalPages;

    /** 현재 페이지의 요소 수 */
    private final int numberOfElements;

    /** 첫 번째 페이지 여부 */
    private final boolean first;

    /** 마지막 페이지 여부 */
    private final boolean last;

    /** 다음 페이지 존재 여부 */
    private final boolean hasNext;

    /** 이전 페이지 존재 여부 */
    private final boolean hasPrevious;

    /** 빈 페이지 여부 */
    private final boolean empty;

    /** 정렬 정보 */
    private final List<SortInfo> sort;

    /**
     * Spring Data Page에서 PageInfo 생성
     */
    public static PageInfo from(org.springframework.data.domain.Page<?> page) {
        List<SortInfo> sortInfos = page.getSort().stream()
                .map(order -> SortInfo.builder()
                        .property(order.getProperty())
                        .direction(order.isAscending() ? SortInfo.Direction.ASC : SortInfo.Direction.DESC)
                        .ignoreCase(order.isIgnoreCase())
                        .build())
                .toList();

        return PageInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .empty(page.isEmpty())
                .sort(sortInfos.isEmpty() ? null : sortInfos)
                .build();
    }

    /**
     * 정렬 정보
     */
    @Getter
    @Builder
    public static class SortInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String property;
        private final Direction direction;
        private final boolean ignoreCase;

        public enum Direction {
            ASC, DESC
        }
    }
}