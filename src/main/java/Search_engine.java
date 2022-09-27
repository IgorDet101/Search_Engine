import DBEntity.Lemma;
import org.hibernate.Session;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Search_engine {

    public static void search(String searchText){
        Session session = DBConnection.getSessionFactory().openSession();
        Comparator<Lemma> comparator = Comparator.comparing(obj -> obj.getFrequency());
        Set<Lemma> searchLemmas = null;
        searchLemmas = GetLemmas.getLemmas(searchText).keySet().stream()
                .map(e -> (Lemma) DBConnection.getElementsByParameter("Lemma", "lemma", e, session).uniqueResult())
                .collect(Collectors.toSet());
        System.out.println(searchLemmas);
    }
}
