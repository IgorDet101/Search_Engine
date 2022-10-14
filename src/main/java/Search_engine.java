import DBEntity.Lemma;
import org.hibernate.Session;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class Search_engine {

    public static void search(String searchText){
        Session session = DBConnection.getNewSession();
        Comparator<Lemma> comparator = Comparator.comparing(obj -> obj.getFrequency());
        Set<Lemma> searchLemmas = null;
        Lemmatizer lemmatizer = new Lemmatizer();
        searchLemmas = lemmatizer.getLemmas(searchText).keySet().stream()
                .map(e -> (Lemma) DBConnection.getElementsByParameter("Lemma", "lemma", e, session).getSingleResult())
                .collect(Collectors.toSet());
        System.out.println(searchLemmas);
    }
}
