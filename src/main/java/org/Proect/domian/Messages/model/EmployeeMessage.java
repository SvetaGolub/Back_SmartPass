package org.Proect.domian.Messages.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_messages")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(nullable = false, length = 50)
    private String author;

    @Column(name = "message_text", nullable = false, length = 500)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
