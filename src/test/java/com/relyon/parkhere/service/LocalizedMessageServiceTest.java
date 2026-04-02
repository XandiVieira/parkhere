package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalizedMessageServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private LocalizedMessageService localizedMessageService;

    @Test
    void translate_shouldResolveMessageFromException() {
        var exception = new DomainException("user.not.found", "test@test.com");
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        when(messageSource.getMessage("user.not.found", new String[]{"test@test.com"}, Locale.ENGLISH))
                .thenReturn("User not found: test@test.com");

        var result = localizedMessageService.translate(exception);

        assertEquals("User not found: test@test.com", result);
    }

    @Test
    void translate_shouldResolveMessageByKey() {
        LocaleContextHolder.setLocale(new Locale("pt"));
        when(messageSource.getMessage("validation.failed", new Object[]{}, new Locale("pt")))
                .thenReturn("Falha na validação");

        var result = localizedMessageService.translate("validation.failed");

        assertEquals("Falha na validação", result);
    }

    @Test
    void translate_shouldUseCurrentLocale() {
        var exception = new DomainException("auth.invalid.credentials");
        LocaleContextHolder.setLocale(new Locale("pt"));
        when(messageSource.getMessage("auth.invalid.credentials", new String[]{}, new Locale("pt")))
                .thenReturn("Email ou senha inválidos");

        var result = localizedMessageService.translate(exception);

        assertEquals("Email ou senha inválidos", result);
    }
}
