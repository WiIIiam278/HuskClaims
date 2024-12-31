package net.william278.huskclaims.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public class BlockMaterialBlock extends BlockProvider.MaterialBlock {

    private final Block data;

    @NotNull
    public static BlockMaterialBlock create(@NotNull Block material) {
        return new BlockMaterialBlock(material);
    }

    @NotNull
    @Override
    public String getMaterialKey() {
        return data.getDefaultState().getRegistryEntry().getKey().toString();
    }
}
