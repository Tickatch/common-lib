package io.github.tickatch.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tickatch.common.util.JsonUtils.JsonConversionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * JsonUtils 단위 테스트.
 */
@DisplayName("JsonUtils 테스트")
class JsonUtilsTest {

    // ========================================
    // 테스트용 DTO
    // ========================================

    record TestDto(Long id, String name, LocalDateTime createdAt) {}

    record NestedDto(Long id, TestDto nested) {}

    // ========================================
    // ObjectMapper 테스트
    // ========================================

    @Nested
    @DisplayName("getObjectMapper() 테스트")
    class GetObjectMapperTest {

        @Test
        @DisplayName("ObjectMapper 인스턴스를 반환한다")
        void getObjectMapper_returnsInstance() {
            // when
            ObjectMapper mapper = JsonUtils.getObjectMapper();

            // then
            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("동일한 ObjectMapper 인스턴스를 반환한다")
        void getObjectMapper_returnsSameInstance() {
            // when
            ObjectMapper mapper1 = JsonUtils.getObjectMapper();
            ObjectMapper mapper2 = JsonUtils.getObjectMapper();

            // then
            assertThat(mapper1).isSameAs(mapper2);
        }
    }

    // ========================================
    // Object → JSON 변환 테스트
    // ========================================

    @Nested
    @DisplayName("toJson() 테스트")
    class ToJsonTest {

        @Test
        @DisplayName("객체를 JSON 문자열로 변환한다")
        void toJson_convertsObjectToJson() {
            // given
            TestDto dto = new TestDto(1L, "테스트", null);

            // when
            String json = JsonUtils.toJson(dto);

            // then
            assertThat(json).isEqualTo("{\"id\":1,\"name\":\"테스트\"}");
        }

        @Test
        @DisplayName("null 값은 JSON에서 제외된다")
        void toJson_excludesNullValues() {
            // given
            TestDto dto = new TestDto(1L, null, null);

            // when
            String json = JsonUtils.toJson(dto);

            // then
            assertThat(json)
                    .contains("\"id\":1")
                    .doesNotContain("name")
                    .doesNotContain("createdAt");
        }

        @Test
        @DisplayName("LocalDateTime을 ISO 형식으로 직렬화한다")
        void toJson_serializesLocalDateTimeAsIso() {
            // given
            LocalDateTime dateTime = LocalDateTime.of(2025, 1, 15, 10, 30, 0);
            TestDto dto = new TestDto(1L, "테스트", dateTime);

            // when
            String json = JsonUtils.toJson(dto);

            // then
            assertThat(json).contains("\"createdAt\":\"2025-01-15T10:30:00\"");
        }

        @Test
        @DisplayName("컬렉션을 JSON 배열로 변환한다")
        void toJson_convertsCollectionToArray() {
            // given
            List<TestDto> list = List.of(
                    new TestDto(1L, "첫번째", null),
                    new TestDto(2L, "두번째", null)
            );

            // when
            String json = JsonUtils.toJson(list);

            // then
            assertThat(json)
                    .startsWith("[")
                    .endsWith("]")
                    .contains("\"id\":1")
                    .contains("\"id\":2");
        }

        @Test
        @DisplayName("Map을 JSON 객체로 변환한다")
        void toJson_convertsMapToObject() {
            // given
            Map<String, Object> map = Map.of("key1", "value1", "key2", 123);

            // when
            String json = JsonUtils.toJson(map);

            // then
            assertThat(json)
                    .contains("\"key1\":\"value1\"")
                    .contains("\"key2\":123");
        }

        @Test
        @DisplayName("null 객체는 \"null\"로 변환된다")
        void toJson_withNull_returnsNullString() {
            // when
            String json = JsonUtils.toJson(null);

            // then
            assertThat(json).isEqualTo("null");
        }

        @Test
        @DisplayName("중첩 객체를 올바르게 변환한다")
        void toJson_convertsNestedObject() {
            // given
            NestedDto nested = new NestedDto(1L, new TestDto(2L, "내부", null));

            // when
            String json = JsonUtils.toJson(nested);

            // then
            assertThat(json).contains("\"nested\":{\"id\":2,\"name\":\"내부\"}");
        }
    }

    @Nested
    @DisplayName("toJsonSafe() 테스트")
    class ToJsonSafeTest {

        @Test
        @DisplayName("정상적인 객체는 Optional에 JSON을 담아 반환한다")
        void toJsonSafe_withValidObject_returnsOptionalWithJson() {
            // given
            TestDto dto = new TestDto(1L, "테스트", null);

            // when
            Optional<String> result = JsonUtils.toJsonSafe(dto);

            // then
            assertThat(result)
                    .isPresent()
                    .hasValue("{\"id\":1,\"name\":\"테스트\"}");
        }

        @Test
        @DisplayName("null 객체는 Optional.of(\"null\")을 반환한다")
        void toJsonSafe_withNull_returnsOptionalWithNullString() {
            // when
            Optional<String> result = JsonUtils.toJsonSafe(null);

            // then
            assertThat(result).isPresent().hasValue("null");
        }
    }

    @Nested
    @DisplayName("toPrettyJson() 테스트")
    class ToPrettyJsonTest {

        @Test
        @DisplayName("객체를 들여쓰기된 JSON으로 변환한다")
        void toPrettyJson_returnsFormattedJson() {
            // given
            TestDto dto = new TestDto(1L, "테스트", null);

            // when
            String prettyJson = JsonUtils.toPrettyJson(dto);

            // then
            assertThat(prettyJson)
                    .contains("\n")
                    .contains("  "); // 들여쓰기 확인
        }
    }

    @Nested
    @DisplayName("toBytes() 테스트")
    class ToBytesTest {

        @Test
        @DisplayName("객체를 byte 배열로 변환한다")
        void toBytes_convertsToByteArray() {
            // given
            TestDto dto = new TestDto(1L, "테스트", null);

            // when
            byte[] bytes = JsonUtils.toBytes(dto);

            // then
            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes)).isEqualTo("{\"id\":1,\"name\":\"테스트\"}");
        }
    }

    // ========================================
    // JSON → Object 변환 테스트
    // ========================================

    @Nested
    @DisplayName("fromJson(String, Class) 테스트")
    class FromJsonWithClassTest {

        @Test
        @DisplayName("JSON을 객체로 변환한다")
        void fromJson_convertsJsonToObject() {
            // given
            String json = "{\"id\":1,\"name\":\"테스트\"}";

            // when
            TestDto dto = JsonUtils.fromJson(json, TestDto.class);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("ISO 형식 날짜를 LocalDateTime으로 역직렬화한다")
        void fromJson_deserializesIsoDateToLocalDateTime() {
            // given
            String json = "{\"id\":1,\"name\":\"테스트\",\"createdAt\":\"2025-01-15T10:30:00\"}";

            // when
            TestDto dto = JsonUtils.fromJson(json, TestDto.class);

            // then
            assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 30, 0));
        }

        @Test
        @DisplayName("알 수 없는 필드가 있어도 에러 없이 무시한다")
        void fromJson_ignoresUnknownProperties() {
            // given
            String json = "{\"id\":1,\"name\":\"테스트\",\"unknownField\":\"value\"}";

            // when
            TestDto dto = JsonUtils.fromJson(json, TestDto.class);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("잘못된 JSON 형식은 예외를 발생시킨다")
        void fromJson_withInvalidJson_throwsException() {
            // given
            String invalidJson = "{invalid json}";

            // when & then
            assertThatThrownBy(() -> JsonUtils.fromJson(invalidJson, TestDto.class))
                    .isInstanceOf(JsonConversionException.class)
                    .hasMessageContaining("JSON 역직렬화에 실패");
        }
    }

    @Nested
    @DisplayName("fromJsonSafe() 테스트")
    class FromJsonSafeTest {

        @Test
        @DisplayName("정상적인 JSON은 Optional에 객체를 담아 반환한다")
        void fromJsonSafe_withValidJson_returnsOptionalWithObject() {
            // given
            String json = "{\"id\":1,\"name\":\"테스트\"}";

            // when
            Optional<TestDto> result = JsonUtils.fromJsonSafe(json, TestDto.class);

            // then
            assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(TestDto::id, TestDto::name)
                    .containsExactly(1L, "테스트");
        }

        @Test
        @DisplayName("잘못된 JSON은 빈 Optional을 반환한다")
        void fromJsonSafe_withInvalidJson_returnsEmptyOptional() {
            // given
            String invalidJson = "{invalid}";

            // when
            Optional<TestDto> result = JsonUtils.fromJsonSafe(invalidJson, TestDto.class);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("fromJson(String, TypeReference) 테스트")
    class FromJsonWithTypeReferenceTest {

        @Test
        @DisplayName("제네릭 타입을 포함한 JSON을 변환한다")
        void fromJson_withTypeReference_convertsGenericType() {
            // given
            String json = "[{\"id\":1,\"name\":\"첫번째\"},{\"id\":2,\"name\":\"두번째\"}]";

            // when
            List<TestDto> list = JsonUtils.fromJson(json, new TypeReference<>() {});

            // then
            assertThat(list).hasSize(2);
            assertThat(list.get(0).id()).isEqualTo(1L);
            assertThat(list.get(1).id()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("fromBytes() 테스트")
    class FromBytesTest {

        @Test
        @DisplayName("byte 배열을 객체로 변환한다")
        void fromBytes_convertsBytesToObject() {
            // given
            byte[] bytes = "{\"id\":1,\"name\":\"테스트\"}".getBytes();

            // when
            TestDto dto = JsonUtils.fromBytes(bytes, TestDto.class);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("잘못된 byte 배열은 예외를 발생시킨다")
        void fromBytes_withInvalidBytes_throwsException() {
            // given
            byte[] invalidBytes = "invalid".getBytes();

            // when & then
            assertThatThrownBy(() -> JsonUtils.fromBytes(invalidBytes, TestDto.class))
                    .isInstanceOf(JsonConversionException.class);
        }
    }

    // ========================================
    // 편의 메서드 테스트
    // ========================================

    @Nested
    @DisplayName("fromJsonToList() 테스트")
    class FromJsonToListTest {

        @Test
        @DisplayName("JSON 배열을 List로 변환한다")
        void fromJsonToList_convertsJsonArrayToList() {
            // given
            String json = "[{\"id\":1,\"name\":\"첫번째\"},{\"id\":2,\"name\":\"두번째\"}]";

            // when
            List<TestDto> list = JsonUtils.fromJsonToList(json, TestDto.class);

            // then
            assertThat(list)
                    .hasSize(2)
                    .extracting(TestDto::name)
                    .containsExactly("첫번째", "두번째");
        }

        @Test
        @DisplayName("빈 배열은 빈 List를 반환한다")
        void fromJsonToList_withEmptyArray_returnsEmptyList() {
            // given
            String json = "[]";

            // when
            List<TestDto> list = JsonUtils.fromJsonToList(json, TestDto.class);

            // then
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("잘못된 JSON은 예외를 발생시킨다")
        void fromJsonToList_withInvalidJson_throwsException() {
            // given
            String invalidJson = "not an array";

            // when & then
            assertThatThrownBy(() -> JsonUtils.fromJsonToList(invalidJson, TestDto.class))
                    .isInstanceOf(JsonConversionException.class);
        }
    }

    @Nested
    @DisplayName("fromJsonToMap() 테스트")
    class FromJsonToMapTest {

        @Test
        @DisplayName("JSON을 Map으로 변환한다")
        void fromJsonToMap_convertsJsonToMap() {
            // given
            String json = "{\"key1\":\"value1\",\"key2\":123,\"key3\":true}";

            // when
            Map<String, Object> map = JsonUtils.fromJsonToMap(json);

            // then
            assertThat(map)
                    .hasSize(3)
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", 123)
                    .containsEntry("key3", true);
        }

        @Test
        @DisplayName("중첩된 JSON도 Map으로 변환한다")
        void fromJsonToMap_convertsNestedJson() {
            // given
            String json = "{\"outer\":{\"inner\":\"value\"}}";

            // when
            Map<String, Object> map = JsonUtils.fromJsonToMap(json);

            // then
            assertThat(map).containsKey("outer");
            assertThat(map.get("outer")).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("잘못된 JSON은 예외를 발생시킨다")
        void fromJsonToMap_withInvalidJson_throwsException() {
            // given
            String invalidJson = "not a json object";

            // when & then
            assertThatThrownBy(() -> JsonUtils.fromJsonToMap(invalidJson))
                    .isInstanceOf(JsonConversionException.class);
        }
    }

    @Nested
    @DisplayName("parseJson() 테스트")
    class ParseJsonTest {

        @Test
        @DisplayName("JSON을 JsonNode로 파싱한다")
        void parseJson_returnsJsonNode() {
            // given
            String json = "{\"id\":1,\"name\":\"테스트\"}";

            // when
            JsonNode node = JsonUtils.parseJson(json);

            // then
            assertThat(node.get("id").asLong()).isEqualTo(1L);
            assertThat(node.get("name").asText()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("잘못된 JSON은 예외를 발생시킨다")
        void parseJson_withInvalidJson_throwsException() {
            // given
            String invalidJson = "{invalid}";

            // when & then
            assertThatThrownBy(() -> JsonUtils.parseJson(invalidJson))
                    .isInstanceOf(JsonConversionException.class)
                    .hasMessageContaining("JSON 파싱에 실패");
        }
    }

    @Nested
    @DisplayName("convert() 테스트")
    class ConvertTest {

        @Test
        @DisplayName("Map을 객체로 변환한다")
        void convert_convertsMapToObject() {
            // given
            Map<String, Object> map = Map.of("id", 1L, "name", "테스트");

            // when
            TestDto dto = JsonUtils.convert(map, TestDto.class);

            // then
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("객체를 다른 타입으로 변환한다")
        void convert_convertsObjectToAnotherType() {
            // given
            TestDto source = new TestDto(1L, "테스트", null);

            // when
            Map<String, Object> map = JsonUtils.convert(source, Map.class);

            // then
            assertThat(map)
                    .containsEntry("id", 1)
                    .containsEntry("name", "테스트");
        }
    }

    @Nested
    @DisplayName("isValidJson() 테스트")
    class IsValidJsonTest {

        @Test
        @DisplayName("유효한 JSON 객체는 true를 반환한다")
        void isValidJson_withValidObject_returnsTrue() {
            // given
            String json = "{\"key\":\"value\"}";

            // when & then
            assertThat(JsonUtils.isValidJson(json)).isTrue();
        }

        @Test
        @DisplayName("유효한 JSON 배열은 true를 반환한다")
        void isValidJson_withValidArray_returnsTrue() {
            // given
            String json = "[1, 2, 3]";

            // when & then
            assertThat(JsonUtils.isValidJson(json)).isTrue();
        }

        @Test
        @DisplayName("유효한 JSON 문자열 값은 true를 반환한다")
        void isValidJson_withStringValue_returnsTrue() {
            // given
            String json = "\"hello\"";

            // when & then
            assertThat(JsonUtils.isValidJson(json)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "{invalid}", "not json"})
        @DisplayName("유효하지 않은 JSON은 false를 반환한다")
        void isValidJson_withInvalidJson_returnsFalse(String invalidJson) {
            // when & then
            assertThat(JsonUtils.isValidJson(invalidJson)).isFalse();
        }
    }

    // ========================================
    // 예외 클래스 테스트
    // ========================================

    @Nested
    @DisplayName("JsonConversionException 테스트")
    class JsonConversionExceptionTest {

        @Test
        @DisplayName("메시지만으로 예외를 생성할 수 있다")
        void constructor_withMessage() {
            // when
            JsonConversionException exception = new JsonConversionException("테스트 에러");

            // then
            assertThat(exception.getMessage()).isEqualTo("테스트 에러");
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("메시지와 원인으로 예외를 생성할 수 있다")
        void constructor_withMessageAndCause() {
            // given
            RuntimeException cause = new RuntimeException("원인");

            // when
            JsonConversionException exception = new JsonConversionException("테스트 에러", cause);

            // then
            assertThat(exception.getMessage()).isEqualTo("테스트 에러");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    // ========================================
    // 라운드트립 테스트
    // ========================================

    @Nested
    @DisplayName("라운드트립(직렬화 → 역직렬화) 테스트")
    class RoundTripTest {

        @Test
        @DisplayName("객체를 직렬화 후 역직렬화하면 원본과 동일하다")
        void roundTrip_preservesData() {
            // given
            TestDto original = new TestDto(1L, "테스트", LocalDateTime.of(2025, 1, 15, 10, 30, 0));

            // when
            String json = JsonUtils.toJson(original);
            TestDto restored = JsonUtils.fromJson(json, TestDto.class);

            // then
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("List를 직렬화 후 역직렬화하면 원본과 동일하다")
        void roundTrip_preservesList() {
            // given
            List<TestDto> original = List.of(
                    new TestDto(1L, "첫번째", null),
                    new TestDto(2L, "두번째", null)
            );

            // when
            String json = JsonUtils.toJson(original);
            List<TestDto> restored = JsonUtils.fromJsonToList(json, TestDto.class);

            // then
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("bytes를 통한 라운드트립도 데이터를 보존한다")
        void roundTrip_withBytes_preservesData() {
            // given
            TestDto original = new TestDto(1L, "테스트", null);

            // when
            byte[] bytes = JsonUtils.toBytes(original);
            TestDto restored = JsonUtils.fromBytes(bytes, TestDto.class);

            // then
            assertThat(restored).isEqualTo(original);
        }
    }
}