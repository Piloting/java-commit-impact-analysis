package ru.pilot.impact.dto;

import java.util.List;

import lombok.Data;

@Data
public class MethodDto {
    private String className;
    private String methodName;
    private List<String> params;
    private LinesDto lines;

    public String toString() {
        return "\n\t" + this.getMethodName() + this.getParams() + ", lines=" + this.getLines();
    }
}
