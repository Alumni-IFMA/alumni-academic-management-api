package com.alumni.academic_management_api.entity;

import com.alumni.academic_management_api.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "users")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    @ToString.Include
    private String name;

    @NotNull
    @Column(name = "cpf", nullable = false, unique = true)
    private String cpf;

    @NotNull
    @Size(min = 5, max = 200)
    @Column(name = "email", nullable = false, unique = true)
    @ToString.Include
    private String email;

    @JsonIgnore
    @NotNull
    @Size(min = 8)
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "bio")
    @ToString.Include
    private String bio;

    @Column(name = "profile_picture_url")
    @ToString.Include
    private String profilePictureUrl;

    @Column(name = "linkedin_url")
    @ToString.Include
    private String linkedinUrl;

    @Column(name = "portfolio_url")
    @ToString.Include
    private String portfolioUrl;

    @Column(name = "current_position")
    @ToString.Include
    private String currentPosition;

    @Column(name = "account_status", nullable = false)
    @ToString.Include
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @ManyToOne
    private AcademicProfile academicProfile;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        return id != null && id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
