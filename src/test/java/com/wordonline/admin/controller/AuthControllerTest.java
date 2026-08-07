package com.wordonline.admin.controller;

import com.wordonline.admin.client.AccountServerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AccountServerClient accountServerClient;

    @Test
    void loginCookieIsHttpOnlySecureAndSameSiteStrict() {
        when(accountServerClient.login("admin", "pw")).thenReturn("token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = new AuthController(accountServerClient)
                .login("admin", "pw", response, new ExtendedModelMap());

        assertEquals("redirect:/", view);
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.contains("jwt=token"), setCookie);
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
        assertTrue(setCookie.contains("Secure"), setCookie);
        assertTrue(setCookie.contains("SameSite=Strict"), setCookie);
    }

    @Test
    void logoutClearsCookieWithSameAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthController(accountServerClient).logout(response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.contains("Max-Age=0"), setCookie);
        assertTrue(setCookie.contains("Secure"), setCookie);
        assertTrue(setCookie.contains("SameSite=Strict"), setCookie);
    }
}
