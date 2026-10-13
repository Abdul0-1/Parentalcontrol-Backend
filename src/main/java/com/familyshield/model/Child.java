package com.familyshield.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "children")
public class Child {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    private String pairingCode;
    private LocalDateTime pairingExpiresAt;

    public Child() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }

    public String getPairingCode() { return pairingCode; }
    public void setPairingCode(String pairingCode) { this.pairingCode = pairingCode; }

    public LocalDateTime getPairingExpiresAt() { return pairingExpiresAt; }
    public void setPairingExpiresAt(LocalDateTime pairingExpiresAt) { this.pairingExpiresAt = pairingExpiresAt; }
}
