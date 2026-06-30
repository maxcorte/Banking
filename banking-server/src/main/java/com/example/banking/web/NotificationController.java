package com.example.banking.web;

import com.example.banking.security.CurrentUser;
import com.example.banking.service.NotificationService;
import com.example.banking.web.dto.NotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        UUID userId = currentUser.require().getId();
        return notifications.list(userId).stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount() {
        return new UnreadCount(notifications.unreadCount(currentUser.require().getId()));
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead() {
        notifications.markAllRead(currentUser.require().getId());
    }

    public record UnreadCount(long count) {}
}
