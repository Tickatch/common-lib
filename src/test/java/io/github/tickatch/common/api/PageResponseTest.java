package io.github.tickatch.common.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

/**
 * PageResponse 단위 테스트.
 */
@DisplayName("PageResponse 테스트")
class PageResponseTest {

    // ========================================
    // from(Page<T>) 테스트
    // ========================================

    @Nested
    @DisplayName("from(Page<T>) 테스트")
    class FromPageTest {

        @Test
        @DisplayName("Page에서 PageResponse를 생성한다")
        void from_createsPageResponse() {
            // given
            List<TestEntity> content = List.of(
                    new TestEntity(1L, "첫번째"),
                    new TestEntity(2L, "두번째")
            );
            PageRequest pageable = PageRequest.of(0, 10);
            Page<TestEntity> page = new PageImpl<>(content, pageable, 100);

            // when
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getPageInfo()).isNotNull();
            assertThat(response.getPageInfo().getPage()).isEqualTo(0);
            assertThat(response.getPageInfo().getSize()).isEqualTo(10);
            assertThat(response.getPageInfo().getTotalElements()).isEqualTo(100);
        }

        @Test
        @DisplayName("빈 Page에서 PageResponse를 생성한다")
        void from_withEmptyPage_createsPageResponse() {
            // given
            Page<TestEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

            // when
            PageResponse<TestEntity> response = PageResponse.from(emptyPage);

            // then
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getPageInfo().isEmpty()).isTrue();
            assertThat(response.getPageInfo().getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("null Page는 예외를 발생시킨다")
        void from_withNullPage_throwsException() {
            // when & then
            assertThatThrownBy(() -> PageResponse.from(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Page must not be null");
        }

        @Test
        @DisplayName("정렬 정보가 포함된 Page를 변환한다")
        void from_withSortedPage_includesSortInfo() {
            // given
            List<TestEntity> content = List.of(new TestEntity(1L, "테스트"));
            PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<TestEntity> page = new PageImpl<>(content, pageable, 1);

            // when
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response.getPageInfo().getSort()).isNotNull();
            assertThat(response.getPageInfo().getSort()).hasSize(1);
            assertThat(response.getPageInfo().getSort().get(0).getProperty()).isEqualTo("id");
            assertThat(response.getPageInfo().getSort().get(0).getDirection())
                    .isEqualTo(PageInfo.SortInfo.Direction.DESC);
        }
    }

    // ========================================
    // from(Page<T>, Function) 테스트
    // ========================================

    @Nested
    @DisplayName("from(Page<T>, Function) 테스트")
    class FromPageWithMapperTest {

        @Test
        @DisplayName("mapper를 사용하여 타입을 변환한다")
        void from_withMapper_transformsContent() {
            // given
            List<TestEntity> content = List.of(
                    new TestEntity(1L, "첫번째"),
                    new TestEntity(2L, "두번째")
            );
            Page<TestEntity> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);
            Function<TestEntity, TestDto> mapper = e -> new TestDto(e.id(), e.name().toUpperCase());

            // when
            PageResponse<TestDto> response = PageResponse.from(page, mapper);

            // then
            assertThat(response.getContent())
                    .hasSize(2)
                    .extracting(TestDto::name)
                    .containsExactly("첫번째".toUpperCase(), "두번째".toUpperCase());
        }

        @Test
        @DisplayName("빈 Page와 mapper로 변환한다")
        void from_withEmptyPageAndMapper_createsEmptyResponse() {
            // given
            Page<TestEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            Function<TestEntity, TestDto> mapper = e -> new TestDto(e.id(), e.name());

            // when
            PageResponse<TestDto> response = PageResponse.from(emptyPage, mapper);

            // then
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("null Page는 예외를 발생시킨다")
        void from_withNullPageAndMapper_throwsException() {
            // given
            Function<TestEntity, TestDto> mapper = e -> new TestDto(e.id(), e.name());

            // when & then
            assertThatThrownBy(() -> PageResponse.from(null, mapper))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Page must not be null");
        }

        @Test
        @DisplayName("null mapper는 예외를 발생시킨다")
        void from_withNullMapper_throwsException() {
            // given
            Page<TestEntity> page = new PageImpl<>(List.of(new TestEntity(1L, "테스트")));

            // when & then
            assertThatThrownBy(() -> PageResponse.from(page, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mapper must not be null");
        }

        @Test
        @DisplayName("PageInfo는 변환 후에도 동일하다")
        void from_withMapper_preservesPageInfo() {
            // given
            List<TestEntity> content = List.of(new TestEntity(1L, "테스트"));
            PageRequest pageable = PageRequest.of(2, 5);
            Page<TestEntity> page = new PageImpl<>(content, pageable, 50);
            Function<TestEntity, TestDto> mapper = e -> new TestDto(e.id(), e.name());

            // when
            PageResponse<TestDto> response = PageResponse.from(page, mapper);

            // then
            assertThat(response.getPageInfo().getPage()).isEqualTo(2);
            assertThat(response.getPageInfo().getSize()).isEqualTo(5);
            assertThat(response.getPageInfo().getTotalElements()).isEqualTo(50);
            assertThat(response.getPageInfo().getTotalPages()).isEqualTo(10);
        }
    }

    // ========================================
    // of() 테스트
    // ========================================

    @Nested
    @DisplayName("of() 테스트")
    class OfTest {

        @Test
        @DisplayName("content와 pageInfo로 직접 생성한다")
        void of_createsPageResponse() {
            // given
            List<TestDto> content = List.of(new TestDto(1L, "테스트"));
            PageInfo pageInfo = PageInfo.builder()
                    .page(0)
                    .size(10)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            // when
            PageResponse<TestDto> response = PageResponse.of(content, pageInfo);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getPageInfo()).isEqualTo(pageInfo);
        }

        @Test
        @DisplayName("null content는 예외를 발생시킨다")
        void of_withNullContent_throwsException() {
            // given
            PageInfo pageInfo = PageInfo.builder().page(0).size(10).build();

            // when & then
            assertThatThrownBy(() -> PageResponse.of(null, pageInfo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content must not be null");
        }

        @Test
        @DisplayName("null pageInfo는 예외를 발생시킨다")
        void of_withNullPageInfo_throwsException() {
            // given
            List<TestDto> content = List.of();

            // when & then
            assertThatThrownBy(() -> PageResponse.of(content, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PageInfo must not be null");
        }

        @Test
        @DisplayName("빈 content로 생성할 수 있다")
        void of_withEmptyContent_succeeds() {
            // given
            List<TestDto> content = List.of();
            PageInfo pageInfo = PageInfo.builder()
                    .page(0)
                    .size(10)
                    .totalElements(0)
                    .empty(true)
                    .build();

            // when
            PageResponse<TestDto> response = PageResponse.of(content, pageInfo);

            // then
            assertThat(response.getContent()).isEmpty();
        }
    }

    // ========================================
    // Serializable 테스트
    // ========================================

    @Nested
    @DisplayName("Serializable 테스트")
    class SerializableTest {

        @Test
        @DisplayName("PageResponse는 Serializable이다")
        void pageResponse_isSerializable() {
            // given
            Page<TestEntity> page = new PageImpl<>(List.of(new TestEntity(1L, "테스트")));
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================
    // 페이지 메타정보 테스트
    // ========================================

    @Nested
    @DisplayName("PageInfo 통합 테스트")
    class PageInfoIntegrationTest {

        @Test
        @DisplayName("첫 번째 페이지의 메타정보를 올바르게 설정한다")
        void firstPage_hasCorrectMetadata() {
            // given
            List<TestEntity> content = List.of(new TestEntity(1L, "테스트"));
            Page<TestEntity> page = new PageImpl<>(content, PageRequest.of(0, 10), 100);

            // when
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response.getPageInfo().isFirst()).isTrue();
            assertThat(response.getPageInfo().isLast()).isFalse();
            assertThat(response.getPageInfo().isHasNext()).isTrue();
            assertThat(response.getPageInfo().isHasPrevious()).isFalse();
        }

        @Test
        @DisplayName("마지막 페이지의 메타정보를 올바르게 설정한다")
        void lastPage_hasCorrectMetadata() {
            // given
            List<TestEntity> content = List.of(new TestEntity(1L, "테스트"));
            Page<TestEntity> page = new PageImpl<>(content, PageRequest.of(9, 10), 100);

            // when
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response.getPageInfo().isFirst()).isFalse();
            assertThat(response.getPageInfo().isLast()).isTrue();
            assertThat(response.getPageInfo().isHasNext()).isFalse();
            assertThat(response.getPageInfo().isHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("중간 페이지의 메타정보를 올바르게 설정한다")
        void middlePage_hasCorrectMetadata() {
            // given
            List<TestEntity> content = List.of(new TestEntity(1L, "테스트"));
            Page<TestEntity> page = new PageImpl<>(content, PageRequest.of(5, 10), 100);

            // when
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response.getPageInfo().isFirst()).isFalse();
            assertThat(response.getPageInfo().isLast()).isFalse();
            assertThat(response.getPageInfo().isHasNext()).isTrue();
            assertThat(response.getPageInfo().isHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("numberOfElements를 올바르게 계산한다")
        void numberOfElements_isCorrect() {
            // given
            List<TestEntity> content = List.of(
                    new TestEntity(1L, "1"),
                    new TestEntity(2L, "2"),
                    new TestEntity(3L, "3")
            );
            Page<TestEntity> page = new PageImpl<>(content, PageRequest.of(0, 10), 3);

            // when
            PageResponse<TestEntity> response = PageResponse.from(page);

            // then
            assertThat(response.getPageInfo().getNumberOfElements()).isEqualTo(3);
        }
    }

    // ========================================
    // 테스트용 DTO
    // ========================================

    record TestEntity(Long id, String name) {}
    record TestDto(Long id, String name) {}
}