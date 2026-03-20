package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query(
        value = """
            SELECT * FROM items
            WHERE search_vector @@ to_tsquery(
                'english',
                regexp_replace(trim(:query), '\\s+', ':* & ', 'g') || ':*'
            )
            """,
        nativeQuery = true
    )
    List<Item> search(@Param("query") String query);
}
