package org.schoolstock.schoolstock.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Redirects unauthenticated requests to the login page.
 * <p>
 * Regular (full page) requests get a standard HTTP redirect. Requests issued by
 * htmx are answered with a 2xx response carrying the {@code HX-Redirect} header
 * instead, since htmx only honours that header on 2xx responses and would
 * otherwise swap the login page's markup into whatever element issued the
 * request (e.g. a tab panel), rather than navigating the whole page.
 */
public class HtmxAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String LOGIN_URL = "/login";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String loginUrl = request.getContextPath() + LOGIN_URL;
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("HX-Redirect", loginUrl);
        } else {
            response.sendRedirect(loginUrl);
        }
    }
}
