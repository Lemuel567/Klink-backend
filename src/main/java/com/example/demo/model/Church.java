package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "churches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Church {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "church_name", nullable = false)
    private String churchName;

    @Column(name = "location")
    private String location;

    @Column(name = "denomination")
    private String denomination;

    @Column(name = "church_code", unique = true, nullable = false)
    private String churchCode;

    @Column(name = "welfare_amount", precision = 19, scale = 2)
    private BigDecimal welfareAmount;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "photo_url")
    private String photoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
