package loyal3.poc.utils;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import scala.collection.JavaConverters;

import com.loyal3.model.email.SocketLabsApiCall;


/**
 * Demonstrates how to use the Loyal3 Model persistence classes
 */
public class HibernateUtil
{
	public static SessionFactory factory = initializeSession();


	/**
	 * Entry point - query all PageSet instances and print their names.
	 * Note: the ruby bootstrap will take care of populating the page_sets table.
	 *
	 */
	public static void main(String[] args) throws Exception {

		Session session = factory.openSession();
		session.beginTransaction();

		List<SocketLabsApiCall> socketLabsApiCall = (List<SocketLabsApiCall>) session.createQuery("FROM SocketLabsApiCall").list();
		for (SocketLabsApiCall set : socketLabsApiCall) {
			println("socketLabsApiCall raw response = " + set.getRawResponse());
		}

		session.close();
	}


	private static void println(String s) { System.out.println(s);}


	/**
	 * Create a new Hibernate session with the Loyal3 Model classes.
	 */
	private static SessionFactory initializeSession() {
		Configuration config = new Configuration();

		// Add the Loyal3 hibernate classes
		List classes = JavaConverters.seqAsJavaListConverter( Schema.resourceFiles() ).asJava();
		List<Class> modelClasses = (List<Class>) classes;
		for (Class cls : modelClasses) {
			config.addAnnotatedClass(cls);
		}

		config.configure("loyal3/poc/utils/hibernate.cfg.xml");

		return config.buildSessionFactory();
	}



}