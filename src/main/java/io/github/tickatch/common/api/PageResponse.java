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
 * <p>목록 조회 시 페이징된 데이터와 메타 정보를 함께 반환한다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * // 동일 타입
 * Page<Ticket> page = ticketRepository.findAll(pageable);
 * PageResponse<Ticket> response = PageResponse.from(page);
 *
 * // Entity → DTO 변환
 * PageResponse<TicketDto> response = PageResponse.from(page, TicketDto::from);
 * }</pre>
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
     * Spring Data Page에서 PageResponse 생성 (동일 타입).
     *
     * @param page Spring Data Page 객체
     * @param <T> 컨텐츠 요소 타입
     * @return PageResponse 인스턴스
     * @throws IllegalArgumentException page가 null인 경우
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        validatePage(page);

        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageInfo(PageInfo.from(page))
                .build();
    }

    /**
     * Spring Data Page에서 PageResponse 생성 (타입 변환).
     *
     * <p>Entity → DTO 변환 시 사용한다.
     *
     * @param page Spring Data Page 객체
     * @param mapper 요소 변환 함수 (예: {@code TicketDto::from})
     * @param <T> 원본 요소 타입
     * @param <R> 변환된 요소 타입
     * @return PageResponse 인스턴스
     * @throws IllegalArgumentException page 또는 mapper가 null인 경우
     */
    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        validatePage(page);
        validateMapper(mapper);

        List<R> content = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<R>builder()
                .content(content)
                .pageInfo(PageInfo.from(page))
                .build();
    }

    /**
     * 직접 생성.
     *
     * @param content 컨텐츠 목록
     * @param pageInfo 페이지 메타 정보
     * @param <T> 컨텐츠 요소 타입
     * @return PageResponse 인스턴스
     * @throws IllegalArgumentException content 또는 pageInfo가 null인 경우
     */
    public static <T> PageResponse<T> of(List<T> content, PageInfo pageInfo) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        if (pageInfo == null) {
            throw new IllegalArgumentException("PageInfo must not be null");
        }

        return PageResponse.<T>builder()
                .content(content)
                .pageInfo(pageInfo)
                .build();
    }

    /**
     * Page 객체 유효성 검증.
     */
    private static void validatePage(Page<?> page) {
        if (page == null) {
            throw new IllegalArgumentException("Page must not be null");
        }
    }

    /**
     * Mapper 함수 유효성 검증.
     */
    private static void validateMapper(Function<?, ?> mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("Mapper must not be null");
        }
    }
}