import demo.model.Student;
import miniORM.core.EntityManager;
import miniORM.db.DataSourceProvider;
import demo.repository.Repository;
import miniORM.schemaGenerator.EntityUtil;
import miniORM.schemaGenerator.SchemaGenerator;
import demo.model.Customer;
import demo.model.Order;

import java.sql.SQLException;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws SQLException {

        var dataSource = DataSourceProvider.getDataSource();

        // Scan and print Entities
        Set<Class<?>> entities = EntityUtil.entityScanner();
        System.out.println("Number of @Entity classes found: " + entities.size());
        entities.forEach(c -> System.out.println("Entity: " + c.getName()));

        // Create database tables
        SchemaGenerator.initializeDatabase();

        // Create EntityManager and Repositories
        EntityManager em = new EntityManager(dataSource);
        Repository<Customer> customerRepo = new Repository<>(Customer.class, em);
        Repository<Order> orderRepo = new Repository<>(Order.class, em);
        Repository<Student> studentRepo = new Repository<>(Student.class,em);

        // Save a customer
        Customer customer = new Customer();
        customer.setName("Ali");
        customerRepo.save(customer);

        Customer customer2 = new Customer();
        customer2.setName("Ali");
        customerRepo.save(customer2);

        // Save an order
        Order order = new Order();
        order.setProduct("Book");
        order.setCustomer(customer);
        orderRepo.save(order);

        Student student=new Student();
        student.setUserId("sm");
        student.setName("sm");
        studentRepo.save(student);

        Customer loadedCustomer = customerRepo.findById(1L);
        System.out.println("Loaded customer: " + loadedCustomer.getName());

        DataSourceProvider.closeDataSource();
    }
}
