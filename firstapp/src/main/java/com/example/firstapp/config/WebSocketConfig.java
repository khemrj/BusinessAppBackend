package com.example.firstapp.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;

    @Value("${app.ws.allowed-origins}") private String[] allowedOrigins;
    @Value("${app.rabbit.host}") private String relayHost;
    @Value("${app.rabbit.stomp-port}") private int relayPort;
    @Value("${app.rabbit.client-login}") private String clientLogin;
    @Value("${app.rabbit.client-passcode}") private String clientPasscode;
    @Value("${app.rabbit.system-login}") private String systemLogin;
    @Value("${app.rabbit.system-passcode}") private String systemPasscode;

    public WebSocketConfig(StompAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(clientLogin)
                .setClientPasscode(clientPasscode)
                .setSystemLogin(systemLogin)
                .setSystemPasscode(systemPasscode)
                .setSystemHeartbeatSendInterval(10_000)
                .setSystemHeartbeatReceiveInterval(10_000)
                // required once you run more than one app instance:
                .setUserDestinationBroadcast("/topic/unresolved-user-dest")
                .setUserRegistryBroadcast("/topic/simp-user-registry");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain WebSocket for Flutter (no SockJS). Origin only applies to browsers.
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024)
                    .setSendBufferSizeLimit(512 * 1024)
                    .setSendTimeLimit(20_000)
                    .setTimeToFirstMessage(30_000); // drop sockets that never send CONNECT
    }
}
