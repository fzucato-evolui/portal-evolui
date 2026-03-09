package br.com.evolui.portalevolui.web.security;

import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import br.com.evolui.portalevolui.web.component.JwtUtilComponent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JWTAuthorizationFilterSecurity extends OncePerRequestFilter {

    @Value("${evolui.app.externalFolder}")
    private String externalFolder;

    @Autowired
    private JwtUtilComponent jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthorizationFilterSecurity.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UsernamePasswordAuthenticationToken authentication = null;
        try {
            //logger.info("IP Request:" + request.getRemoteAddr());
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }
        try {
            String redirect = this.getRedirect(request, response, authentication != null ? (UserDetailsSecurity) authentication.getPrincipal() : null);
            if (redirect != null) {
                request.getRequestDispatcher(redirect)
                        .forward(request, response);
                return;
            }
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7, headerAuth.length());
        } else if (request.getServletPath().equals("/portalEvoluiWebSocket") && StringUtils.hasText(request.getParameter("Authorization"))) {
            return request.getParameter("Authorization");
        }

        return null;
    }

    private String getRedirect(HttpServletRequest request,HttpServletResponse response, UserDetailsSecurity userDetails) throws Exception {
        if (StringUtils.hasLength(this.externalFolder)) {
            if (request.getServletPath().startsWith("/server-files/files/")) {
                if (userDetails != null) {
                    if (!userDetails.getAuthorities().stream().anyMatch(x ->
                            x.getAuthority().equals(ProfileEnum.ROLE_ADMIN.value()) ||
                                    x.getAuthority().equals(ProfileEnum.ROLE_SUPER.value()) ||
                                            x.getAuthority().equals(ProfileEnum.ROLE_HYPER.value())
                    )) {
                        if(!request.getServletPath().startsWith("/server-files/files/" + userDetails.getId() + "/")) {
                            throw new Exception("não permitido");
                        };
                    }
                } else {
                    throw new Exception("não permitido");
                }
            } else if (request.getServletPath().startsWith("/s-link")) {
                if (this.jwtUtils.validateJwtToken(request.getQueryString())) {
                    return this.jwtUtils.getFilePathFromJwtToken(request.getQueryString());

                } else {
                    throw new Exception("não permitido");
                }

            }
        }
        return null;
    }

}
