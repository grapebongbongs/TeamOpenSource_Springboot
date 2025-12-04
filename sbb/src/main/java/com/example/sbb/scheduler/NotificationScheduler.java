package com.example.sbb.scheduler;

import com.example.sbb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    // 매일 09시, 21시에 하루 두 번 알림 대상 예약
    @Scheduled(cron = "0 0 9,21 * * *")
    public void scheduleDailySlots() {
        notificationService.scheduleTwiceDaily();
    }
}
