package com.appdev.Finance.Config; // Or com.appdev.Finance.config if you use a sub-package

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry; // Required import
// import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity; // REMOVED FROM HERE
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; // Crucial annotation
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // This enables STOMP over WebSocket message handling and the broker
// @EnableWebSocketSecurity      // REMOVED FROM HERE
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker.
     * This method sets up how messages are routed.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enables a simple in-memory message broker to carry messages back to the client
        // on destinations prefixed with "/topic" (for pub-sub), "/queue" (often for point-to-point),
        // and "/user" (for user-specific messaging).
        config.enableSimpleBroker("/topic", "/queue", "/user");

        // Designates the "/app" prefix for messages that are bound for
        // @MessageMapping-annotated methods (i.e., messages from clients to the server).
        config.setApplicationDestinationPrefixes("/app");

        // Configures the prefix used to identify user-specific destinations.
        // When using SimpMessagingTemplate.convertAndSendToUser(), Spring prepends this prefix.
        // For example, sending to a user "john" at "/queue/notifications" becomes "/user/john/queue/notifications".
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registers STOMP endpoints, mapping each endpoint to a specific URL
     * and enabling SockJS fallback options.
     * Clients will connect to these endpoints to establish a WebSocket connection.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registers the "/ws-notifications" endpoint for WebSocket connections.
        // withSockJS() enables SockJS fallback options for browsers or environments
        // where WebSocket is not fully supported, using alternative transports like polling.
        registry.addEndpoint("/ws-notifications") // Client connects to this STOMP endpoint
                .withSockJS();                    // Enable SockJS fallback options
    }
}