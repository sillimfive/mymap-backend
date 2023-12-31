package com.sillimfive.mymap.domain;

import com.sillimfive.mymap.domain.roadmap.RoadMapLike;
import com.sillimfive.mymap.domain.roadmap.RoadMapReply;
import com.sillimfive.mymap.domain.users.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.lang.Nullable;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Alarm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private RoadMapReply roadMapReply;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "like_id")
    private RoadMapLike roadMapLike;

    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;
    private boolean deleteFlag;
    private boolean readFlag;

    @Builder
    protected Alarm(AlarmType alarmType, boolean deleteFlag, boolean readFlag, User user,RoadMapReply roadMapReply,RoadMapLike roadMapLike ){
        this.alarmType = alarmType;
        this.deleteFlag = deleteFlag;
        this.readFlag = readFlag;
        this.user = user;
        this.roadMapReply = roadMapReply;
        this.roadMapLike = roadMapLike;
    }

    public static Alarm createAlarm(AlarmType alarmType, boolean deleteFlag, boolean readFlag, User user,RoadMapReply roadMapReply,RoadMapLike roadMapLike) {

        return Alarm.builder()
                .alarmType(alarmType)
                .deleteFlag(deleteFlag)
                .readFlag(readFlag)
                .user(user)
                .roadMapReply(roadMapReply)
                .roadMapLike(roadMapLike)
                .build();
    }
}
