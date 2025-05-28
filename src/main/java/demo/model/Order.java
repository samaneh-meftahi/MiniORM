package demo.model;

import miniORM.annotation.*;
import miniORM.annotation.Relation.ManyToOne;
import miniORM.annotation.Relation.JoinColumn;

@Entity(tableName = "cart")
public class Order {

    @Id
    @Column(name = "id")
    @GeneratedValue
    private Long id;

    @Column(name = "product")
    private String product;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
