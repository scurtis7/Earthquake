package com.scurtis.earthquakes.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "earthquake", name = "day_count")
public class DayCount implements Persistable<Integer> {

    @Id
    private Integer id;
    private String day;
    private int count;

    @Override
    @Transient
    public boolean isNew() {
        return id == null || id == 0;
    }

}
