package com.wordonline.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wordonline.admin.dto.tag.TagDto;
import com.wordonline.admin.dto.tag.TagRequestDto;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.tag.GameObjectTag;
import com.wordonline.admin.entity.tag.Tag;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.tag.GameObjectTagRepository;
import com.wordonline.admin.repository.tag.TagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final GameObjectRepository gameObjectRepository;
    private final GameObjectTagRepository gameObjectTagRepository;

    public List<GameObjectTag> findAllTags() {
        return gameObjectTagRepository.findAll();
    }

    public long addTag(TagRequestDto tagRequestDto) {
        Tag tag = tagRequestDto.toEntity();
        Tag savedTag = tagRepository.save(tag);
        return savedTag.getId();
    }

    public void putTag(TagDto tagDto) {
        Tag tag = tagDto.toEntity();
        tagRepository.save(tag);
    }

    public void addTagToGameObject(Long gameObjectId, Long tagId) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId)
                .orElseThrow(() -> new IllegalArgumentException("Game object not found"));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        GameObjectTag gameObjectTag = new GameObjectTag(null, gameObject, tag);

        gameObjectTagRepository.save(gameObjectTag);
    }

    public void removeTagFromGameObject(Long gameObjectId, Long tagId) {
        gameObjectTagRepository.deleteByGameObjectIdAndTagId(gameObjectId, tagId);
    }

    public void removeTag(long tagId) {
        tagRepository.deleteById(tagId);
    }
}
