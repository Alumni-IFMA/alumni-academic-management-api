package com.alumni.academic_management_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_messages")
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @NotNull
    @Column(name = "name", nullable = false)
    @ToString.Include
    private String name;

    @NotNull
    @Column(name = "email", nullable = false)
    @ToString.Include
    private String email;

    @NotNull
    @Column(name = "subject", nullable = false)
    @ToString.Include
    private String subject;

    @NotNull
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(name = "resolved", nullable = false)
    @ToString.Include
    private boolean resolved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SupportMessage)) {
            return false;
        }
        return id != null && id.equals(((SupportMessage) o).id);
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
