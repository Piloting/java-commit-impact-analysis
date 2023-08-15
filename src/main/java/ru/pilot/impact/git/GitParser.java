package ru.pilot.impact.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import ru.pilot.impact.ast.ClassParser;
import ru.pilot.impact.dto.ClassDto;
import ru.pilot.impact.dto.LinesDto;
import ru.pilot.impact.dto.MethodDto;
import ru.pilot.impact.dto.ModifiedJavaFile;

/**
 * Разбор локального репо гита
 */
public class GitParser {

    private final ClassParser classParser = new ClassParser();

    /**
     * Разбор локального репо гита
     *
     * @param gitPath путь до папки .git
     * @param task    имя таски для поиска коммитов
     * @return информация об измененых строках и на каких строках какие методы
     */
    public Set<MethodDto> getModifiedLines(String gitPath, String task) throws IOException, GitAPIException {

        // main repo for extract info
        Repository repo = new FileRepository(gitPath);
        ObjectReader reader = repo.newObjectReader();
        DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(repo.getConfig().getEnum(ConfigConstants.CONFIG_DIFF_SECTION, null, ConfigConstants.CONFIG_KEY_ALGORITHM, DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));

        // extract all modified lines and address methods in class 
        List<ModifiedJavaFile> modifiedJavaFiles = new ArrayList<>();
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repo);

            // get commit list
            Iterable<RevCommit> logs = getLastHeadCommits(repo);
            for (RevCommit commit : logs) {
                if (!byTask(task, commit)) {
                    // skip unnecessary commits
                    continue;
                }

                modifiedJavaFiles.addAll(
                        commitProcess(reader, diffAlgorithm, diffFormatter, commit));
            }
        }

        // map change to methods
        Set<MethodDto> modifiedMethods = extractModifyMethods(modifiedJavaFiles);

        return modifiedMethods;
    }

    private List<ModifiedJavaFile> commitProcess(ObjectReader reader, DiffAlgorithm diffAlgorithm, DiffFormatter diffFormatter, RevCommit commit) throws IOException {
        List<ModifiedJavaFile> modifiedJavaFiles = new ArrayList<>();

        // git tree by commit
        RevTree currentTree = commit.getTree();
        RevTree parentTree = commit.getParent(0).getTree();

        // get changes by tree
        for (DiffEntry diffEntry : diffFormatter.scan(parentTree, currentTree)) {
            if (isNotJava(diffEntry) || DiffEntry.ChangeType.MODIFY != diffEntry.getChangeType()) {
                // skip unnecessary changes
                continue;
            }
            //System.out.println(diffEntry.getNewPath() + " " + diffEntry.getChangeType());

            // changed java files - old and new version
            RawText oldText = readText(diffEntry.getOldId(), reader);
            RawText newText = readText(diffEntry.getNewId(), reader);

            // changes in file
            EditList editList = diffAlgorithm.diff(RawTextComparator.DEFAULT, oldText, newText);
            Set<Integer> modifiedLines = getModifiedLines(editList);

            if (modifiedLines.size() > 0) {
                // get methods address (lines) from java file
                List<ClassDto> classDtoList = classParser.extractMethods(new String(oldText.getRawContent(), StandardCharsets.UTF_8));
                if (classDtoList.isEmpty()) {
                    // interface, abstract, etc.
                    continue;
                }
                // create complex dto
                ModifiedJavaFile modifiedJavaFile = new ModifiedJavaFile(diffEntry.getOldPath(), modifiedLines, classDtoList);
                modifiedJavaFiles.add(modifiedJavaFile);
            }
        }

        return modifiedJavaFiles;
    }

    private Set<Integer> getModifiedLines(EditList editList) {
        Set<Integer> modifyLines = new TreeSet<>();
        for (Edit edit : editList) {
            //System.out.println(edit.getBeginA() + ".." + edit.getEndA());
            // collect all change lines by OLD file 
            modifyLines.addAll(IntStream.range(edit.getBeginA(), edit.getEndA()).boxed().collect(Collectors.toSet()));
        }
        return modifyLines;
    }


    private Set<MethodDto> extractModifyMethods(List<ModifiedJavaFile> modifiedJavaFiles) {
        Set<MethodDto> modifiedMethods = new HashSet<>();
        for (ModifiedJavaFile modifiedJavaFile : modifiedJavaFiles) {
            Set<Integer> modifyLines = modifiedJavaFile.getModifyLines();
            List<ClassDto> classDtoList = modifiedJavaFile.getClassDtoList();

            // address methods
            Map<Integer, MethodDto> lineToMethod = lineToMethod(classDtoList);

            // get java method by changed line
            for (Integer modifyLine : modifyLines) {
                MethodDto methodDto = lineToMethod.get(modifyLine);
                if (methodDto != null) {
                    modifiedMethods.add(methodDto);
                }
            }

        }

        return modifiedMethods;
    }

    private Map<Integer, MethodDto> lineToMethod(List<ClassDto> classDtoList) {
        Map<Integer, MethodDto> lineToMethod = new HashMap<>();
        for (ClassDto classDto : classDtoList) {
            for (MethodDto method : classDto.getMethods()) {
                LinesDto lines = method.getLines();
                IntStream.range(lines.getStart(), lines.getEnd()).forEach(line -> lineToMethod.put(line, method));
            }
        }
        return lineToMethod;
    }

    private boolean isNotJava(DiffEntry diffEntry) {
        return !diffEntry.getOldPath().endsWith(".java");
    }

    private static RawText readText(AbbreviatedObjectId blobId, ObjectReader reader) throws IOException {
        ObjectLoader oldLoader = reader.open(blobId.toObjectId(), Constants.OBJ_BLOB);
        return new RawText(oldLoader.getCachedBytes());
    }

    private boolean byTask(String task, RevCommit commit) {
        return commit.getShortMessage().toLowerCase().startsWith(task);
    }

    private Iterable<RevCommit> getLastHeadCommits(Repository repo) throws IOException, GitAPIException {
        Git git = new Git(repo);
        ObjectId currentBranch = repo.resolve(Constants.HEAD);
        LogCommand log = git.log().add(currentBranch).setMaxCount(20);
        return log.call();
    }
}
