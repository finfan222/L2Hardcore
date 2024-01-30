package net.sf.l2j;

import lombok.Getter;
import lombok.ToString;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author finfan
 */
public class Test {

    public static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forJavaClassPath())
        .setScanners(
            Scanners.SubTypes,
            Scanners.FieldsAnnotated,
            Scanners.TypesAnnotated,
            Scanners.MethodsAnnotated,
            Scanners.ConstructorsAnnotated,
            Scanners.Resources
        ));

    @ToString
    @Getter
    public static class Data {
        int npcId;
        List<Integer> questIds = new ArrayList<>();
    }

    private static boolean isExists(int npcId, List<Data> data) {
        for(Data next : data) {
            if(next.getNpcId() > 0 && next.getNpcId() == npcId) {
                return true;
            }
        }
        return false;
    }

    private static Data get(int npcId, List<Data> data) {
        return data.stream().filter(e -> e.npcId == npcId).findAny().orElseThrow();
    }

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        File dir = new File("D:\\L2Hardcore\\src\\main\\java\\net\\sf\\l2j\\gameserver\\scripting\\quest\\");


        List<String> finalNames = new ArrayList<>();
        List<Data> data = new CopyOnWriteArrayList<>();
        for (File next : Objects.requireNonNull(dir.listFiles())) {
            char[] charArray = next.getName().toCharArray();
            if (!Character.isDigit(charArray[1])) {
                continue;
            }

            String questId = new StringBuilder().append(charArray[1]).append(charArray[2]).append(charArray[3]).toString();

            List<String> result = new ArrayList<>();
            for (String line : Files.readAllLines(next.toPath())) {
                String replace = line.replace("\s", "");
                if (replace.startsWith("addStartNpc")) {
                    //addStartNpc(EYE_OF_ARGOS);
                    replace = replace.replace("addStartNpc(", "");
                    //EYE_OF_ARGOS);
                    String temp = "";
                    for (int i = 0; i < replace.toCharArray().length; i++) {
                        char ch = replace.toCharArray()[i];
                        if (ch == ')') {
                            break;
                        }
                        temp += ch;
                    }
                    //EYE_OF_ARGOS
                    result.add(temp);
                    break;
                }
            }

            List<String> names = new CopyOnWriteArrayList<>();
            List<Integer> ids = new ArrayList<>();
            for (String str : result) {
                if (str.matches("[0-9]+")) {
                    ids.add(Integer.parseInt(str));
                } else {
                    names.add(str);
                }
            }

            extract(finalNames, names, ids);
            if (!ids.isEmpty()) {
                for (int id : ids) {
                    if (isExists(id, data)) {
                        get(id, data).questIds.add(Integer.parseInt(questId));
                    } else {
                        Data d = new Data();
                        d.npcId = id;
                        d.questIds.add(Integer.parseInt(questId));
                        data.add(d);
                    }
                }
            }
        }
//        System.out.println(data);
        System.out.println(finalNames);
        finalNames.remove("i");

        for (File next : Objects.requireNonNull(dir.listFiles())) {
            char[] charArray = next.getName().toCharArray();
            if (!Character.isDigit(charArray[1])) {
                continue;
            }

            String questStrId = new StringBuilder().append(charArray[1]).append(charArray[2]).append(charArray[3]).toString();
            int questId = Integer.valueOf(questStrId);
            int npcId = 0;
            for (String line : Files.readAllLines(next.toPath())) {
                for (String name : finalNames) {
                    if (line.contains(name + " = ")) {
                        String[] split = line.split(" = ");
                        npcId = Integer.valueOf(split[1].replace(";", ""));
                        break;
                    }
                }
                if (npcId > 0) {
                    break;
                }
            }

            if (isExists(npcId, data)) {
                get(npcId, data).questIds.add(questId);
            } else {
                Data d = new Data();
                d.npcId = npcId;
                d.questIds.add(questId);
                data.add(d);
            }
        }

        data.removeIf(d -> d.npcId == 0);

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String line : Files.readAllLines(new File("D:\\npcgrp.txt").toPath())) {
            if (isFirst) {
                isFirst = false;
                continue;
            }

            String[] split = line.split("\t");
            int grpNpcId = Integer.parseInt(split[0]);

            for(Data d : data) {
                if (d.getNpcId() == grpNpcId) {
                    List<Integer> questIds = d.getQuestIds();
                    int size = questIds.size();
                    for (int i = 0; i < size; i++) {
                        split[60 + i] = questIds.get(i).toString();
                    }
                    split[59] = String.valueOf(size);
                }
            }


            for (int j = 0; j < split.length; j++) {
                if (j + 1 == split.length) {
                    sb.append(split[j]).append("\n");
                } else {
                    sb.append(split[j]).append("\t");
                }
            }
        }

        File newF = new File("D:\\npcgrp_modified.txt");
        try (FileWriter fw = new FileWriter(newF)) {
            fw.write(sb.toString());
        }
        /*List<Class<? extends Quest>> questClasses = REFLECTIONS.getSubTypesOf(Quest.class).stream().filter(t -> t.getSimpleName().startsWith("Q")).toList();

        List<Data> nameIdData = new ArrayList<>();
        for (Class<?> next : questClasses) {
            Data d = new Data();
            for (Field f : next.getDeclaredFields()) {
                String fieldName = f.getName();
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    if (f.getType() == String.class && fieldName.contentEquals("QUEST_NAME")) {
                        String questName = (String) f.get(next);
                        char[] charArray = questName.toCharArray();
                        if (!Character.isDigit(charArray[1])) {
                            continue;
                        }
                        String questId = new StringBuilder().append(charArray[1]).append(charArray[2]).append(charArray[3]).toString();
                        d.questId = Integer.valueOf(questId);
                        break;
                    }
                    if (f.getType() == int.class) {
                        for (String name : finalNames) {
                            if (fieldName.contentEquals(name)) {
                                d.npcId = (Integer) f.get(next);
                                break;
                            }
                        }
                    }
                }

            }
            if (d.npcId > 0 && d.questId > 0) {
                nameIdData.add(d);
            }
        }*/
    }

    private static void extract(List<String> finalNames, List<String> names, List<Integer> ids) {
        for (String name : names) {
            String[] split = name.split(",");
            if (split.length > 1) {
                if (Character.isDigit(split[0].charAt(0))) {
                    for (String plt : split) {
                        if (Character.isDigit(plt.charAt(0))) {
                            ids.add(Integer.parseInt(plt));
                        }
                    }
                    names.remove(name);
                } else {
                    for (String next : split) {
                        finalNames.add(next);
                    }
                }
            } else {
                finalNames.add(name);
            }
        }
        names.remove("i");
    }

}
