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

import static org.assertj.core.api.Assertions.*;

/**
 * PageInfo 단위 테스트.
 */
@DisplayName("PageInfo 테스트")
class PageInfoTest {

    // ========================================
    // from(Page<?>) 테스트
    // ========================================

    @Nested
    @DisplayName("from(Page<?>) 테스트")
    class FromPageTest {

        @Test
        @DisplayName("Page에서 모든 메타정보를 추출한다")
        void from_extractsAllMetadata() {
            // given
            List<String> content = List.of("a", "b", "c");
            PageRequest pageable = PageRequest.of(2, 10);
            Page<String> page = new PageImpl<>(content, pageable, 53);

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getPage()).isEqualTo(2);
            assertThat(pageInfo.getSize()).isEqualTo(10);
            assertThat(pageInfo.getTotalElements()).isEqualTo(53);
            assertThat(pageInfo.getTotalPages()).isEqualTo(6); // ceil(53/10) = 6
            assertThat(pageInfo.getNumberOfElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("첫 번째 페이지를 올바르게 표시한다")
        void from_firstPage_setsFlags() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10),
                    100
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.isFirst()).isTrue();
            assertThat(pageInfo.isLast()).isFalse();
            assertThat(pageInfo.isHasNext()).isTrue();
            assertThat(pageInfo.isHasPrevious()).isFalse();
        }

        @Test
        @DisplayName("마지막 페이지를 올바르게 표시한다")
        void from_lastPage_setsFlags() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(9, 10),
                    100
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.isFirst()).isFalse();
            assertThat(pageInfo.isLast()).isTrue();
            assertThat(pageInfo.isHasNext()).isFalse();
            assertThat(pageInfo.isHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("빈 페이지를 올바르게 표시한다")
        void from_emptyPage_setsEmptyFlag() {
            // given
            Page<String> page = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 10),
                    0
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.isEmpty()).isTrue();
            assertThat(pageInfo.getNumberOfElements()).isEqualTo(0);
            assertThat(pageInfo.getTotalElements()).isEqualTo(0);
            assertThat(pageInfo.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("단일 페이지(첫 번째이자 마지막)를 올바르게 표시한다")
        void from_singlePage_isFirstAndLast() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a", "b"),
                    PageRequest.of(0, 10),
                    2
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.isFirst()).isTrue();
            assertThat(pageInfo.isLast()).isTrue();
            assertThat(pageInfo.isHasNext()).isFalse();
            assertThat(pageInfo.isHasPrevious()).isFalse();
        }
    }

    // ========================================
    // 정렬 정보 테스트
    // ========================================

    @Nested
    @DisplayName("정렬 정보 테스트")
    class SortInfoTest {

        @Test
        @DisplayName("정렬 정보가 없으면 sort는 null이다")
        void from_unsortedPage_sortIsNull() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10),
                    1
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getSort()).isNull();
        }

        @Test
        @DisplayName("단일 정렬 정보를 추출한다")
        void from_singleSort_extractsSortInfo() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")),
                    1
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getSort()).hasSize(1);
            assertThat(pageInfo.getSort().get(0).getProperty()).isEqualTo("name");
            assertThat(pageInfo.getSort().get(0).getDirection()).isEqualTo(PageInfo.SortInfo.Direction.ASC);
        }

        @Test
        @DisplayName("다중 정렬 정보를 추출한다")
        void from_multipleSort_extractsAllSortInfo() {
            // given
            Sort sort = Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id")
            );
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10, sort),
                    1
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getSort()).hasSize(2);

            assertThat(pageInfo.getSort().get(0).getProperty()).isEqualTo("createdAt");
            assertThat(pageInfo.getSort().get(0).getDirection()).isEqualTo(PageInfo.SortInfo.Direction.DESC);

            assertThat(pageInfo.getSort().get(1).getProperty()).isEqualTo("id");
            assertThat(pageInfo.getSort().get(1).getDirection()).isEqualTo(PageInfo.SortInfo.Direction.ASC);
        }

        @Test
        @DisplayName("ignoreCase 정렬 옵션을 추출한다")
        void from_ignoreCaseSort_extractsIgnoreCase() {
            // given
            Sort sort = Sort.by(Sort.Order.asc("name").ignoreCase());
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10, sort),
                    1
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getSort()).hasSize(1);
            assertThat(pageInfo.getSort().get(0).isIgnoreCase()).isTrue();
        }

        @Test
        @DisplayName("대소문자 구분 정렬 옵션을 추출한다")
        void from_caseSensitiveSort_extractsIgnoreCaseFalse() {
            // given
            Sort sort = Sort.by(Sort.Direction.ASC, "name");
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10, sort),
                    1
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getSort()).hasSize(1);
            assertThat(pageInfo.getSort().get(0).isIgnoreCase()).isFalse();
        }
    }

    // ========================================
    // SortInfo 테스트
    // ========================================

    @Nested
    @DisplayName("SortInfo 테스트")
    class SortInfoClassTest {

        @Test
        @DisplayName("SortInfo를 빌더로 생성한다")
        void builder_createsSortInfo() {
            // when
            PageInfo.SortInfo sortInfo = PageInfo.SortInfo.builder()
                    .property("createdAt")
                    .direction(PageInfo.SortInfo.Direction.DESC)
                    .ignoreCase(false)
                    .build();

            // then
            assertThat(sortInfo.getProperty()).isEqualTo("createdAt");
            assertThat(sortInfo.getDirection()).isEqualTo(PageInfo.SortInfo.Direction.DESC);
            assertThat(sortInfo.isIgnoreCase()).isFalse();
        }

        @Test
        @DisplayName("SortInfo는 Serializable이다")
        void sortInfo_isSerializable() {
            // given
            PageInfo.SortInfo sortInfo = PageInfo.SortInfo.builder()
                    .property("id")
                    .direction(PageInfo.SortInfo.Direction.ASC)
                    .build();

            // then
            assertThat(sortInfo).isInstanceOf(java.io.Serializable.class);
        }

        @Test
        @DisplayName("Direction enum 값을 확인한다")
        void direction_hasCorrectValues() {
            // then
            assertThat(PageInfo.SortInfo.Direction.values())
                    .containsExactly(PageInfo.SortInfo.Direction.ASC, PageInfo.SortInfo.Direction.DESC);
        }
    }

    // ========================================
    // Builder 테스트
    // ========================================

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("빌더로 모든 필드를 설정할 수 있다")
        void builder_setsAllFields() {
            // given
            List<PageInfo.SortInfo> sortList = List.of(
                    PageInfo.SortInfo.builder()
                            .property("name")
                            .direction(PageInfo.SortInfo.Direction.ASC)
                            .build()
            );

            // when
            PageInfo pageInfo = PageInfo.builder()
                    .page(3)
                    .size(20)
                    .totalElements(500)
                    .totalPages(25)
                    .numberOfElements(20)
                    .first(false)
                    .last(false)
                    .hasNext(true)
                    .hasPrevious(true)
                    .empty(false)
                    .sort(sortList)
                    .build();

            // then
            assertThat(pageInfo.getPage()).isEqualTo(3);
            assertThat(pageInfo.getSize()).isEqualTo(20);
            assertThat(pageInfo.getTotalElements()).isEqualTo(500);
            assertThat(pageInfo.getTotalPages()).isEqualTo(25);
            assertThat(pageInfo.getNumberOfElements()).isEqualTo(20);
            assertThat(pageInfo.isFirst()).isFalse();
            assertThat(pageInfo.isLast()).isFalse();
            assertThat(pageInfo.isHasNext()).isTrue();
            assertThat(pageInfo.isHasPrevious()).isTrue();
            assertThat(pageInfo.isEmpty()).isFalse();
            assertThat(pageInfo.getSort()).hasSize(1);
        }

        @Test
        @DisplayName("빌더로 일부 필드만 설정할 수 있다")
        void builder_setsPartialFields() {
            // when
            PageInfo pageInfo = PageInfo.builder()
                    .page(0)
                    .size(10)
                    .totalElements(5)
                    .build();

            // then
            assertThat(pageInfo.getPage()).isEqualTo(0);
            assertThat(pageInfo.getSize()).isEqualTo(10);
            assertThat(pageInfo.getTotalElements()).isEqualTo(5);
        }
    }

    // ========================================
    // Serializable 테스트
    // ========================================

    @Nested
    @DisplayName("Serializable 테스트")
    class SerializableTest {

        @Test
        @DisplayName("PageInfo는 Serializable이다")
        void pageInfo_isSerializable() {
            // given
            Page<String> page = new PageImpl<>(List.of("test"), PageRequest.of(0, 10), 1);
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo).isInstanceOf(java.io.Serializable.class);
        }
    }

    // ========================================
    // 경계값 테스트
    // ========================================

    @Nested
    @DisplayName("경계값 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("매우 큰 totalElements를 처리한다")
        void from_largeTotalElements_handlesCorrectly() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 10),
                    Long.MAX_VALUE
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getTotalElements()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("size가 1인 페이지를 처리한다")
        void from_sizeOne_handlesCorrectly() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a"),
                    PageRequest.of(0, 1),
                    100
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getSize()).isEqualTo(1);
            assertThat(pageInfo.getTotalPages()).isEqualTo(100);
        }

        @Test
        @DisplayName("totalElements가 size로 정확히 나누어 떨어지는 경우")
        void from_exactDivision_calculatesCorrectTotalPages() {
            // given
            Page<String> page = new PageImpl<>(
                    List.of("a", "b", "c", "d", "e"),
                    PageRequest.of(0, 5),
                    50
            );

            // when
            PageInfo pageInfo = PageInfo.from(page);

            // then
            assertThat(pageInfo.getTotalPages()).isEqualTo(10);
        }
    }
}
