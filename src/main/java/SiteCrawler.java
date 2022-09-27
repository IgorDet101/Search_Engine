import DBEntity.Field;
import DBEntity.Index;
import DBEntity.Lemma;
import DBEntity.Page;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class SiteCrawler extends RecursiveAction {
    private final String rootUrl;
    private final String fullUrl;
    private final String shortUrl;
    private int connectionStatusCode;
    private Session session;

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

        Page page = new Page(shortUrl, connectionStatusCode, doc.html());

        session = DBConnection.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(page);
        transaction.commit();

        createSubTask(doc);
        if (connectionStatusCode == 200) {
            List<Field> fieldList = session.createQuery("From Field", Field.class).getResultList();
            for (Field field : fieldList) {
                String html = doc.selectFirst(field.getSelector()).html();
                Map<String, Integer> lemmas = GetLemmas.getLemmas(html);

                for (String word : lemmas.keySet()) {
                    Lemma lemma = (Lemma) DBConnection.getElementsByParameter("Lemma", "lemma", word, session).uniqueResult();
                    if (lemma != null) {
                        lemma.setFrequency(lemma.getFrequency() + 1);
                    } else {
                        lemma = new Lemma(word, 1);
                    }

                    //create index or change his rank
                    int pageId = page.getId();
                    int lemmaId = lemma.getId();
                    Index index = session.createQuery("From Index Where page_id = :pageId " +
                                    "AND lemma_id = :lemmaId", Index.class)
                            .setParameter("pageId", pageId)
                            .setParameter("lemmaId", lemmaId)
                            .uniqueResult();
                    float rank = field.getWeight() * lemmas.get(word);
                    if (index != null) {
                        rank += index.getRank();
                        index.setRank(rank);
                    } else {
                        index = new Index(pageId, lemmaId, rank);
                    }

                    transaction = session.beginTransaction();
                    session.save(lemma);
                    session.save(index);
                    transaction.commit();
                }
            }
        }
        session.close();
    }

    private void createSubTask(Document doc) {
        Set<String> urlSet = null;
        if (doc != null) {
            urlSet = doc.select("a").stream()
                    .map(e -> e.attr("abs:href"))
                    .filter(e -> e.startsWith(rootUrl))
                    .filter(e -> e.endsWith("/") | (e.endsWith("html")))
                    .map(e -> e.substring(rootUrl.length()))
                    .filter(e -> {
//                        CriteriaBuilder builder = session.getCriteriaBuilder();
//                        CriteriaQuery<Page> query = builder.createQuery(Page.class);
//                        Root<Page> root = query.from(Page.class);
//                        query.select(root).where(builder.like(root.get("path"), e));
//                        Page page = session.createQuery(query).getSingleResultOrNull();


                        Page page = (Page) DBConnection.getElementsByParameter("Page", "path", e, session)
                                .uniqueResult();
                        return page == null;
                    })
                    .collect(Collectors.toSet());
        }

        if (urlSet != null && urlSet.size() > 1) {
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
