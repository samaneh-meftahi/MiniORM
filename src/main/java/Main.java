import miniORM.core.EntityManager;
import miniORM.db.DataSourceProvider;
import demo.repository.Repository;
import miniORM.schemaGenerator.EntityUtil;
import miniORM.schemaGenerator.SchemaGenerator;
import demo.model.Customer;
import demo.model.Order;
import miniORM.schemaGenerator.config.SchemaGenerationStrategy;

import java.sql.SQLException;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws SQLException {

        var dataSource = DataSourceProvider.getDataSource();

        // Scan and print Entities
        Set<Class<?>> entities = EntityUtil.entityScanner();
        System.out.println("Number of @Entity classes found: " + entities.size());
        entities.forEach(c -> System.out.println("Entity: " + c.getName()));

        // Initialize or update the database schema based on the specified strategy:
        // CREATE - Drops existing tables and creates new ones from scratch
        // UPDATE - Adds missing columns to existing tables without dropping data
        // NONE   - Skips any schema changes
        SchemaGenerator.initializeDatabase(SchemaGenerationStrategy.CREATE);

        // Create EntityManager and Repositories
        EntityManager em = new EntityManager(dataSource);
        Repository<Customer> customerRepo = new Repository<>(Customer.class, em);
        Repository<Order> orderRepo = new Repository<>(Order.class, em);

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

        Customer loadedCustomer = customerRepo.findById(1L);
        System.out.println("Loaded customer: " + loadedCustomer.getName());

        DataSourceProvider.closeDataSource();
    }
}
