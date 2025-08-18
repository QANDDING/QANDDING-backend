package com.qandding.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures a CSRF token is generated and the XSRF-TOKEN cookie is set
 * for SPA clients to read and send back via X-XSRF-TOKEN header.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      // Trigger token resolution so CookieCsrfTokenRepository writes the cookie
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}

