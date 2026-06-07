package com.nova.food.api.rest;

import com.nova.food.app.dto.response.ResponseDto;
import com.nova.food.app.service.ResponseFactory;
import com.nova.food.domain.events.outbox.OutboxOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/outbox")
public class OutboxOpsController {

    private final OutboxOpsService outboxOpsService;
    private final ResponseFactory responseFactory;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto summary() {
        return responseFactory.success(outboxOpsService.snapshot());
    }

    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto failed() {
        return responseFactory.success(outboxOpsService.listFailed());
    }

    @PostMapping("/{outboxId}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseDto replay(@PathVariable UUID outboxId) {
        return responseFactory.success(outboxOpsService.replayFailed(outboxId));
    }
}
