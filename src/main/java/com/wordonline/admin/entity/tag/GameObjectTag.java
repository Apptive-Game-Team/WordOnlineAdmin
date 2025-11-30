package com.wordonline.admin.entity.tag;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.wordonline.admin.entity.parameter.GameObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_object_tags")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameObjectTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_object_id")
    private GameObject gameObject;

    @ManyToOne
    @JoinColumn(name = "tag_id")
    private Tag tag;
}
