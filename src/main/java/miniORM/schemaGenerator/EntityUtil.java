package miniORM.schemaGenerator;

import miniORM.annotation.Entity;
import miniORM.annotation.Relation.ManyToOne;
import miniORM.annotation.Relation.OneToMany;
import miniORM.annotation.Relation.OneToOne;
import miniORM.annotation.Relation.ManyToMany;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class EntityUtil {

    private static String basePackage;

    static {
        // Find the base package at class loading time
        basePackage = findEntityPackage();
    }

    /**
     * Scan and return all classes annotated with @Entity
     */
    public static Set<Class<?>> entityScanner() {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }

    /**
     * Find all entities that have at least one relation
     * (i.e., have a field with one of the JPA relation annotations)
     */
    public static Set<Class<?>> findEntitiesWithRelations() {
        Set<Class<?>> entitiesWithRelations = new HashSet<>();
        for (Class<?> clazz : entityScanner()) {
            if (hasAnyRelationField(clazz)) {
                entitiesWithRelations.add(clazz);
            }
        }
        return entitiesWithRelations;
    }

    /**
     * Find all entities that have no relations
     */
    public static Set<Class<?>> findEntitiesWithoutRelations() {
        Set<Class<?>> entitiesWithoutRelations = new HashSet<>();
        for (Class<?> clazz : entityScanner()) {
            if (!hasAnyRelationField(clazz)) {
                entitiesWithoutRelations.add(clazz);
            }
        }
        return entitiesWithoutRelations;
    }

    /**
     * Check if the class has at least one field with a JPA relation annotation
     */
    private static boolean hasAnyRelationField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ManyToOne.class)
                    || field.isAnnotationPresent(OneToOne.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the base package by scanning the entire classpath and finding the first @Entity class
     */
    private static String findEntityPackage() {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forJavaClassPath())
                        .setScanners(new TypeAnnotationsScanner())
        );

        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);

        if (entities.isEmpty()) {
            throw new IllegalStateException("No @Entity class found!");
        }

        return entities.iterator().next().getPackageName();
    }
}
