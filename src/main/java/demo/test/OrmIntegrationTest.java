package demo.test;

import miniORM.core.EntityManager;
import miniORM.db.DataSourceProvider;
import miniORM.schemaGenerator.SchemaGenerator;
import demo.model.Customer;
import demo.model.Order;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrmIntegrationTest {

    private EntityManager entityManager;

    @BeforeAll
    void setupDatabase() {
        SchemaGenerator.initializeDatabase();
        entityManager = new EntityManager(DataSourceProvider.getDataSource());
    }

    @AfterAll
    void close() {
        DataSourceProvider.closeDataSource();
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void saveAndFindCustomer_shouldPersistAndRetrieveCustomer() {
        Customer customer = new Customer();
        customer.setName("Ali Mohammadi");
        entityManager.save(customer);

        assertNotNull(customer.getId(), "Customer ID should be generated after save.");

        Customer found = entityManager.findById(Customer.class, customer.getId());
        assertNotNull(found, "Customer should be found by ID.");
        assertEquals("Ali Mohammadi", found.getName(), "Customer name should match.");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void saveOrderWithCustomer_shouldPersistOrderAndLinkCustomer() {
        Customer customer = new Customer();
        customer.setName("Mina Ahmadi");
        entityManager.save(customer);

        Order order = new Order();
        order.setProduct("Book");
        order.setCustomer(customer);
        entityManager.save(order);

        assertNotNull(order.getId(), "Order ID should be generated after save.");

        Order foundOrder = entityManager.findById(Order.class, order.getId());
        assertNotNull(foundOrder, "Order should be found by ID.");
        assertEquals("Book", foundOrder.getProduct(), "Order product should match.");
        assertNotNull(foundOrder.getCustomer(), "Order's customer should not be null.");
        assertEquals(customer.getId(), foundOrder.getCustomer().getId(), "Order's customer ID should match.");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void findAllCustomers_shouldReturnAllSavedCustomers() {
        Customer c1 = new Customer(); c1.setName("A"); entityManager.save(c1);
        Customer c2 = new Customer(); c2.setName("B"); entityManager.save(c2);

        List<Customer> all = entityManager.findAll(Customer.class);
        assertTrue(all.size() >= 2, "There should be at least 2 customers in the database.");
    }
}
