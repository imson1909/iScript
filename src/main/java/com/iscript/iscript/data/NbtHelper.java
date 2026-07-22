package com.iscript.iscript.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NbtHelper {

    public static void writeVec3(CompoundTag tag, String key, Vec3 v) {
        CompoundTag t = new CompoundTag();
        t.putDouble("x", v.x);
        t.putDouble("y", v.y);
        t.putDouble("z", v.z);
        tag.put(key, t);
    }

    public static Vec3 readVec3(CompoundTag tag, String key, Vec3 def) {
        if (!tag.contains(key)) return def;
        CompoundTag t = tag.getCompound(key);
        return new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
    }

    public static void writeBlockPos(CompoundTag tag, String key, BlockPos pos) {
        if (pos != null) tag.put(key, NbtUtils.writeBlockPos(pos));
    }

    public static BlockPos readBlockPos(CompoundTag tag, String key) {
        return tag.contains(key) ? NbtUtils.readBlockPos(tag.getCompound(key)) : null;
    }

    public static ListTag writeList(List<? extends DataObject> list) {
        ListTag tag = new ListTag();
        for (DataObject o : list) tag.add(o.save(new CompoundTag()));
        return tag;
    }

    public static <T extends DataObject> List<T> readList(ListTag tag, Supplier<T> factory) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) {
            T t = factory.get();
            t.load(tag.getCompound(i));
            list.add(t);
        }
        return list;
    }

    public static <T extends DataObject> void readListInto(CompoundTag tag, String key, List<T> target, Supplier<T> factory) {
        target.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        target.addAll(readList(tag.getList(key, Tag.TAG_COMPOUND), factory));
    }

    public static <T extends DataObject> void writeListFrom(CompoundTag tag, String key, List<T> source) {
        tag.put(key, writeList(source));
    }

    public static <T extends DataObject> T copy(T original, Function<CompoundTag, T> loader, Function<T, CompoundTag> saver) {
        return loader.apply(saver.apply(original));
    }
}