package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints",
        uniqueConstraints = @UniqueConstraint(columnNames = {"productId", "reporter"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;
    private String content;
    private LocalDateTime createdAt;
    private String reporter;
    private String country;
    private Integer counter;
}
