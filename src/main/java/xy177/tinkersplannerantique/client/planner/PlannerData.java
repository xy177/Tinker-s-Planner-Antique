package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

final class PlannerData {

    private final File rootFolder;
    private final File toolFile;
    private final File armorFile;

    PlannerData(File rootFolder) {
        this.rootFolder = rootFolder;
        this.toolFile = new File(rootFolder, "planner_bookmarks_tools.dat");
        this.armorFile = new File(rootFolder, "planner_bookmarks_armor.dat");
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }
    }

    List<PlannerBlueprint> loadSaved(PlannerTarget.TargetType type, List<? extends PlannerTarget> targets) {
        File file = getFile(type);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null) {
                return new ArrayList<>();
            }
            NBTTagList list = root.getTagList("saved", 10);
            List<PlannerBlueprint> result = new ArrayList<>(list.tagCount());
            for (int i = 0; i < list.tagCount(); i++) {
                PlannerBlueprint blueprint = PlannerBlueprint.fromTag(list.getCompoundTagAt(i), targets);
                if (blueprint != null) {
                    result.add(blueprint);
                }
            }
            return result;
        } catch (IOException ignored) {
            return new ArrayList<>();
        }
    }

    PlannerBlueprint loadStarred(PlannerTarget.TargetType type, List<? extends PlannerTarget> targets) {
        File file = getFile(type);
        if (!file.exists()) {
            return null;
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null || !root.hasKey("starred", 10)) {
                return null;
            }
            return PlannerBlueprint.fromTag(root.getCompoundTag("starred"), targets);
        } catch (IOException ignored) {
            return null;
        }
    }

    void save(PlannerTarget.TargetType type, List<PlannerBlueprint> saved, PlannerBlueprint starred) {
        try {
            NBTTagCompound root = new NBTTagCompound();
            NBTTagList list = new NBTTagList();
            for (PlannerBlueprint blueprint : saved == null ? Collections.<PlannerBlueprint>emptyList() : saved) {
                if (blueprint != null && blueprint.isComplete()) {
                    list.appendTag(blueprint.toTag());
                }
            }
            root.setTag("saved", list);
            if (starred != null && starred.isComplete()) {
                root.setTag("starred", starred.toTag());
            }
            CompressedStreamTools.write(root, getFile(type));
        } catch (IOException ignored) {
        }
    }

    private File getFile(PlannerTarget.TargetType type) {
        return type == PlannerTarget.TargetType.ARMOR ? armorFile : toolFile;
    }

    File getRootFolder() {
        return rootFolder;
    }
}
