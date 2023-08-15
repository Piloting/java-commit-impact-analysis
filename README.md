# Получение списка измененных java методов по комиту 

Заготовка.

GitParser

1. Указываем путь до папки .git. 
2. Указываем номер тикета Jira (сообщение коммита должно с него начинаться)
3. Получаем список измененных java методов
```
new GitParser().getModifiedLines("c:\\Work\\PROJECT\\pilot\\gitlog-plugin\\.git\\", "gitlog plugin");
Результат:
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#convertToDto[List<Map<String,String>>]
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#addEndSlash[String]
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#getReplacedIssueNumberToJiraUrl[String]
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#byTemplateStr[List<Map<String,String>>, StringBuffer]
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#getCommitInfoMap[RevCommit]
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#execute[]
ru.suntsovto.plugin.gitLogGenerator.GitLogGenerator#byTemplateFile[Set<String>, List<Map<String,String>>, StringBuffer]
```
4. Применяем данный список для подбора тесткейсов для проверки
