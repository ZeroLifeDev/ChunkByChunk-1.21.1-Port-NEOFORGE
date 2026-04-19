package xyz.immortius.chunkbychunk.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.*;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import xyz.immortius.chunkbychunk.client.screens.BedrockChestScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldForgeScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldMenderScreen;
import xyz.immortius.chunkbychunk.client.screens.WorldScannerScreen;
import xyz.immortius.chunkbychunk.common.ChunkByChunkConstants;
import xyz.immortius.chunkbychunk.common.CommonEventHandler;
import xyz.immortius.chunkbychunk.common.blockEntities.BedrockChestBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldForgeBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldMenderBlockEntity;
import xyz.immortius.chunkbychunk.common.blockEntities.WorldScannerBlockEntity;
import xyz.immortius.chunkbychunk.common.blocks.*;
import xyz.immortius.chunkbychunk.common.menus.BedrockChestMenu;
import xyz.immortius.chunkbychunk.common.menus.WorldForgeMenu;
import xyz.immortius.chunkbychunk.common.menus.WorldMenderMenu;
import xyz.immortius.chunkbychunk.common.menus.WorldScannerMenu;
import xyz.immortius.chunkbychunk.config.ChunkByChunkConfig;
import xyz.immortius.chunkbychunk.config.system.ConfigSystem;
import xyz.immortius.chunkbychunk.server.ServerEventHandler;
import xyz.immortius.chunkbychunk.server.commands.SpawnChunkCommand;
import xyz.immortius.chunkbychunk.server.world.SkyChunkGenerator;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Forge mod, registers all mod elements for forge
 */
@Mod(ChunkByChunkConstants.MOD_ID)
public class ChunkByChunkMod {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(BuiltInRegistries.MENU, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ChunkByChunkConstants.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, ChunkByChunkConstants.MOD_ID);

    public static final DeferredHolder<Block, SpawnChunkBlock> SPAWN_CHUNK_BLOCK = BLOCKS.register("chunkspawner", () -> new SpawnChunkBlock("", false, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredHolder<Block, SpawnChunkBlock> UNSTABLE_SPAWN_CHUNK_BLOCK = BLOCKS.register("unstablechunkspawner", () -> new SpawnChunkBlock("", true, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredHolder<Block, BedrockChestBlock> BEDROCK_CHEST_BLOCK = BLOCKS.register("bedrockchest", () -> new BedrockChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(-1, 3600000.0F).noLootTable().isValidSpawn(((p_61031_, p_61032_, p_61033_, p_61034_) -> false))));
    public static final DeferredHolder<Block, Block> WORLD_CORE_BLOCK = BLOCKS.register("worldcore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.0F).lightLevel((state) -> 7)));
    public static final DeferredHolder<Block, WorldForgeBlock> WORLD_FORGE_BLOCK = BLOCKS.register("worldforge", () -> new WorldForgeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.5F).lightLevel((state) -> 7)));
    public static final DeferredHolder<Block, WorldScannerBlock> WORLD_SCANNER_BLOCK = BLOCKS.register("worldscanner", () -> new WorldScannerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.5F).lightLevel((state) -> 4)));
    public static final DeferredHolder<Block, WorldMenderBlock> WORLD_MENDER_BLOCK = BLOCKS.register("worldmender", () -> new WorldMenderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.5F).lightLevel((state) -> 4)));

    public static final DeferredHolder<Item, BlockItem> SPAWN_CHUNK_BLOCK_ITEM = ITEMS.register("chunkspawner", () -> new BlockItem(SPAWN_CHUNK_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM = ITEMS.register("unstablechunkspawner", () -> new BlockItem(UNSTABLE_SPAWN_CHUNK_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> BEDROCK_CHEST_ITEM = ITEMS.register("bedrockchest", () -> new BlockItem(BEDROCK_CHEST_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WORLD_CORE_BLOCK_ITEM = ITEMS.register("worldcore", () -> new BlockItem(WORLD_CORE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WORLD_FORGE_BLOCK_ITEM = ITEMS.register("worldforge", () -> new BlockItem(WORLD_FORGE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WORLD_SCANNER_BLOCK_ITEM = ITEMS.register("worldscanner", () -> new BlockItem(WORLD_SCANNER_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WORLD_MENDER_BLOCK_ITEM = ITEMS.register("worldmender", () -> new BlockItem(WORLD_MENDER_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> WORLD_FRAGMENT_ITEM = ITEMS.register("worldfragment", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WORLD_SHARD_ITEM = ITEMS.register("worldshard", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WORLD_CRYSTAL_ITEM = ITEMS.register("worldcrystal", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BedrockChestBlockEntity>> BEDROCK_CHEST_BLOCK_ENTITY = BLOCK_ENTITIES.register("bedrockchestentity", () -> BlockEntityType.Builder.of(BedrockChestBlockEntity::new, BEDROCK_CHEST_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldForgeBlockEntity>> WORLD_FORGE_BLOCK_ENTITY = BLOCK_ENTITIES.register("worldforgeentity", () -> BlockEntityType.Builder.of(WorldForgeBlockEntity::new, WORLD_FORGE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldScannerBlockEntity>> WORLD_SCANNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("worldscannerentity", () -> BlockEntityType.Builder.of(WorldScannerBlockEntity::new, WORLD_SCANNER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldMenderBlockEntity>> WORLD_MENDER_BLOCK_ENTITY = BLOCK_ENTITIES.register("worldmenderentity", () -> BlockEntityType.Builder.of(WorldMenderBlockEntity::new, WORLD_MENDER_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BedrockChestMenu>> BEDROCK_CHEST_MENU = CONTAINERS.register("bedrockchestmenu", () -> new MenuType<>(BedrockChestMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorldForgeMenu>> WORLD_FORGE_MENU = CONTAINERS.register("worldforgemenu", () -> new MenuType<>(WorldForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorldScannerMenu>> WORLD_SCANNER_MENU = CONTAINERS.register("worldscannermenu", () -> new MenuType<>(WorldScannerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<WorldMenderMenu>> WORLD_MENDER_MENU = CONTAINERS.register("worldmendermenu", () -> new MenuType<>(WorldMenderMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN_CHUNK_SOUND_EVENT = SOUNDS.register("spawnchunkevent", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "chunk_spawn_sound")));

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends SkyChunkGenerator>> SKY_CHUNK_GENERATOR = CHUNK_GENERATORS.register("skychunkgenerator", () -> SkyChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends SkyChunkGenerator>> OLD_NETHER_CHUNK_GENERATOR = CHUNK_GENERATORS.register("netherchunkgenerator", () -> SkyChunkGenerator.OLD_NETHER_CODEC);

    public static final List<Supplier<ItemStack>> THEMED_SPAWN_CHUNK_ITEMS = new ArrayList<>();

    private static final String PROTOCOL_VERSION = "1";

    static {
        for (String biomeTheme : ChunkByChunkConstants.BIOME_THEMES) {
            DeferredHolder<Block, SpawnChunkBlock> spawnBlock = BLOCKS.register(biomeTheme + ChunkByChunkConstants.BIOME_CHUNK_BLOCK_SUFFIX, () -> new SpawnChunkBlock(biomeTheme, false, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
            DeferredHolder<Item, BlockItem> spawnBlockItem = ITEMS.register(biomeTheme + ChunkByChunkConstants.BIOME_CHUNK_BLOCK_ITEM_SUFFIX, () -> new BlockItem(spawnBlock.get(), new Item.Properties()));
            THEMED_SPAWN_CHUNK_ITEMS.add(() -> spawnBlockItem.get().getDefaultInstance());
        }
    }

    public ChunkByChunkMod(IEventBus eventBus) {
        new ConfigSystem().synchConfig(Paths.get(ChunkByChunkConstants.DEFAULT_CONFIG_PATH, ChunkByChunkConstants.CONFIG_FILE), ChunkByChunkConfig.get());

        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);

        BLOCK_ENTITIES.register(eventBus);
        CONTAINERS.register(eventBus);
        SOUNDS.register(eventBus);
        CHUNK_GENERATORS.register(eventBus);

        eventBus.addListener(this::updateCreativeTabs);
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::registerScreens);
        eventBus.addListener(this::onGameConfiguration);
        eventBus.addListener(this::registerPayloadHandler);
        NeoForge.EVENT_BUS.addListener(this::registerResourceReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlaceItem);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    public void onGameConfiguration(final RegisterConfigurationTasksEvent event) {
        event.register(new ConfigurationTask(event.getListener()));
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register config screen if needed or other client stuff
        });
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BEDROCK_CHEST_MENU.get(), BedrockChestScreen::new);
        event.register(WORLD_FORGE_MENU.get(), WorldForgeScreen::new);
        event.register(WORLD_SCANNER_MENU.get(), WorldScannerScreen::new);
        event.register(WORLD_MENDER_MENU.get(), WorldMenderScreen::new);
    }

    public void updateCreativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            e.accept(WORLD_FRAGMENT_ITEM.get());
            e.accept(WORLD_SHARD_ITEM.get());
            e.accept(WORLD_CRYSTAL_ITEM.get());
            e.accept(WORLD_CORE_BLOCK_ITEM.get());
            e.accept(WORLD_FORGE_BLOCK_ITEM.get());
            e.accept(WORLD_SCANNER_BLOCK_ITEM.get());
            e.accept(WORLD_MENDER_BLOCK_ITEM.get());
            e.accept(SPAWN_CHUNK_BLOCK_ITEM.get());
            e.accept(UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM.get());
            for (Supplier<ItemStack> biomeThemeSpawner : THEMED_SPAWN_CHUNK_ITEMS) {
                e.accept(biomeThemeSpawner.get());
            }
        }
    }

    public void registerPayloadHandler(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ChunkByChunkConstants.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.configurationBidirectional(ConfigMessage.TYPE, ConfigMessage.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                ChunkByChunkConfig.get().getGameplayConfig().setBlockPlacementAllowedOutsideSpawnedChunks(payload.blockPlacementAllowed);
            });
        });
    }

    public void registerResourceReloadListeners(AddReloadListenerEvent e) {
        ChunkByChunkConstants.LOGGER.info("Registering resource reload listeners");
        e.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ChunkByChunkConstants.LOGGER.info("Loading resources");
                ServerEventHandler.onResourceManagerReload(resourceManager);
            }

            @Override
            public String getName() {
                return ChunkByChunkConstants.MOD_ID + ":server_data";
            }
        });
    }

    public void registerCommands(RegisterCommandsEvent event) {
        SpawnChunkCommand.register(event.getDispatcher());
    }

    public void onPlaceItem(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockPos placePos = pos.relative(event.getFace());
        if (!CommonEventHandler.isBlockPlacementAllowed(placePos, event.getEntity(), event.getLevel())) {
            event.setCanceled(true);
        }
    }

    public void onServerTick(ServerTickEvent.Post event) {
        ServerEventHandler.onLevelTick(event.getServer());
    }

    public void onServerStarted(ServerStartedEvent event) {
        ServerEventHandler.onServerStarted(event.getServer());
    }

    public void onServerStarting(ServerAboutToStartEvent event) {
        ServerEventHandler.onServerStarting(event.getServer());
    }

    private record ConfigMessage(boolean blockPlacementAllowed) implements CustomPacketPayload {

        public static final Type<ConfigMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "configchannel"));
        public static final StreamCodec<FriendlyByteBuf, ConfigMessage> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ConfigMessage::blockPlacementAllowed,
                ConfigMessage::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ConfigurationTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        public static final ICustomConfigurationTask.Type TYPE = new ICustomConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(ChunkByChunkConstants.MOD_ID, "configure"));

        @Override
        public void run(java.util.function.Consumer<CustomPacketPayload> sender) {
            final ConfigMessage payload = new ConfigMessage(ChunkByChunkConfig.get().getGameplayConfig().isBlockPlacementAllowedOutsideSpawnedChunks());
            sender.accept(payload);
            listener.finishCurrentTask(type());
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }

}
