package DBEntity;

import jakarta.persistence.*;
import jakarta.persistence.Index;

@Entity
@Table(name = "Lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    public Lemma() {
    }

    public Lemma(String lemma) {
        this.lemma = lemma;
        frequency = 1;
    }

    public void increaseFrequency(){
        frequency++;
    }
    public int getId() {
        return id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
