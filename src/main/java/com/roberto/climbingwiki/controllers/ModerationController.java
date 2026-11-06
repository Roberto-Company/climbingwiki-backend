package com.roberto.climbingwiki.controllers;

import com.roberto.climbingwiki.dtos.UnverifiedItem;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import com.roberto.climbingwiki.services.ModerationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /** Moderators and admins only. */
    @GetMapping("/unverified")
    public List<UnverifiedItem> unverified(@AuthenticationPrincipal AuthenticatedUser principal) {
        return moderationService.unverifiedItems(principal);
    }
}
