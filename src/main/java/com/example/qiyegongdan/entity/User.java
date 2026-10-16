package com.example.qiyegongdan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统用户
 */
@Entity
@Table(name = "sys_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录名 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 密码（BCrypt 加密） */
    @Column(nullable = false)
    private String password;

    /** 姓名 */
    @Column(nullable = false, length = 50)
    private String realName;

    private String email;

    private String phone;

    /** 所属部门 */
    private String department;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 角色集合（一个用户可多角色） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sys_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30)
    private Set<com.example.qiyegongdan.enums.RoleName> roles = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public User(String username, String password, String realName) {
        this.username = username;
        this.password = password;
        this.realName = realName;
    }
}