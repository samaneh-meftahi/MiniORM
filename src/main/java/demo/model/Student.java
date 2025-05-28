package demo.model;

import miniORM.annotation.Column;
import miniORM.annotation.Entity;
import miniORM.annotation.Id;

@Entity(tableName = "student")
public class Student {
    @Id
    @Column(name = "userId")
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "LastName")
    private String lastName;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
