package com.wordonline.admin.entity.server;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "servers")
@NoArgsConstructor
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String protocol;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ServerType type;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ServerState state;

    public String getUrl() {
        return protocol + "://" + domain + ":" + port;
    }
}
