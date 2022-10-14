import DBEntity.*;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteCrawler extends RecursiveAction {
    private static final Set<String> passedAddressSet = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> addedLemmas = Collections.synchronizedSet(new HashSet<>());
    private final String rootUrl;
    private final String fullUrl;
    private final String shortUrl;
    private int connectionStatusCode;
    private final Lemmatizer lemmatizer = new Lemmatizer();

    public SiteCrawler(String rootUrl, String shortUrl) {
        this.rootUrl = rootUrl;
        fullUrl = rootUrl + shortUrl;
        this.shortUrl = shortUrl;
    }

    private Connection getConnection() {
        Connection connection = null;
        try {
            Thread.sleep(500);
            connection = Jsoup.connect(fullUrl)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT" +
                            "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com");
            connectionStatusCode = connection.execute().statusCode();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return connection;
    }

    @Override
    protected void compute() {
        Connection connection = getConnection();

        Document doc = null;
        try {
            doc = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (doc == null) {
            return;
        }

        Page page = new Page(shortUrl, connectionStatusCode, doc.html());

        Session session = DBConnection.getNewSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();

        session.beginTransaction();
        session.persist(page);
        session.getTransaction().commit();

        createSubTask(doc);
        if (connectionStatusCode == 200) {
            List<Field> fieldList = session.createQuery("From Field", Field.class).getResultList();
            for (Field field : fieldList) {
                String html = Objects.requireNonNull(doc.selectFirst(field.getSelector())).html();
                Map<String, Integer> lemmas = lemmatizer.getLemmas(html);
                Set<String> keySet = new HashSet<>(lemmas.keySet());

                for (String word : keySet) {
                    Lemma lemma = null;

                    if (addedLemmas.contains(word)) {
                            CriteriaQuery<Lemma> query = builder.createQuery(Lemma.class);
                            Root<Lemma> root = query.from(Lemma.class);
                            query.where(builder.equal(root.get("lemma"), word));
                        try {
                            lemma = session.createQuery(query).getSingleResult();
                        } catch (NoResultException ex) {
                            try {
                                wait(100);
                                lemma = session.createQuery(query).getSingleResult();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    } else {
                        addedLemmas.add(word);
                    }


                    session.beginTransaction();
                    if (lemma != null) {
                        lemma.increaseFrequency();
                        session.merge(lemma);
                    } else {
                        lemma = new Lemma(word);
                        session.persist(lemma);
                    }
                    session.getTransaction().commit();

                    //create index or change his rank
                    int pageId = page.getId();
                    int lemmaId = lemma.getId();
                    float rank = field.getWeight() * lemmas.get(word);

                    CriteriaQuery<Index> query = builder.createQuery(Index.class);
                    Root<Index> root = query.from(Index.class);
                    query.where(builder.and(builder.equal(root.get("pageId"), pageId),
                            builder.equal(root.get("lemmaId"), lemmaId)));
                    Index index = session.createQuery(query).getSingleResultOrNull();

                    session.beginTransaction();
                    if (index != null) {
                        rank += index.getRank();
                        index.setRank(rank);
                        session.merge(index);
                    } else {
                        index = new Index(pageId, lemmaId, rank);
                        session.persist(index);
                    }
                    session.getTransaction().commit();
                }
            }
        }
        session.close();
    }

    private synchronized void createSubTask(Document doc) {
        Set<String> urlSet = doc.select("a").stream()
                .map(e -> e.attr("abs:href"))
                .filter(e -> e.startsWith(rootUrl))
                .filter(e -> e.endsWith("/") | (e.endsWith("html")))
                .map(e -> e.substring(rootUrl.length()))
                .filter(e -> !passedAddressSet.contains(e))
                .collect(Collectors.toSet());


        if (urlSet.size() >= 1) {
            passedAddressSet.addAll(urlSet);
            List<SiteCrawler> taskList = new ArrayList<>();
            for (String shortUrl : urlSet) {
                taskList.add(new SiteCrawler(rootUrl, shortUrl));
            }
            ForkJoinTask.invokeAll(taskList);
            for (SiteCrawler task : taskList) {
                task.compute();
            }
        }
    }


}
