package com.example.qiyegongdan.service;

import com.example.qiyegongdan.dto.*;
import com.example.qiyegongdan.entity.*;
import com.example.qiyegongdan.enums.RoleName;
import com.example.qiyegongdan.enums.TicketPriority;
import com.example.qiyegongdan.enums.TicketStatus;
import com.example.qiyegongdan.exception.BusinessException;
import com.example.qiyegongdan.exception.ResourceNotFoundException;
import com.example.qiyegongdan.repository.*;
import com.example.qiyegongdan.security.SecurityUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketLogRepository logRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketTypeService typeService;
    private final UserService userService;
    private final AssignmentService assignmentService;
    private final AuditLogService auditLogService;

    public TicketService(TicketRepository ticketRepository, TicketCommentRepository commentRepository,
                         TicketLogRepository logRepository, TicketAttachmentRepository attachmentRepository,
                         TicketTypeService typeService, UserService userService,
                         AssignmentService assignmentService, AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.logRepository = logRepository;
        this.attachmentRepository = attachmentRepository;
        this.typeService = typeService;
        this.userService = userService;
        this.assignmentService = assignmentService;
        this.auditLogService = auditLogService;
    }

    // ============ 创建 ============
    @Transactional
    public TicketDTO create(TicketCreateRequest req, User reporter) {
        TicketType type = typeService.getEntity(req.typeId());
        validateCustomFields(type.getId(), req.customFields());

        Ticket ticket = new Ticket();
        ticket.setNumber(generateNumber());
        ticket.setTitle(req.title());
        ticket.setDescription(req.description());
        ticket.setPriority(req.priority() == null ? TicketPriority.MEDIUM : req.priority());
        ticket.setType(type);
        ticket.setReporter(reporter);
        ticket.setCustomFields(req.customFields() == null ? Map.of() : req.customFields());

        if (req.autoAssign()) {
            User assignee = assignmentService.autoAssign(null);
            ticket.setAssignee(assignee);
            ticket.setStatus(TicketStatus.ASSIGNED);
            ticket.setAssignStrategy("ROUND_ROBIN");
            ticket = ticketRepository.save(ticket);
            addLog(ticket, reporter, "自动分配", "系统自动分配到: " + assignee.getRealName());
        } else {
            if (req.assigneeId() == null) {
                throw new BusinessException("手动分配模式下必须指定处理人");
            }
            User assignee = userService.getEntity(req.assigneeId());
            ticket.setAssignee(assignee);
            ticket.setStatus(TicketStatus.ASSIGNED);
            ticket.setAssignStrategy("MANUAL");
            ticket = ticketRepository.save(ticket);
            addLog(ticket, reporter, "手动分配", "分配给: " + assignee.getRealName());
        }
        return toDTO(ticket);
    }

    // ============ 分配（重新分配） ============
    @Transactional
    public TicketDTO reassign(Long ticketId, AssignRequest req, User operator) {
        Ticket ticket = getEntity(ticketId);
        if (ticket.getStatus().equals(TicketStatus.CLOSED) || ticket.getStatus().equals(TicketStatus.CANCELED)) {
            throw new BusinessException("已关闭或已取消的工单不能重新分配");
        }
        User assignee = userService.getEntity(req.assigneeId());
        if (!assignee.getRoles().contains(RoleName.AGENT)) {
            throw new BusinessException("只能分配给处理员(AGENT)角色用户");
        }
        ticket.setAssignee(assignee);
        if (ticket.getStatus().equals(TicketStatus.OPEN)) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        ticketRepository.save(ticket);
        addLog(ticket, operator, "重新分配", "分配给: " + assignee.getRealName());
        return toDTO(ticket);
    }

    // ============ 状态流转 ============
    @Transactional
    public TicketDTO changeStatus(Long ticketId, TicketUpdateStatusRequest req, User operator) {
        Ticket ticket = getEntity(ticketId);
        TicketStatus from = ticket.getStatus();
        TicketStatus to = req.toStatus();
        validateTransition(from, to, operator, ticket, req.remark());

        ticket.setStatus(to);
        if (to == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (to == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        ticketRepository.save(ticket);
        addLog(ticket, operator, from.name() + " -> " + to.name(),
                req.remark() == null ? to.getLabel() : req.remark());
        return toDTO(ticket);
    }

    // ============ 评论 ============
    @Transactional
    public TicketDTO addComment(Long ticketId, CommentRequest req, User author) {
        Ticket ticket = getEntity(ticketId);
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(req.content());
        comment.setInternal(req.internal());
        commentRepository.save(comment);
        addLog(ticket, author, "评论", req.internal() ? "[内部] " + req.content() : req.content());
        return toDTO(ticket);
    }

    // ============ 附件 ============
    @Transactional
    public TicketAttachment attach(Long ticketId, String filename, String storagePath, String contentType,
                                   long size, User uploader) {
        Ticket ticket = getEntity(ticketId);
        TicketAttachment att = new TicketAttachment();
        att.setTicket(ticket);
        att.setUploader(uploader);
        att.setFilename(filename);
        att.setStoragePath(storagePath);
        att.setContentType(contentType);
        att.setSize(size);
        att = attachmentRepository.save(att);
        addLog(ticket, uploader, "上传附件", filename);
        return att;
    }

    // ============ 查询 ============
    @Transactional(readOnly = true)
    public TicketDTO get(Long id) {
        Ticket ticket = getEntity(id);
        ensureVisible(ticket, SecurityUtils.requireCurrentUser());
        return toDTO(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketComment> comments(Long ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional(readOnly = true)
    public List<TicketAttachment> attachments(Long ticketId) {
        return attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId);
    }

    @Transactional(readOnly = true)
    public TicketDTO.PageResult search(TicketQueryRequest req, User currentUser) {
        int page = req.page() == null ? 0 : Math.max(0, req.page());
        int size = req.size() == null ? 10 : Math.min(50, Math.max(1, req.size()));
        String sortBy = req.sortBy() == null ? "createdAt" : req.sortBy();
        String order = req.order() == null ? "desc" : req.order();
        Sort sort = Sort.by(Sort.Direction.fromString(order.equalsIgnoreCase("asc") ? "asc" : "desc"), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Ticket> spec = buildSpec(req, currentUser);
        Page<Ticket> result = ticketRepository.findAll(spec, pageable);
        return TicketDTO.PageResult.of(result.map(this::toDTO));
    }

    private Specification<Ticket> buildSpec(TicketQueryRequest req, User currentUser) {
        boolean isAgent = currentUser.getRoles().contains(RoleName.AGENT);
        boolean isReporter = currentUser.getRoles().contains(RoleName.REPORTER)
                && !currentUser.getRoles().contains(RoleName.ADMIN)
                && !currentUser.getRoles().contains(RoleName.MANAGER)
                && !currentUser.getRoles().contains(RoleName.AGENT);

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req.keyword() != null && !req.keyword().isBlank()) {
                String kw = "%" + req.keyword().trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), kw), cb.like(root.get("number"), kw)));
            }
            if (req.status() != null) predicates.add(cb.equal(root.get("status"), req.status()));
            if (req.priority() != null) predicates.add(cb.equal(root.get("priority"), req.priority()));
            if (req.typeId() != null) predicates.add(cb.equal(root.get("type").get("id"), req.typeId()));
            if (req.assigneeId() != null) predicates.add(cb.equal(root.get("assignee").get("id"), req.assigneeId()));
            if (req.reporterId() != null) predicates.add(cb.equal(root.get("reporter").get("id"), req.reporterId()));

            if (req.createFrom() != null && !req.createFrom().isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        LocalDate.parse(req.createFrom(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()));
            }
            if (req.createTo() != null && !req.createTo().isBlank()) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                        LocalDate.parse(req.createTo(), DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1).atStartOfDay()));
            }
            // 数据权限
            if (isAgent) {
                predicates.add(cb.equal(root.get("assignee").get("id"), currentUser.getId()));
            } else if (isReporter) {
                predicates.add(cb.equal(root.get("reporter").get("id"), currentUser.getId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ============ 状态校验辅助 ============
    private void validateTransition(TicketStatus from, TicketStatus to, User operator,
                                    Ticket ticket, String remark) {
        boolean isAdminOrManager = operator.getRoles().contains(RoleName.ADMIN)
                || operator.getRoles().contains(RoleName.MANAGER);
        boolean isAssignee = ticket.getAssignee() != null
                && ticket.getAssignee().getId().equals(operator.getId());
        boolean isReporter = ticket.getReporter().getId().equals(operator.getId());

        if (!isAdminOrManager && !isAssignee && !isReporter) {
            throw new BusinessException("无权变更该工单状态");
        }
        if (to == null) {
            throw new BusinessException("目标状态不能为空");
        }
        // 仅允许流转到相邻的下一状态，保证流程完整性
        int fromOrd = from.ordinal();
        int toOrd = to.ordinal();
        boolean forwardOk = toOrd == fromOrd + 1;
        boolean resolves = from == TicketStatus.IN_PROGRESS && to == TicketStatus.RESOLVED;
        boolean closes = from == TicketStatus.RESOLVED && to == TicketStatus.CLOSED;
        boolean cancels = (from == TicketStatus.OPEN || from == TicketStatus.ASSIGNED)
                && to == TicketStatus.CANCELED;
        boolean selfNoop = from == to;
        if (from == TicketStatus.OPEN && to == TicketStatus.IN_PROGRESS && !isAdminOrManager) {
            throw new BusinessException("未分配的处理人不能直接开始处理");
        }
        if (!selfNoop && !forwardOk && !resolves && !closes && !cancels) {
            throw new BusinessException("非法状态流转: " + from.name() + " -> " + to.name());
        }
    }

    private void ensureVisible(Ticket ticket, User user) {
        if (user.getRoles().contains(RoleName.ADMIN) || user.getRoles().contains(RoleName.MANAGER)) {
            return;
        }
        if (user.getRoles().contains(RoleName.AGENT)) {
            if (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(user.getId())) {
                return;
            }
            if (ticket.getReporter().getId().equals(user.getId())) {
                return;
            }
            throw new BusinessException("无权查看该工单");
        }
        if (!ticket.getReporter().getId().equals(user.getId())) {
            throw new BusinessException("无权查看该工单");
        }
    }

    // ============ 其他辅助 ============
    private void addLog(Ticket ticket, User operator, String action, String detail) {
        TicketLog log = new TicketLog();
        log.setTicket(ticket);
        log.setOperator(operator);
        log.setAction(action);
        log.setDetail(detail);
        logRepository.save(log);
    }

    private String generateNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "TK-" + date + "-";
        // 基于当日已存在的最小编号递增，避免已删除工单或总数导致编号重复
        String last = ticketRepository.findTopByNumberPrefix(prefix + "%",
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "number")))
                .stream().map(Ticket::getNumber).findFirst().orElse(null);
        long seq = 1;
        if (last != null && last.length() > prefix.length()) {
            seq = Long.parseLong(last.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", seq % 100000);
    }

    public Ticket getEntity(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工单不存在"));
    }

    public TicketDTO toDTO(Ticket t) {
        List<TicketDTO.TicketLogDTO> logs = logRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()).stream()
                .map(l -> new TicketDTO.TicketLogDTO(l.getAction(), l.getDetail(),
                        l.getOperator() == null ? "系统" : l.getOperator().getRealName(), l.getCreatedAt()))
                .toList();
        return new TicketDTO(
                t.getId(), t.getNumber(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getPriority(),
                t.getType().getId(), t.getType().getName(),
                brief(t.getReporter()), t.getAssignee() == null ? null : brief(t.getAssignee()),
                t.getAssignStrategy(), t.getCustomFields(),
                t.getResolvedAt(), t.getClosedAt(), t.getCreatedAt(), t.getUpdatedAt(),
                logs
        );
    }

    private TicketDTO.UserBrief brief(User u) {
        return new TicketDTO.UserBrief(u.getId(), u.getUsername(), u.getRealName(), u.getDepartment());
    }

    private void validateCustomFields(Long typeId, Map<String, Object> values) {
        // 不论用户是否提交自定义字段，均须校验必填项，防止必填校验被绕过
        List<com.example.qiyegongdan.entity.CustomFieldDefinition> defs =
                typeService.fieldsOf(typeId).stream().map(d -> {
                    com.example.qiyegongdan.entity.CustomFieldDefinition f = new com.example.qiyegongdan.entity.CustomFieldDefinition();
                    f.setFieldKey(d.fieldKey());
                    f.setRequired(d.required());
                    return f;
                }).toList();
        for (com.example.qiyegongdan.entity.CustomFieldDefinition def : defs) {
            Object val = values == null ? null : values.get(def.getFieldKey());
            if (Boolean.TRUE.equals(def.getRequired()) && (val == null || val.toString().isBlank())) {
                throw new BusinessException("自定义字段必填: " + def.getFieldKey());
            }
        }
    }
}