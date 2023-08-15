package ru.pilot.impact.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LinesDto {
    private Integer start;
    private Integer end;

    public String toString() {
        return this.getStart() + ".." + this.getEnd();
    }
}
