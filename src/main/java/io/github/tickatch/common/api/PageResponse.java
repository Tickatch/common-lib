package io.github.tickatch.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 페이징 응답.
 *
 * <p>목록 조회 시 페이징된 데이터와 메타 정보를 함께 반환
 *
 * @param <T> 컨텐츠 요소 타입
 * @author Tickatch
 * @since 0.0.1
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 페이지 컨텐츠 */
    private final List<T> content;

    /** 페이지 메타 정보 */
    private final PageInfo pageInfo;

    /**
     * Spring Data Page에서 PageResponse 생성 (동일 타입)
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageInfo(PageInfo.from(page))
                .build();
    }

    /**
     * Spring Data Page에서 PageResponse 생성 (타입 변환)
     * Entity → DTO 변환 시 사용
     */
    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        List<R> content = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<R>builder()
                .content(content)
                .pageInfo(PageInfo.from(page))
                .build();
    }

    /**
     * 직접 생성
     */
    public static <T> PageResponse<T> of(List<T> content, PageInfo pageInfo) {
        return PageResponse.<T>builder()
                .content(content)
                .pageInfo(pageInfo)
                .build();
    }
}