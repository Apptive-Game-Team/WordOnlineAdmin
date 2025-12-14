package com.wordonline.admin.entity.server;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String protocol;
    private String domain;
    private Integer port;

    @Enumerated(EnumType.STRING)
    private ServerType type;

    @Setter
    @Enumerated(EnumType.STRING)
    private ServerState state;

    public String getUrl() {
        return String.format("%s://%s:%d", protocol, domain, port);
    }

    public Server(String protocol, String domain, int port, ServerType serverType, ServerState state) {
        this(null, protocol, domain, port, serverType, state);
    }
}
