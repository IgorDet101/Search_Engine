import DBEntity.Field;
import DBEntity.Page;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.HashSet;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

public class Main {
    private static final String URL = "https://et-cetera.ru/mobile";
    public static void main(String[] args) {
        new ForkJoinPool().invoke(new SiteCrawler(URL, "/"));

        String text = "компания банк насос шесть кот";
        Search_engine.search(text);

        DBConnection.getNewSession().getSessionFactory().close();
//        Session session = DBConnection.getSessionFactory().openSession();
//        Transaction transaction = session.beginTransaction();
//        Page page = (Page) DBConnection.getElementsByParameter("Page", "path", "/performance/starshaya_sestra/").getSingleResult();
//        System.out.println(page.getId());
    }
}
