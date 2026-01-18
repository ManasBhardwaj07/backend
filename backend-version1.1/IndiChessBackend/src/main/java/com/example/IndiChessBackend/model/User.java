package com.example.IndiChessBackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Size(min = 4, max = 50)
    @Column(name = "user_name", unique = true, nullable = false)
    private String username;

    @Email
    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;

    /**
     * WRITE_ONLY is CRITICAL:
     * - accepted from request JSON
     * - never returned in responses
     */
    @Size(min = 6, max = 512)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    private String pfpUrl;

    private String country;

    @Column(nullable = false)
    private Integer rating;
}
