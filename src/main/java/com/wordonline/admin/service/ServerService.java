package com.wordonline.admin.service;

import com.wordonline.admin.entity.server.Server;
import com.wordonline.admin.repository.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;

    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }
}
