package com.ticket.user.infrastructure;

import com.ticket.user.domain.AdminAuditLog;
import com.ticket.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final String bootstrapEmail;

    public AdminBootstrapRunner(UserRepository userRepository,
                                AdminAuditLogRepository adminAuditLogRepository,
                                @Value("${app.admin.bootstrap-email:}") String bootstrapEmail) {
        this.userRepository = userRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Override
    public void run(String... args) {
        boolean anyAdminExists = userRepository.existsByRole(Role.ADMIN);
        if (anyAdminExists) {
            return;
        }

        if (bootstrapEmail == null || bootstrapEmail.isBlank()) {
            log.warn("В системе нет ни одного администратора, а ADMIN_BOOTSTRAP_EMAIL не задан. " +
                    "Задайте переменную окружения и перезапустите сервис, чтобы назначить первого администратора.");
            return;
        }

        userRepository.findByEmail(bootstrapEmail.trim()).ifPresentOrElse(user -> {
            user.setRole(Role.ADMIN);
            userRepository.save(user);
            adminAuditLogRepository.save(AdminAuditLog.bootstrapped(user.getId(), user.getEmail()));
            log.info("Пользователь {} назначен администратором при старте (bootstrap)", user.getEmail());
        }, () -> log.warn("ADMIN_BOOTSTRAP_EMAIL={} указан, но такой пользователь ещё не зарегистрирован. " +
                "Сначала зарегистрируйте этот email через обычную регистрацию, затем перезапустите сервис.", bootstrapEmail));
    }
}