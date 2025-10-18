package com.wordonline.admin.entity.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Table(name = "magics")
public class Magic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String name;

    @OneToMany(mappedBy = "magic")
    private List<MagicCard> magicCards = new ArrayList<>();

    public void addMagicCard(MagicCard magicCard) {
        magicCards.add(magicCard);
    }

    public boolean hasMagicCard(long cardId) {
        return magicCards.stream()
                .anyMatch(magicCard -> magicCard.getId() == cardId);
    }

    public MagicCard removeOneMagicCard(long cardId) {
        Optional<MagicCard> magicCardOptional = magicCards.stream()
                .filter(magicCard1 -> magicCard1.getCard().getId() == cardId)
                .findAny();

        if (magicCardOptional.isEmpty()) {
            return null;
        }

        magicCards.remove(magicCardOptional.get());

        return magicCardOptional.get();
    }
}
