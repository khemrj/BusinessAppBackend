package com.example.firstapp.config;



import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.example.firstapp.security.JwtService;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String EXP_ATTR = "jwtExpMillis";

    // Add /topic/group.{id} style rules here (with a membership check) when you build groups.
    private static final Set<String> ALLOWED_SUBSCRIPTIONS = Set.of(
            "/user/queue/messages",
            "/user/queue/errors",
            "/topic/announcements");

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() == null) {
            return message; // heartbeats etc.
        }

        switch (acc.getCommand()) {
            case CONNECT -> authenticate(acc);
            case SEND -> {
                requireLiveSession(acc);
                String dest = acc.getDestination();
                if (dest == null || !dest.startsWith("/app/")) {
                    throw new AccessDeniedException("Clients may only send to /app/**");
                }
            }
            case SUBSCRIBE -> {
                requireLiveSession(acc);
                String dest = acc.getDestination();
                if (dest == null || !ALLOWED_SUBSCRIPTIONS.contains(dest)) {
                    throw new AccessDeniedException("Subscription not allowed: " + dest);
                }
            }
            default -> { }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor acc) {
        String header = acc.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing bearer token");
        }
        Map.Entry<Long, java.util.Date> parsed = jwtService.extractClaim(
            header.substring(7),
            claims -> Map.entry(claims.get("userId", Long.class), claims.getExpiration()));

        // Principal name = user id. This is what convertAndSendToUser() targets.
        // Security note: authorize conversation membership in handlers; /app/** access alone is not sufficient.
        acc.setUser(new UsernamePasswordAuthenticationToken(
            parsed.getKey().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Map<String, Object> attrs = acc.getSessionAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No session attributes");
        }
        attrs.put(EXP_ATTR, parsed.getValue().getTime());
    }

    /** Long-lived sockets outlive JWTs: once expired, the ERROR frame closes the socket and the client reconnects with a fresh token. */
    private void requireLiveSession(StompHeaderAccessor acc) {
        Principal user = acc.getUser();
        Map<String, Object> attrs = acc.getSessionAttributes();
        Object exp = attrs == null ? null : attrs.get(EXP_ATTR);
        if (user == null || !(exp instanceof Long)) {
            throw new BadCredentialsException("Not authenticated");
        }
        if (System.currentTimeMillis() >= (Long) exp) {
            throw new CredentialsExpiredException("Token expired");
        }
    }
}