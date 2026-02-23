package com.denisbondd111.apigateway.config;

import com.denisbondd111.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.*;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {

        return route("auth-service")
                .route(RequestPredicates.path("/auth/**"),
                        http("http://localhost:8081"))
                .build()

                .and(route("storage-service")
                        .route(RequestPredicates.path("/api/storage/**"),
                                http("http://localhost:8082"))
                        .filter(addUserIdHeader())
                        .build())

                .and(route("metadata-service")
                        .route(RequestPredicates.path("/api/metadata/**"),
                                http("http://localhost:8083"))
                        .filter(addUserIdHeader())
                        .build());
    }

    private HandlerFilterFunction<ServerResponse, ServerResponse> addUserIdHeader() {
        return (request, next) -> {

            String authHeader = request.headers().firstHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ServerResponse.status(401).build();
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            var mutatedRequest = ServerRequest.from(request)
                    .header("X-User-Id", userId)
                    .build();

            return next.handle(mutatedRequest);
        };
    }

    private static HandlerFunction<ServerResponse> http(String uri) {
        return org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http(uri);
    }
}