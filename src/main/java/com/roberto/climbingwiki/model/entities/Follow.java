package com.roberto.climbingwiki.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "follows")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Follow {

    @EmbeddedId
    private FollowId id = new FollowId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followedId")
    @JoinColumn(name = "followed_id")
    private User followed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
