package com.wordonline.admin.repository.server;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findAllByTypeAndState(ServerType type, ServerState state);

    /**
     * Targeted update instead of a whole-entity save: the game server rewrites the rest of the
     * row on every heartbeat, and saving a stale entity here would write those columns back.
     *
     * @return the number of rows changed; 0 when the id does not name a game server
     */
    @Modifying
    @Transactional
    @Query("update Server s set s.targetBotSessions = :targetBotSessions "
            + "where s.id = :id and s.type = com.wordonline.admin.entity.server.ServerType.GAME")
    int updateTargetBotSessions(@Param("id") long id, @Param("targetBotSessions") Integer targetBotSessions);
}
