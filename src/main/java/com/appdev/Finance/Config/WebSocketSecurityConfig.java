package com.appdev.Finance.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // Permit all STOMP command types including CONNECT, SEND, SUBSCRIBE, etc.
            // This is very broad and should allow any action by any user (authenticated or not).
            .simpTypeMatchers(
                SimpMessageType.CONNECT,
                SimpMessageType.DISCONNECT,
                SimpMessageType.HEARTBEAT,
                SimpMessageType.MESSAGE,      // Corresponds to SEND frames from client
                SimpMessageType.SUBSCRIBE,
                SimpMessageType.UNSUBSCRIBE,
                SimpMessageType.OTHER // Catch-all for any other STOMP frame type
            ).permitAll()
            // As a fallback, permit any destination, though simpTypeMatchers should cover STOMP commands.
            .simpDestMatchers("/**").permitAll()
            // And a final catch-all
            .anyMessage().permitAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}