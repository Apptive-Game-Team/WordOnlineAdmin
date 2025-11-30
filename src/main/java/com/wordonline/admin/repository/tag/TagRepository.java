package com.wordonline.admin.repository.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.tag.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

}
