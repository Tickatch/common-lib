package io.github.tickatch.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * AuthenticatedUser 단위 테스트.
 */
@DisplayName("AuthenticatedUser 테스트")
class AuthenticatedUserTest {

    // ========================================
    // 생성 테스트
    // ========================================

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("of() 팩토리 메서드로 생성한다")
        void of_createsInstance() {
            // given
            String userId = UUID.randomUUID().toString();

            // when
            AuthenticatedUser user = AuthenticatedUser.of(userId);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("생성자로 직접 생성한다")
        void constructor_createsInstance() {
            // given
            String userId = "usr-12345-abcde";

            // when
            AuthenticatedUser user = new AuthenticatedUser(userId);

            // then
            assertThat(user.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("null userId로 생성할 수 있다")
        void of_withNull_createsInstance() {
            // when
            AuthenticatedUser user = AuthenticatedUser.of(null);

            // then
            assertThat(user.getUserId()).isNull();
        }

        @Test
        @DisplayName("UUID 형식의 userId로 생성한다")
        void of_withUuid_createsInstance() {
            // given
            String uuid = "550e8400-e29b-41d4-a716-446655440000";

            // when
            AuthenticatedUser user = AuthenticatedUser.of(uuid);

            // then
            assertThat(user.getUserId()).isEqualTo(uuid);
        }
    }

    // ========================================
    // UserDetails 구현 테스트
    // ========================================

    @Nested
    @DisplayName("UserDetails 구현 테스트")
    class UserDetailsImplementationTest {

        @Test
        @DisplayName("UserDetails 인터페이스를 구현한다")
        void implementsUserDetails() {
            // when
            AuthenticatedUser user = AuthenticatedUser.of("test-user-id");

            // then
            assertThat(user).isInstanceOf(UserDetails.class);
        }

        @Test
        @DisplayName("getUsername()은 userId를 반환한다")
        void getUsername_returnsUserId() {
            // given
            String userId = "usr-abc-123";
            AuthenticatedUser user = AuthenticatedUser.of(userId);

            // when
            String username = user.getUsername();

            // then
            assertThat(username).isEqualTo(userId);
        }

        @Test
        @DisplayName("getUsername()은 null userId일 때 null을 반환한다")
        void getUsername_withNullUserId_returnsNull() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of(null);

            // when
            String username = user.getUsername();

            // then
            assertThat(username).isNull();
        }

        @Test
        @DisplayName("getPassword()는 빈 문자열을 반환한다")
        void getPassword_returnsEmptyString() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of("user-id");

            // when
            String password = user.getPassword();

            // then
            assertThat(password).isEmpty();
        }

        @Test
        @DisplayName("getAuthorities()는 빈 컬렉션을 반환한다")
        void getAuthorities_returnsEmptyCollection() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of("user-id");

            // when
            var authorities = user.getAuthorities();

            // then
            assertThat(authorities).isEmpty();
        }
    }

    // ========================================
    // 계정 상태 테스트
    // ========================================

    @Nested
    @DisplayName("계정 상태 테스트")
    class AccountStatusTest {

        @Test
        @DisplayName("isAccountNonExpired()는 항상 true를 반환한다")
        void isAccountNonExpired_returnsTrue() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of("user-id");

            // then
            assertThat(user.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("isAccountNonLocked()는 항상 true를 반환한다")
        void isAccountNonLocked_returnsTrue() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of("user-id");

            // then
            assertThat(user.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("isCredentialsNonExpired()는 항상 true를 반환한다")
        void isCredentialsNonExpired_returnsTrue() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of("user-id");

            // then
            assertThat(user.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("isEnabled()는 항상 true를 반환한다")
        void isEnabled_returnsTrue() {
            // given
            AuthenticatedUser user = AuthenticatedUser.of("user-id");

            // then
            assertThat(user.isEnabled()).isTrue();
        }
    }

    // ========================================
    // Record 특성 테스트
    // ========================================

    @Nested
    @DisplayName("Record 특성 테스트")
    class RecordFeaturesTest {

        @Test
        @DisplayName("동일한 userId를 가진 인스턴스는 equals()가 true이다")
        void equals_withSameUserId_returnsTrue() {
            // given
            String userId = "same-user-id";
            AuthenticatedUser user1 = AuthenticatedUser.of(userId);
            AuthenticatedUser user2 = AuthenticatedUser.of(userId);

            // then
            assertThat(user1).isEqualTo(user2);
        }

        @Test
        @DisplayName("다른 userId를 가진 인스턴스는 equals()가 false이다")
        void equals_withDifferentUserId_returnsFalse() {
            // given
            AuthenticatedUser user1 = AuthenticatedUser.of("user-1");
            AuthenticatedUser user2 = AuthenticatedUser.of("user-2");

            // then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("동일한 userId를 가진 인스턴스는 같은 hashCode를 갖는다")
        void hashCode_withSameUserId_returnsSameHash() {
            // given
            String userId = "same-user-id";
            AuthenticatedUser user1 = AuthenticatedUser.of(userId);
            AuthenticatedUser user2 = AuthenticatedUser.of(userId);

            // then
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("toString()은 record 형식으로 반환된다")
        void toString_returnsRecordFormat() {
            // given
            String userId = "test-user-42";
            AuthenticatedUser user = AuthenticatedUser.of(userId);

            // when
            String str = user.toString();

            // then
            assertThat(str).contains("AuthenticatedUser");
            assertThat(str).contains(userId);
        }

        @Test
        @DisplayName("userId() 접근자 메서드로 값에 접근할 수 있다")
        void userId_accessorMethod_returnsValue() {
            // given
            String userId = "accessor-test";
            AuthenticatedUser user = new AuthenticatedUser(userId);

            // then
            assertThat(user.userId()).isEqualTo(userId);
        }
    }

    // ========================================
    // 다양한 userId 값 테스트
    // ========================================

    @Nested
    @DisplayName("다양한 userId 값 테스트")
    class VariousUserIdTest {

        @Test
        @DisplayName("빈 문자열 userId를 처리한다")
        void emptyUserId() {
            // when
            AuthenticatedUser user = AuthenticatedUser.of("");

            // then
            assertThat(user.getUserId()).isEmpty();
            assertThat(user.getUsername()).isEmpty();
        }

        @Test
        @DisplayName("특수문자가 포함된 userId를 처리한다")
        void specialCharactersUserId() {
            // given
            String userId = "user-123_abc@test";

            // when
            AuthenticatedUser user = AuthenticatedUser.of(userId);

            // then
            assertThat(user.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("한글이 포함된 userId를 처리한다")
        void koreanUserId() {
            // given
            String userId = "사용자-123";

            // when
            AuthenticatedUser user = AuthenticatedUser.of(userId);

            // then
            assertThat(user.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("매우 긴 userId를 처리한다")
        void veryLongUserId() {
            // given
            String userId = "a".repeat(1000);

            // when
            AuthenticatedUser user = AuthenticatedUser.of(userId);

            // then
            assertThat(user.getUserId()).hasSize(1000);
        }
    }
}