package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

final class PlannerDebugOptions {

    private static final String FILE_NAME = "planner_debug_deleted_modifiers.dat";
    private static boolean deleteMode;
    private static final Set<String> deletedModifierIds = new LinkedHashSet<>();
    private static boolean loaded;

    private PlannerDebugOptions() {
    }

    static boolean isDeleteMode() {
        return deleteMode;
    }

    static void setDeleteMode(boolean enabled) {
        deleteMode = enabled;
    }

    static void markDeletedModifier(String identifier) {
        load();
        if (identifier != null && !identifier.isEmpty()) {
            if (deletedModifierIds.add(identifier)) {
                save();
            }
        }
    }

    static void unmarkDeletedModifier(String identifier) {
        load();
        if (identifier != null && deletedModifierIds.remove(identifier)) {
            save();
        }
    }

    static boolean isDeletedModifier(String identifier) {
        load();
        return identifier != null && deletedModifierIds.contains(identifier);
    }

    static int deletedModifierCount() {
        load();
        return deletedModifierIds.size();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        File file = getFile();
        if (!file.exists()) {
            return;
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null) {
                return;
            }
            NBTTagList list = root.getTagList("modifiers", 8);
            for (int i = 0; i < list.tagCount(); i++) {
                String id = list.getStringTagAt(i);
                if (!id.isEmpty()) {
                    deletedModifierIds.add(id);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void save() {
        try {
            File file = getFile();
            File folder = file.getParentFile();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }
            NBTTagCompound root = new NBTTagCompound();
            NBTTagList list = new NBTTagList();
            for (String id : deletedModifierIds) {
                list.appendTag(new NBTTagString(id));
            }
            root.setTag("modifiers", list);
            CompressedStreamTools.write(root, file);
        } catch (IOException ignored) {
        }
    }

    private static File getFile() {
        return new File(PlannerClientEvents.getDataFolder(), FILE_NAME);
    }
}
