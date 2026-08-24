package com.wordonline.admin.entity.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
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

    // Both columns are NOT NULL in the database and cast_type additionally carries a CHECK.
    // They are plain strings here for the same reason Adventure.accessType is: the lobby server
    // reads them as lowercase text, which an @Enumerated(EnumType.STRING) mapping would not produce.
    @Setter
    @Column(name = "cast_type", length = 10, nullable = false)
    private String castType;

    @Setter
    @Column(name = "access_type", length = 10, nullable = false)
    private String accessType;

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
