package com.example.qiyegongdan.config;

import com.example.qiyegongdan.entity.CustomFieldDefinition;
import com.example.qiyegongdan.entity.TicketType;
import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.enums.FieldType;
import com.example.qiyegongdan.enums.RoleName;
import com.example.qiyegongdan.repository.CustomFieldDefinitionRepository;
import com.example.qiyegongdan.repository.TicketTypeRepository;
import com.example.qiyegongdan.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 数据初始化：创建默认角色账号、工单类型与自定义字段定义
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TicketTypeRepository typeRepository;
    private final CustomFieldDefinitionRepository fieldRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository, TicketTypeRepository typeRepository,
                      CustomFieldDefinitionRepository fieldRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.seed-admin-username}") String adminUsername,
                      @Value("${app.seed-admin-password}") String adminPassword) {
        this.userRepository = userRepository;
        this.typeRepository = typeRepository;
        this.fieldRepository = fieldRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDemoUsers();
        seedTicketTypes();
    }

    private void ensureUser(String username, String password, String realName, String dept, RoleName... roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User u = new User(username, passwordEncoder.encode(password), realName);
        u.setDepartment(dept);
        u.setRoles(Set.of(roles));
        userRepository.save(u);
        log.info("已创建用户: {}", username);
    }

    private void seedAdmin() {
        ensureUser(adminUsername, adminPassword, "系统管理员", "信息技术部", RoleName.ADMIN);
    }

    private void seedDemoUsers() {
        ensureUser("manager", "123456", "张经理", "信息技术部", RoleName.MANAGER);
        ensureUser("agent1", "123456", "李处理员", "客服中心", RoleName.AGENT);
        ensureUser("agent2", "123456", "王处理员", "客服中心", RoleName.AGENT);
        ensureUser("user1", "123456", "赵员工", "市场部", RoleName.REPORTER);
    }

    private void seedTicketTypes() {
        if (typeRepository.findByName("软件故障").isPresent()) {
            return;
        }
        TicketType software = new TicketType();
        software.setName("软件故障");
        software.setDescription("业务系统软件故障上报");
        TicketType hardware = new TicketType();
        hardware.setName("硬件故障");
        hardware.setDescription("电脑、网络设备等硬件故障");
        TicketType service = new TicketType();
        service.setName("服务申请");
        service.setDescription("账号开通、权限申请等服务请求");
        TicketType requirement = new TicketType();
        requirement.setName("需求变更");
        requirement.setDescription("业务功能需求变更");
        typeRepository.saveAll(List.of(software, hardware, service, requirement));

        addField(software, "softwareName", "涉及软件系统", FieldType.SELECT, true, "ERP,OA,HR,CRM,其他");
        addField(software, "impact", "影响范围", FieldType.SELECT, false, "个人,部门,全公司");
        addField(hardware, "deviceType", "设备类型", FieldType.SELECT, true, "台式机,笔记本,打印机,交换机,其他");
        addField(service, "applyType", "申请类型", FieldType.SELECT, true, "账号开通,权限申请,邮箱开通");
        log.info("已初始化工单类型及自定义字段");
    }

    private void addField(TicketType type, String key, String label, FieldType dataType, boolean required, String options) {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setTicketType(type);
        def.setFieldKey(key);
        def.setLabel(label);
        def.setDataType(dataType);
        def.setRequired(required);
        def.setOptions(options);
        fieldRepository.save(def);
    }
}