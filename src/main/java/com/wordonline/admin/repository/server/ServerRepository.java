package com.wordonline.admin.repository.server;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findAllByTypeAndState(ServerType type, ServerState state);
}
