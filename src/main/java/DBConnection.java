import DBEntity.Field;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;


public class DBConnection {
    private static SessionFactory sessionFactory = null;

    public static Session getNewSession() {
        if (sessionFactory == null) {
            final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml")
                    .build();
            sessionFactory = new MetadataSources(serviceRegistry).buildMetadata().buildSessionFactory();
            Session session = sessionFactory.openSession();

            Transaction transaction = session.beginTransaction();
            session.persist(new Field("title", "title", 1));
            session.persist(new Field("body", "body", 0.8F));
            transaction.commit();
            session.close();
        }
        return sessionFactory.openSession();
    }

    public static SelectionQuery getElementsByParameter(String entity, String parameterName, String value, Session session) {
        String queryBuilder = "From " + entity + " Where " + parameterName +
                " = '" + value + "'";
        Transaction transaction = session.beginTransaction();
        SelectionQuery query = session.createSelectionQuery(queryBuilder);
        transaction.commit();

        return query;
    }
}
