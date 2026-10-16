package com.example.qiyegongdan.service;

import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.exception.BusinessException;
import com.example.qiyegongdan.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 自动分配引擎：支持轮询和负载均衡两种策略
 */
@Service
public class AssignmentService {

    private final UserService userService;
    private final TicketRepository ticketRepository;

    public AssignmentService(UserService userService, TicketRepository ticketRepository) {
        this.userService = userService;
        this.ticketRepository = ticketRepository;
    }

    public User autoAssign(String strategy) {
        List<User> agents = userService.listAgents();
        if (agents.isEmpty()) {
            throw new BusinessException("没有可用的处理员(AGENT)，无法自动分配");
        }
        String upper = strategy == null ? "" : strategy.toUpperCase();
        return switch (upper) {
            case "LEAST_LOAD" -> leastLoad(agents);
            case "ROUND_ROBIN", "" -> roundRobin(agents);
            default -> throw new BusinessException("未知的分配策略: " + strategy);
        };
    }

    /** 轮询：从负载最低的处理员中轮询选择 */
    private User roundRobin(List<User> agents) {
        User min = agents.stream()
                .min(Comparator.comparingLong(u -> ticketRepository.countByStatusInAndAssigneeId(
                        List.of(com.example.qiyegongdan.enums.TicketStatus.ASSIGNED,
                                com.example.qiyegongdan.enums.TicketStatus.IN_PROGRESS), u.getId())))
                .orElseThrow(() -> new BusinessException("无可用处理员"));
        return min;
    }

    /** 负载均衡：选择未完成工单最少的处理员；若相同选取处理中的最少者 */
    private User leastLoad(List<User> agents) {
        return agents.stream()
                .min(Comparator
                        .comparingLong((User u) -> ticketRepository.countByStatusInAndAssigneeId(
                                List.of(com.example.qiyegongdan.enums.TicketStatus.OPEN,
                                        com.example.qiyegongdan.enums.TicketStatus.ASSIGNED,
                                        com.example.qiyegongdan.enums.TicketStatus.IN_PROGRESS), u.getId()))
                        .thenComparingLong(u -> ticketRepository.countByStatusAndAssigneeId(
                                com.example.qiyegongdan.enums.TicketStatus.IN_PROGRESS, u.getId())))
                .orElseThrow(() -> new BusinessException("无可用处理员"));
    }
}