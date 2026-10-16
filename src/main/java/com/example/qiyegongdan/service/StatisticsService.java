package com.example.qiyegongdan.service;

import com.example.qiyegongdan.repository.TicketRepository;
import com.example.qiyegongdan.enums.TicketPriority;
import com.example.qiyegongdan.enums.TicketStatus;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 多维度统计报表
 */
@Service
public class StatisticsService {

    private final TicketRepository ticketRepository;

    public StatisticsService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Dashboard dashboard() {
        Dashboard d = new Dashboard();
        d.total = ticketRepository.totalCount();
        for (TicketStatus s : TicketStatus.values()) {
            d.byStatus.add(new NameValue(s.getLabel(), ticketRepository.countByStatus(s)));
        }
        for (TicketPriority p : TicketPriority.values()) {
            d.byPriority.add(new NameValue(p.getLabel(), ticketRepository.countByPriority(p)));
        }
        d.open = ticketRepository.countByStatus(TicketStatus.OPEN);
        d.assigned = ticketRepository.countByStatus(TicketStatus.ASSIGNED);
        d.inProgress = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        d.resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        d.closed = ticketRepository.countByStatus(TicketStatus.CLOSED);
        d.canceled = ticketRepository.countByStatus(TicketStatus.CANCELED);
        return d;
    }

    public List<NameValue> trend(int days) {
        int n = days <= 0 ? 7 : Math.min(days, 90);
        List<NameValue> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // 用历史累计总数的差值近似每日新增（此处简化为当日新增计数）
        for (int i = n - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            list.add(new NameValue(day.toString(), countCreatedOn(day)));
        }
        return list;
    }

    private long countCreatedOn(LocalDate day) {
        return ticketRepository.countByCreatedAtBetween(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    @Getter
    public static class Dashboard {
        public long total;
        public long open;
        public long assigned;
        public long inProgress;
        public long resolved;
        public long closed;
        public long canceled;
        public List<NameValue> byStatus = new ArrayList<>();
        public List<NameValue> byPriority = new ArrayList<>();
    }

    public record NameValue(String name, long value) {}
}