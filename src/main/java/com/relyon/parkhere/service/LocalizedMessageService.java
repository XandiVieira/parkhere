package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LocalizedMessageService {

    private final MessageSource messageSource;

    public String translate(DomainException exception) {
        var locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(exception.getMessageKey(), exception.getArguments(), locale);
    }

    public String translate(String key, Object... args) {
        var locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, locale);
    }
}
