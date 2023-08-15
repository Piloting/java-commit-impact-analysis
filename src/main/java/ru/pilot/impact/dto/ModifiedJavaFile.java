package ru.pilot.impact.dto;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModifiedJavaFile {
    private String path;
    private Set<Integer> modifyLines;
    private List<ClassDto> classDtoList;
}
