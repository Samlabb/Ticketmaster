package com.ticket.security;

public record TokenPrincipal(String userId, String email, Role role) {
}