package nlgrandtaskmanager.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(unique = true,nullable = false)
    private  String email;
    @Column(nullable = false)
    private  String passwordHash;
    @Column(nullable = false)
    private Instant createdAt;

}
