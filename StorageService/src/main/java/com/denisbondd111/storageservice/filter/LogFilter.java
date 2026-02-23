package com.denisbondd111.storageservice.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LogFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        System.out.println("=== Incoming Request to StorageService ===");
        System.out.println("Method: " + req.getMethod());
        System.out.println("Content-Type: " + req.getContentType());

        java.util.Collections.list(req.getHeaderNames())
                .forEach(name -> System.out.println(name + ": " + req.getHeader(name)));

        chain.doFilter(request, response);
    }
}
