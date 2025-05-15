package net.minecraft.world.chunk.storage;

import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.IOException;

public interface IChunkLoader {
    Chunk loadChunk(World worldIn, int x, int z) throws IOException;

    void saveChunk(World worldIn, Chunk chunkIn) throws MinecraftException;

    void saveExtraChunkData(World worldIn, Chunk chunkIn);

    void chunkTick();

    void saveExtraData();
}
