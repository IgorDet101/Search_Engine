import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Lemmatizer {
    private static LuceneMorphology luceneMorphology;
    private final Map<String, Integer> lemmas = new HashMap<>();

    public Lemmatizer() {
    }
    private static void initialMorphology() throws IOException {
        if (luceneMorphology == null) {
            luceneMorphology = new RussianLuceneMorphology();
        }
    }

    public Map<String, Integer> getLemmas(String sourceText) {
        try {
            initialMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] wordArray = sourceText
                .replaceAll("[^а-яА-Я\\s]+", " ")
                .trim()
                .toLowerCase()
                .split("\\s+");

        for (String word : wordArray) {
            luceneMorphology.getMorphInfo(word).forEach(x -> {
                String[] var = x.split("\\|");
                String normalWord = var[0];
                String info = var[1];
                String partOfSpeech = info.split("\\s+")[1];
                if (!isBasic(partOfSpeech)) {
                    Integer count = lemmas.get(normalWord);
                    lemmas.put(normalWord, count == null ? 1 : count + 1);
                }
            });
        }

        return lemmas;
    }

    private static boolean isBasic(String str) {
        try {
            PartsOfSpeech.valueOf(str);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }
}

enum PartsOfSpeech {
    СОЮЗ,
    МЕЖД,
    ПРЕДЛ,
    ЧАСТ
}
