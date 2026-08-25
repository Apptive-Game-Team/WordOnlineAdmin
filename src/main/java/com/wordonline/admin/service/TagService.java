package com.wordonline.admin.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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

import jakarta.persistence.PersistenceException;
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
        String name = tagRequestDto.name();
        rejectExistingName(name);
        Tag savedTag = saveWithUniqueName(tagRequestDto.toEntity(), name);
        return savedTag.getId();
    }

    public void putTag(TagDto tagDto) {
        String name = tagDto.name();
        rejectExistingNameOnOtherTag(tagDto.id(), name);
        saveWithUniqueName(tagDto.toEntity(), name);
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

    private void rejectExistingName(String name) {
        if (tagRepository.existsByName(name)) {
            throw new IllegalArgumentException(duplicateMessage(name));
        }
    }

    private void rejectExistingNameOnOtherTag(long tagId, String name) {
        if (tagRepository.existsByNameAndIdNot(name, tagId)) {
            throw new IllegalArgumentException(duplicateMessage(name));
        }
    }

    private Tag saveWithUniqueName(Tag tag, String name) {
        try {
            // saveAndFlush so the constraint fails here instead of at commit, where this catch
            // could no longer turn it into a readable message.
            return tagRepository.saveAndFlush(tag);
        } catch (DataIntegrityViolationException | PersistenceException exception) {
            // The UNIQUE index on tags(name) is the last line of defence when two admins take the
            // same name at once; without this the page would show a raw SQL failure.
            throw new IllegalArgumentException(duplicateMessage(name), exception);
        }
    }

    private String duplicateMessage(String name) {
        return "Tag name already exists: " + name
                + "; tag names must be unique because counter rules join tags by name";
    }
}
