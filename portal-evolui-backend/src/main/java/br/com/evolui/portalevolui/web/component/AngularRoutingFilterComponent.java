package br.com.evolui.portalevolui.web.component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AngularRoutingFilterComponent implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
        String requestURI = httpServletRequest.getRequestURI();
        String contextPath = httpServletRequest.getContextPath();

        if (shouldDispatch(contextPath, requestURI)) {
            request.getRequestDispatcher("/").forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    private boolean shouldDispatch(String contextPath, String uri) {
        /* Exclude/Inlclude URLs here */
        return (!uri.startsWith(contextPath + "/api") &&
                !uri.startsWith(contextPath + "/h2-console") &&
                !uri.startsWith(contextPath + "/server-files") &&
                !uri.startsWith(contextPath + "/portalEvoluiWebSocket") &&
                !uri.startsWith(contextPath + "/app") &&
                !uri.startsWith(contextPath + "/s-link") &&
                !uri.startsWith(contextPath + "/v3/api-docs") &&
                !uri.startsWith(contextPath + "/swagger-ui") &&
                //!uri.equals(contextPath) &&
                // only forward if there's no file extension (exclude *.js, *.css etc)
                uri.matches("^([^.]+)$")) ||
                (uri.startsWith(contextPath + "/sign-in"));
    }

}
