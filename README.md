# MiniORM

**MiniORM** is a lightweight, Java-based Object-Relational Mapping (ORM) framework designed to streamline database interactions using plain Java classes. It provides essential ORM features such as entity management, schema generation, and relationship mapping, embodying a minimal yet effective subset of JPA/Hibernate functionalities.

---

## Features

- **Annotation-Driven Entity Mapping**  
  Use standard annotations like `@Entity`, `@Id`, `@Column`, and relationship annotations (`@OneToOne`, `@ManyToOne`, `@OneToMany`, `@ManyToMany`).

- **Relationship Support**  
  Handle one-to-one, one-to-many, many-to-one, and many-to-many relationships with automatic join table management.

- **Flexible Schema Generation**  
  - `CREATE`: Drops and recreates all tables from entity definitions.
  - `UPDATE`: Alters existing tables to match entity changes (without data loss).
  - `NONE`: Disables automatic schema management.

- **Custom SQL Generation**  
  Dynamic query generation for efficient CRUD operations.

- **Transaction Management**  
  Lightweight transaction handling for data consistency.

- **Connection Pooling**  
  Uses HikariCP for high-performance database connections (see `miniORM.db.DataSourceProvider`).

---

## Project Structure

```
src/
└── main/
├── java/
│   └── miniORM/
│       ├── annotation/        # Custom annotations (@Entity, @Id, ...)
│       ├── core/              # Core ORM logic (EntityManager, etc.)
│       ├── db/                # Database connection (HikariCP)
│       │   └── DataSourceProvider.java
│       ├── exception/         # Custom exception classes
│       ├── metaData/          # Entity metadata management
│       ├── schemaGenerator/   # Schema generation & migration
│       │   ├── config/        # Schema config classes
│       │   ├── EntityUtil.java
│       │   └── SchemaGenerator.java
│       ├── sql/               # SQL utilities
│       └── Main.java          # Entry point
├── resources/
│   └── META-INF/
│       └── orm.properties     # ORM configuration file
└── test/                      # Unit and integration tests
```

---

## Configuration

By default, MiniORM uses an embedded H2 database with HikariCP.  
You can change your connection settings in `miniORM.db.DataSourceProvider` or via `orm.properties`:

```
db.url=jdbc:h2:./testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false
db.username=sa
db.password=
db.driver=org.h2.Driver
schema.strategy=CREATE
```

**Available `schema.strategy` options:**
- `CREATE`: Drops and recreates all tables on startup.
- `UPDATE`: Applies incremental changes to existing schema.
- `NONE`: Disables automatic schema management.

---

## Usage Example

Define your entity:

```
@Entity
public class User {
@Id
private Long id;

    @Column
    private String name;

    @OneToMany(mappedBy = "user")
    private List posts;
}
```

Persist and query with `EntityManager`:

```
EntityManager em = new EntityManager();
em.persist(new User(...));
List users = em.findAll(User.class);
```

---

## Building

To package MiniORM into a JAR file:

```
mvn clean package
```
or
```
jar cf miniorm.jar -C out/production/MiniORM .
```

**Include only these packages in your JAR:**
- `annotation`
- `core`
- `db`
- `exception`
- `metaData`
- `schemaGenerator`
- `sql`
---

## License

This project is licensed under the MIT License.  
Feel free to use, modify, and distribute.

---

## Contact

For questions or contributions, please open an issue or pull request.

---

> **Tip:**  
> By default, the project is set up for H2. For other databases (e.g., MySQL), update `DataSourceProvider` and `orm.properties` accordingly.