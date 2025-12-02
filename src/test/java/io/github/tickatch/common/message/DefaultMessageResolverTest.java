package io.github.tickatch.common.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DefaultMessageResolver 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultMessageResolver 테스트")
class DefaultMessageResolverTest {

    @Mock
    private MessageSource messageSource;

    private DefaultMessageResolver messageResolver;

    @BeforeEach
    void setUp() {
        messageResolver = new DefaultMessageResolver(messageSource);
        LocaleContextHolder.setLocale(Locale.KOREAN);
    }

    // ========================================
    // resolve(code, args) 테스트
    // ========================================

    @Nested
    @DisplayName("resolve(code, args) 테스트")
    class ResolveTest {

        @Test
        @DisplayName("메시지 코드로 메시지를 조회한다")
        void resolve_withCode_returnsMessage() {
            // given
            when(messageSource.getMessage("TICKET_NOT_FOUND", new Object[]{}, Locale.KOREAN))
                    .thenReturn("티켓을 찾을 수 없습니다.");

            // when
            String result = messageResolver.resolve("TICKET_NOT_FOUND");

            // then
            assertThat(result).isEqualTo("티켓을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("인자를 포함한 메시지를 조회한다")
        void resolve_withArgs_returnsFormattedMessage() {
            // given
            Object[] args = new Object[]{123L};
            when(messageSource.getMessage("TICKET_NOT_FOUND", args, Locale.KOREAN))
                    .thenReturn("티켓 123을(를) 찾을 수 없습니다.");

            // when
            String result = messageResolver.resolve("TICKET_NOT_FOUND", 123L);

            // then
            assertThat(result).isEqualTo("티켓 123을(를) 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("여러 인자를 포함한 메시지를 조회한다")
        void resolve_withMultipleArgs_returnsFormattedMessage() {
            // given
            Object[] args = new Object[]{"A", "15"};
            when(messageSource.getMessage("SEAT_RESERVED", args, Locale.KOREAN))
                    .thenReturn("A구역 15번 좌석은 이미 예약되었습니다.");

            // when
            String result = messageResolver.resolve("SEAT_RESERVED", "A", "15");

            // then
            assertThat(result).isEqualTo("A구역 15번 좌석은 이미 예약되었습니다.");
        }

        @Test
        @DisplayName("메시지가 없으면 코드를 반환한다")
        void resolve_withNoMessage_returnsCode() {
            // given
            when(messageSource.getMessage(eq("UNKNOWN_CODE"), any(), any(Locale.class)))
                    .thenThrow(new NoSuchMessageException("UNKNOWN_CODE"));

            // when
            String result = messageResolver.resolve("UNKNOWN_CODE");

            // then
            assertThat(result).isEqualTo("UNKNOWN_CODE");
        }

        @Test
        @DisplayName("현재 Locale을 사용한다")
        void resolve_usesCurrentLocale() {
            // given
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            when(messageSource.getMessage("HELLO", new Object[]{}, Locale.ENGLISH))
                    .thenReturn("Hello");

            // when
            String result = messageResolver.resolve("HELLO");

            // then
            assertThat(result).isEqualTo("Hello");
            verify(messageSource).getMessage("HELLO", new Object[]{}, Locale.ENGLISH);
        }
    }

    // ========================================
    // resolve(code, locale, args) 테스트
    // ========================================

    @Nested
    @DisplayName("resolve(code, locale, args) 테스트")
    class ResolveWithLocaleTest {

        @Test
        @DisplayName("지정된 Locale로 메시지를 조회한다")
        void resolve_withLocale_usesSpecifiedLocale() {
            // given
            when(messageSource.getMessage("HELLO", new Object[]{}, Locale.ENGLISH))
                    .thenReturn("Hello");

            // when
            String result = messageResolver.resolve("HELLO", Locale.ENGLISH);

            // then
            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("다른 Locale로 조회해도 현재 Locale에 영향 없다")
        void resolve_withLocale_doesNotAffectCurrentLocale() {
            // given
            LocaleContextHolder.setLocale(Locale.KOREAN);
            when(messageSource.getMessage("HELLO", new Object[]{}, Locale.JAPANESE))
                    .thenReturn("こんにちは");

            // when
            String result = messageResolver.resolve("HELLO", Locale.JAPANESE);

            // then
            assertThat(result).isEqualTo("こんにちは");
            assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.KOREAN);
        }

        @Test
        @DisplayName("메시지가 없으면 코드를 반환한다")
        void resolve_withLocaleAndNoMessage_returnsCode() {
            // given
            when(messageSource.getMessage(eq("UNKNOWN"), any(), eq(Locale.FRENCH)))
                    .thenThrow(new NoSuchMessageException("UNKNOWN"));

            // when
            String result = messageResolver.resolve("UNKNOWN", Locale.FRENCH);

            // then
            assertThat(result).isEqualTo("UNKNOWN");
        }

        @Test
        @DisplayName("Locale과 인자를 함께 사용한다")
        void resolve_withLocaleAndArgs_returnsFormattedMessage() {
            // given
            Object[] args = new Object[]{"Tokyo"};
            when(messageSource.getMessage("WELCOME", args, Locale.JAPANESE))
                    .thenReturn("Tokyoへようこそ");

            // when
            String result = messageResolver.resolve("WELCOME", Locale.JAPANESE, "Tokyo");

            // then
            assertThat(result).isEqualTo("Tokyoへようこそ");
        }
    }

    // ========================================
    // resolveWithDefault() 테스트
    // ========================================

    @Nested
    @DisplayName("resolveWithDefault() 테스트")
    class ResolveWithDefaultTest {

        @Test
        @DisplayName("메시지가 있으면 메시지를 반환한다")
        void resolveWithDefault_withMessage_returnsMessage() {
            // given
            when(messageSource.getMessage(eq("FOUND"), any(), eq("기본 메시지"), any(Locale.class)))
                    .thenReturn("찾았습니다.");

            // when
            String result = messageResolver.resolveWithDefault("FOUND", "기본 메시지");

            // then
            assertThat(result).isEqualTo("찾았습니다.");
        }

        @Test
        @DisplayName("메시지가 없으면 기본 메시지를 반환한다")
        void resolveWithDefault_withNoMessage_returnsDefaultMessage() {
            // given
            when(messageSource.getMessage(eq("UNKNOWN"), any(), eq("기본 메시지"), any(Locale.class)))
                    .thenReturn("기본 메시지");

            // when
            String result = messageResolver.resolveWithDefault("UNKNOWN", "기본 메시지");

            // then
            assertThat(result).isEqualTo("기본 메시지");
        }

        @Test
        @DisplayName("인자와 함께 기본 메시지를 사용한다")
        void resolveWithDefault_withArgs_returnsFormattedMessage() {
            // given
            Object[] args = new Object[]{"테스트"};
            when(messageSource.getMessage(eq("GREETING"), eq(args), eq("안녕, {0}!"), any(Locale.class)))
                    .thenReturn("안녕, 테스트!");

            // when
            String result = messageResolver.resolveWithDefault("GREETING", "안녕, {0}!", "테스트");

            // then
            assertThat(result).isEqualTo("안녕, 테스트!");
        }
    }

    // ========================================
    // MessageResolver 인터페이스 구현 테스트
    // ========================================

    @Nested
    @DisplayName("MessageResolver 인터페이스 구현 테스트")
    class MessageResolverInterfaceTest {

        @Test
        @DisplayName("MessageResolver 인터페이스를 구현한다")
        void implementsMessageResolver() {
            // then
            assertThat(messageResolver).isInstanceOf(MessageResolver.class);
        }
    }
}