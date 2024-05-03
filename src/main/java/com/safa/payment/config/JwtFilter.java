package com.safa.payment.config;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

  private final TokenManager tokenManager;

  Logger logger = LoggerFactory.getLogger(JwtFilter.class);

  @Autowired
  public JwtFilter(TokenManager tokenManager) {
    this.tokenManager = tokenManager;
  }

  @Override
  protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    String tokenHeader = request.getHeader("Authorization");
    String userId = null;
    String token = null;
    String[] roles = null;
    if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
      token = tokenHeader.substring(7);
      try {
        userId = this.tokenManager.getUserIdFromJwtToken(token);
        roles = this.tokenManager.getRolesFromToken(token);
      } catch (IllegalArgumentException e) {
        logger.error("Unable to get JWT Token");
      } catch (ExpiredJwtException e) {
        logger.error("JWT Token has expired");
      }
    }
    if (StringUtils.isNotEmpty(userId)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = User.builder().username(userId).password(userId).roles(roles).build();
      if (tokenManager.validateJwtToken(token)) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        usernamePasswordAuthenticationToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        // After setting the Authentication in the context, we specify
        // that the current user is authenticated. So it passes the Spring Security Configurations
        // successfully.
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
      }
    }
    filterChain.doFilter(request, response);
  }
}
