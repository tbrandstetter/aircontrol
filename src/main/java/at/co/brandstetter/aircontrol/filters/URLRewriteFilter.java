package at.co.brandstetter.aircontrol.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class URLRewriteFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        String requestURI = servletRequest.getRequestURI();
        String contextPath = servletRequest.getContextPath();
        String POINT_EXCLUSION_PATTERN = "^([^.]+)$";
        String API_PATTERN = "^/api/(.+)$";
        if(!requestURI.equals(contextPath) &&
                !requestURI.matches(API_PATTERN) && // Check if the requested URL is not a controller (/api/**)
                requestURI.matches(POINT_EXCLUSION_PATTERN) // Check if there are no "." in requested URL
        ) {
            RequestDispatcher dispatcher = request.getRequestDispatcher("/");
            dispatcher.forward(request, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
