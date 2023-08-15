package ru.pilot.impact.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ClassDto {
    private String className;
    private List<MethodDto> methods = new ArrayList<>();

    public void addMethod(MethodDto methodDto) {
        methods.add(methodDto);
    }

    public String toString() {
        return "class=" + this.className + "\nmethods" + this.methods + "\n\n";
    }
}
