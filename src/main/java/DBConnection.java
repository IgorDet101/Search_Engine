import DBEntity.Field;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;


public class DBConnection {
    private static SessionFactory sessionFactory = null;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
            Metadata metadata = new MetadataSources(serviceRegistry).getMetadataBuilder().build();
            sessionFactory = metadata.buildSessionFactory();
            Session session = sessionFactory.openSession();

            Transaction transaction = session.beginTransaction();
            session.save(new Field("title", "title", 1));
            session.save(new Field("body", "body", 0.8F));
            transaction.commit();
        }
        return sessionFactory;
    }

    public static Query getElementsByParameter(String entity, String parameterName, String value, Session session) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("From ").append(entity).append(" Where ").append(parameterName)
                .append(" = \'").append(value).append("\'");
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery(queryBuilder.toString());
        transaction.commit();
        return query;
    }
}
