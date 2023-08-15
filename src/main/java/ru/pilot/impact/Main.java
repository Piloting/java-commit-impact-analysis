package ru.pilot.impact;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import ru.pilot.impact.dto.MethodDto;
import ru.pilot.impact.git.GitParser;

public class Main {

    public static void main(String[] args) throws IOException, GitAPIException {
        GitParser gitParser = new GitParser();

        Set<MethodDto> modifiedMethods = gitParser.getModifiedLines(
                "c:\\Work\\PROJECT\\pilot\\gitlog-plugin\\.git\\",
                "gitlog plugin");

        for (MethodDto methodDto : modifiedMethods) {
            System.out.println(methodDto.getClassName() + "#" + methodDto.getMethodName() + methodDto.getParams());
        }
    }

}