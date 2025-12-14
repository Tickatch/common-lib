package io.github.tickatch.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
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
    @DisplayName("of(userId, userType) 팩토리 메서드로 생성한다")
    void of_withUserIdAndUserType_createsInstance() {
      // given
      String userId = UUID.randomUUID().toString();
      UserType userType = UserType.CUSTOMER;

      // when
      AuthenticatedUser user = AuthenticatedUser.of(userId, userType);

      // then
      assertThat(user).isNotNull();
      assertThat(user.getUserId()).isEqualTo(userId);
      assertThat(user.getUserType()).isEqualTo(userType);
    }

    @Test
    @DisplayName("of(userId, String) 팩토리 메서드로 생성한다")
    void of_withUserIdAndStringUserType_createsInstance() {
      // given
      String userId = "usr-12345-abcde";
      String userType = "SELLER";

      // when
      AuthenticatedUser user = AuthenticatedUser.of(userId, userType);

      // then
      assertThat(user.getUserId()).isEqualTo(userId);
      assertThat(user.getUserType()).isEqualTo(UserType.SELLER);
    }

    @Test
    @DisplayName("of(userId) 팩토리 메서드로 생성한다 (하위 호환성)")
    @SuppressWarnings("deprecation")
    void of_withUserIdOnly_createsInstance() {
      // given
      String userId = UUID.randomUUID().toString();

      // when
      AuthenticatedUser user = AuthenticatedUser.of(userId);

      // then
      assertThat(user).isNotNull();
      assertThat(user.getUserId()).isEqualTo(userId);
      assertThat(user.getUserType()).isNull();
    }

    @Test
    @DisplayName("생성자로 직접 생성한다")
    void constructor_createsInstance() {
      // given
      String userId = "usr-12345-abcde";
      UserType userType = UserType.ADMIN;

      // when
      AuthenticatedUser user = new AuthenticatedUser(userId, userType);

      // then
      assertThat(user.getUserId()).isEqualTo(userId);
      assertThat(user.getUserType()).isEqualTo(userType);
    }

    @Test
    @DisplayName("null userId로 생성할 수 있다")
    void of_withNullUserId_createsInstance() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of(null, UserType.CUSTOMER);

      // then
      assertThat(user.getUserId()).isNull();
      assertThat(user.getUserType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    @DisplayName("null userType으로 생성할 수 있다")
    void of_withNullUserType_createsInstance() {
      // given
      String userId = "test-user";

      // when
      AuthenticatedUser user = AuthenticatedUser.of(userId, (UserType) null);

      // then
      assertThat(user.getUserId()).isEqualTo(userId);
      assertThat(user.getUserType()).isNull();
    }

    @Test
    @DisplayName("UUID 형식의 userId로 생성한다")
    void of_withUuid_createsInstance() {
      // given
      String uuid = "550e8400-e29b-41d4-a716-446655440000";

      // when
      AuthenticatedUser user = AuthenticatedUser.of(uuid, UserType.CUSTOMER);

      // then
      assertThat(user.getUserId()).isEqualTo(uuid);
    }
  }

  // ========================================
  // UserType 문자열 파싱 테스트
  // ========================================

  @Nested
  @DisplayName("UserType 문자열 파싱 테스트")
  class UserTypeParsingTest {

    @Test
    @DisplayName("CUSTOMER 문자열을 파싱한다")
    void parseUserType_customer() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "CUSTOMER");

      // then
      assertThat(user.getUserType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    @DisplayName("SELLER 문자열을 파싱한다")
    void parseUserType_seller() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "SELLER");

      // then
      assertThat(user.getUserType()).isEqualTo(UserType.SELLER);
    }

    @Test
    @DisplayName("ADMIN 문자열을 파싱한다")
    void parseUserType_admin() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "ADMIN");

      // then
      assertThat(user.getUserType()).isEqualTo(UserType.ADMIN);
    }

    @Test
    @DisplayName("null 문자열은 null UserType으로 변환된다")
    void parseUserType_null_returnsNull() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", (String) null);

      // then
      assertThat(user.getUserType()).isNull();
    }

    @Test
    @DisplayName("빈 문자열은 null UserType으로 변환된다")
    void parseUserType_empty_returnsNull() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "");

      // then
      assertThat(user.getUserType()).isNull();
    }

    @Test
    @DisplayName("공백 문자열은 null UserType으로 변환된다")
    void parseUserType_blank_returnsNull() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "   ");

      // then
      assertThat(user.getUserType()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 문자열은 null UserType으로 변환된다")
    void parseUserType_invalid_returnsNull() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "INVALID_TYPE");

      // then
      assertThat(user.getUserType()).isNull();
    }

    @Test
    @DisplayName("소문자 문자열은 null UserType으로 변환된다")
    void parseUserType_lowercase_returnsNull() {
      // when
      AuthenticatedUser user = AuthenticatedUser.of("user-id", "customer");

      // then
      assertThat(user.getUserType()).isNull();
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
      AuthenticatedUser user = AuthenticatedUser.of("test-user-id", UserType.CUSTOMER);

      // then
      assertThat(user).isInstanceOf(UserDetails.class);
    }

    @Test
    @DisplayName("getUsername()은 userId를 반환한다")
    void getUsername_returnsUserId() {
      // given
      String userId = "usr-abc-123";
      AuthenticatedUser user = AuthenticatedUser.of(userId, UserType.CUSTOMER);

      // when
      String username = user.getUsername();

      // then
      assertThat(username).isEqualTo(userId);
    }

    @Test
    @DisplayName("getUsername()은 null userId일 때 null을 반환한다")
    void getUsername_withNullUserId_returnsNull() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of(null, UserType.CUSTOMER);

      // when
      String username = user.getUsername();

      // then
      assertThat(username).isNull();
    }

    @Test
    @DisplayName("getPassword()는 빈 문자열을 반환한다")
    void getPassword_returnsEmptyString() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

      // when
      String password = user.getPassword();

      // then
      assertThat(password).isEmpty();
    }
  }

  // ========================================
  // 권한(Authorities) 테스트
  // ========================================

  @Nested
  @DisplayName("권한(Authorities) 테스트")
  class AuthoritiesTest {

    @Test
    @DisplayName("CUSTOMER 유형은 ROLE_CUSTOMER 권한을 가진다")
    void getAuthorities_customer_hasRoleCustomer() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

      // when
      var authorities = user.getAuthorities();

      // then
      assertThat(authorities).hasSize(1);
      assertThat(authorities)
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("SELLER 유형은 ROLE_SELLER 권한을 가진다")
    void getAuthorities_seller_hasRoleSeller() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.SELLER);

      // when
      var authorities = user.getAuthorities();

      // then
      assertThat(authorities).hasSize(1);
      assertThat(authorities)
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_SELLER");
    }

    @Test
    @DisplayName("ADMIN 유형은 ROLE_ADMIN 권한을 가진다")
    void getAuthorities_admin_hasRoleAdmin() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.ADMIN);

      // when
      var authorities = user.getAuthorities();

      // then
      assertThat(authorities).hasSize(1);
      assertThat(authorities)
          .extracting(GrantedAuthority::getAuthority)
          .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("null userType은 빈 권한 컬렉션을 반환한다")
    void getAuthorities_nullUserType_returnsEmptyCollection() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", (UserType) null);

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
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

      // then
      assertThat(user.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isAccountNonLocked()는 항상 true를 반환한다")
    void isAccountNonLocked_returnsTrue() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

      // then
      assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("isCredentialsNonExpired()는 항상 true를 반환한다")
    void isCredentialsNonExpired_returnsTrue() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

      // then
      assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("isEnabled()는 항상 true를 반환한다")
    void isEnabled_returnsTrue() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

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
    @DisplayName("동일한 userId와 userType을 가진 인스턴스는 equals()가 true이다")
    void equals_withSameValues_returnsTrue() {
      // given
      String userId = "same-user-id";
      UserType userType = UserType.SELLER;
      AuthenticatedUser user1 = AuthenticatedUser.of(userId, userType);
      AuthenticatedUser user2 = AuthenticatedUser.of(userId, userType);

      // then
      assertThat(user1).isEqualTo(user2);
    }

    @Test
    @DisplayName("다른 userId를 가진 인스턴스는 equals()가 false이다")
    void equals_withDifferentUserId_returnsFalse() {
      // given
      AuthenticatedUser user1 = AuthenticatedUser.of("user-1", UserType.CUSTOMER);
      AuthenticatedUser user2 = AuthenticatedUser.of("user-2", UserType.CUSTOMER);

      // then
      assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("다른 userType을 가진 인스턴스는 equals()가 false이다")
    void equals_withDifferentUserType_returnsFalse() {
      // given
      AuthenticatedUser user1 = AuthenticatedUser.of("user-id", UserType.CUSTOMER);
      AuthenticatedUser user2 = AuthenticatedUser.of("user-id", UserType.SELLER);

      // then
      assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("동일한 값을 가진 인스턴스는 같은 hashCode를 갖는다")
    void hashCode_withSameValues_returnsSameHash() {
      // given
      String userId = "same-user-id";
      UserType userType = UserType.ADMIN;
      AuthenticatedUser user1 = AuthenticatedUser.of(userId, userType);
      AuthenticatedUser user2 = AuthenticatedUser.of(userId, userType);

      // then
      assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("toString()은 record 형식으로 반환된다")
    void toString_returnsRecordFormat() {
      // given
      String userId = "test-user-42";
      UserType userType = UserType.CUSTOMER;
      AuthenticatedUser user = AuthenticatedUser.of(userId, userType);

      // when
      String str = user.toString();

      // then
      assertThat(str).contains("AuthenticatedUser");
      assertThat(str).contains(userId);
      assertThat(str).contains("CUSTOMER");
    }

    @Test
    @DisplayName("userId() 접근자 메서드로 값에 접근할 수 있다")
    void userId_accessorMethod_returnsValue() {
      // given
      String userId = "accessor-test";
      AuthenticatedUser user = new AuthenticatedUser(userId, UserType.CUSTOMER);

      // then
      assertThat(user.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("userType() 접근자 메서드로 값에 접근할 수 있다")
    void userType_accessorMethod_returnsValue() {
      // given
      UserType userType = UserType.SELLER;
      AuthenticatedUser user = new AuthenticatedUser("user-id", userType);

      // then
      assertThat(user.userType()).isEqualTo(userType);
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
      AuthenticatedUser user = AuthenticatedUser.of("", UserType.CUSTOMER);

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
      AuthenticatedUser user = AuthenticatedUser.of(userId, UserType.CUSTOMER);

      // then
      assertThat(user.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("한글이 포함된 userId를 처리한다")
    void koreanUserId() {
      // given
      String userId = "사용자-123";

      // when
      AuthenticatedUser user = AuthenticatedUser.of(userId, UserType.CUSTOMER);

      // then
      assertThat(user.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("매우 긴 userId를 처리한다")
    void veryLongUserId() {
      // given
      String userId = "a".repeat(1000);

      // when
      AuthenticatedUser user = AuthenticatedUser.of(userId, UserType.CUSTOMER);

      // then
      assertThat(user.getUserId()).hasSize(1000);
    }
  }

  // ========================================
  // UserType 헬퍼 메서드 테스트
  // ========================================

  @Nested
  @DisplayName("UserType 헬퍼 메서드 테스트")
  class UserTypeHelperMethodsTest {

    @Test
    @DisplayName("CUSTOMER 유형에서 isCustomer()는 true를 반환한다")
    void isCustomer_withCustomer_returnsTrue() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.CUSTOMER);

      // then
      assertThat(user.getUserType().isCustomer()).isTrue();
      assertThat(user.getUserType().isSeller()).isFalse();
      assertThat(user.getUserType().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("SELLER 유형에서 isSeller()는 true를 반환한다")
    void isSeller_withSeller_returnsTrue() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.SELLER);

      // then
      assertThat(user.getUserType().isCustomer()).isFalse();
      assertThat(user.getUserType().isSeller()).isTrue();
      assertThat(user.getUserType().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("ADMIN 유형에서 isAdmin()는 true를 반환한다")
    void isAdmin_withAdmin_returnsTrue() {
      // given
      AuthenticatedUser user = AuthenticatedUser.of("user-id", UserType.ADMIN);

      // then
      assertThat(user.getUserType().isCustomer()).isFalse();
      assertThat(user.getUserType().isSeller()).isFalse();
      assertThat(user.getUserType().isAdmin()).isTrue();
    }
  }
}