package com.infinixmc.sleepingmod;

import com.moandjiezana.toml.Toml;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// La anotación @Mod le dice a Forge que esta es una clase de mod principal
// value especifica el modid, dist especifica que es solo para servidor
@Mod(value = "better_sleep_infinixmc")
public class SleepingMod {
    private static final int DELAY_TICKS = 100;        // Delay en ticks (5 segundos a 20 TPS)

    // Valores por defecto, se sobreescriben con lo que carguemos de TOML
    private static String serverName = "Servidor";
    private static String serverNameColor = "§6§l";
    private static String messageColor = "§a";
    private static String textFormat = "§o";
    private static double sleepThreshold = 0.5;
    private static boolean clearStorms = true;
    private static String stormPersistMessage  = "It's still storming! Unable to skip the night.";

    private final Map<ServerLevel, Integer> sleepTicks = new HashMap<>();
    private final Random random = new Random();
    private final List<String> sleepingMessages = new ArrayList<>();
    private final List<String> morningMessages = new ArrayList<>();
    private final Map<ServerLevel, SleepState> worldStates = new HashMap<>();
    private final Map<String, Object> defaultConfig = new LinkedHashMap<>();
    private final Map<String, String> defaultComments = new LinkedHashMap<>();

    // Enum para manejar estados del proceso de sueño
    private enum SleepState {
        NONE,       // No hay jugadores durmiendo o el proceso no está en marcha
        SLEEPING,   // Jugadores están durmiendo, pero aún no se ha completado el proceso
        COMPLETED   // El proceso de sueño y tormenta se ha completado
    }

    public SleepingMod() {
        // Aceptar clientes aunque no tengan el mod (server-only)
        ModLoadingContext.get().registerExtensionPoint(
            IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(
                NetworkConstants.IGNORESERVERONLY,     // <-- String correcto
                (remoteVersion, isServer) -> true      // aceptar siempre la conexión
            )
        );

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }


    private void setup(final FMLCommonSetupEvent event) {
        // Inicializa los valores predeterminados
        initializeDefaultConfig();

        // Cargar (o crear) configuración en TOML
        loadConfig();
    }

    // Este evento se dispara cada tick del servidor
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Solo ejecutamos en la fase END para evitar duplicados
        if (event.phase != TickEvent.Phase.END) return;
        
        for (ServerLevel world : event.getServer().getAllLevels()) {
            int totalPlayers = world.players().size();
            long sleepingPlayers = world.players().stream().filter(LivingEntity::isSleeping).count();

            // Obtiene el estado actual del mundo
            SleepState state = worldStates.getOrDefault(world, SleepState.NONE);

            if (totalPlayers > 0 &&
                    ((sleepThreshold < 1 && (double) sleepingPlayers / totalPlayers >= sleepThreshold) ||
                            (sleepThreshold == 1 && sleepingPlayers == totalPlayers))) {

                if (state == SleepState.NONE) {
                    // Cambia el estado a "SLEEPING" y envía el mensaje de inicio
                    worldStates.put(world, SleepState.SLEEPING);
                    sleepTicks.put(world, event.getServer().getTickCount());
                    String message = getRandomMessage(sleepingMessages,
                            "The majority is sleeping! Preparing for sunrise...");
                    String formattedMessage = formatMessage(serverName, message);
                    world.players().forEach(player ->
                            player.sendSystemMessage(Component.literal(formattedMessage))
                    );
                } else if (state == SleepState.SLEEPING) {
                    // Verifica si ha pasado el tiempo de delay
                    int ticksSinceStart = event.getServer().getTickCount() - sleepTicks.get(world);
                    if (ticksSinceStart >= DELAY_TICKS) {
                        // Si `clearStorms` está desactivado y sigue habiendo tormenta, informa a los jugadores
                        if (!clearStorms && (world.isRaining() || world.isThundering())) {
                            String formattedMessage = formatMessage(serverName, stormPersistMessage);
                            world.players().forEach(player ->
                                    player.sendSystemMessage(Component.literal(formattedMessage))
                            );

                            // Cambia el estado a NONE para evitar el bucle
                            worldStates.put(world, SleepState.NONE);
                            sleepTicks.remove(world);

                            // "Levanta" a los jugadores de la cama
                            world.players().stream()
                                    .filter(LivingEntity::isSleeping)
                                    .forEach(player -> player.stopSleeping());

                            System.out.println("Storm persists, skipping process halted.");
                            return; // Sal del flujo sin completar
                        }

                        // Cambia el tiempo a la mañana
                        world.setDayTime(0);

                        // Limpia la tormenta si corresponde
                        if ((world.isRaining() || world.isThundering()) && clearStorms) {
                            world.setWeatherParameters(0, 0, false, false);
                            String weatherMessage = "The storm has ended!";
                            world.players().forEach(player ->
                                    player.sendSystemMessage(Component.literal(formatMessage(serverName, weatherMessage)))
                            );
                        }

                        // Envía el mensaje de buenos días
                        String message = getRandomMessage(morningMessages,
                                "Good morning! The sun is here.");
                        String formattedMessage = formatMessage(serverName, message);
                        world.players().forEach(player ->
                                player.sendSystemMessage(Component.literal(formattedMessage))
                        );

                        // Cambia el estado a "COMPLETED"
                        worldStates.put(world, SleepState.COMPLETED);
                    }
                }
            } else {
                // Reinicia el flujo si ya no duermen la mayoría
                if (state != SleepState.NONE) {
                    System.out.println("Resetting sleep countdown for world: " + world.dimension().location());
                }
                worldStates.put(world, SleepState.NONE);
                sleepTicks.remove(world);
            }
        }
    }

    /**
     * Carga la configuración desde sleepingmod-config.toml, o la crea si no existe.
     */
    private void loadConfig() {
        File configFile = new File("config/sleepingmod-config.toml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        } else {
            // Verifica y actualiza claves faltantes
            updateConfigIfNeeded(configFile);
        }

        try {
            // Leemos el archivo TOML
            Toml toml = new Toml().read(configFile);

            // Campos simples
            serverName = toml.getString("server_name", serverName);
            serverNameColor = toml.getString("server_name_color", serverNameColor);
            messageColor = toml.getString("message_color", messageColor);
            textFormat = toml.getString("format", textFormat);

            // Leer porcentaje de jugadores que deben dormir
            try {
                sleepThreshold = toml.getDouble("sleep_threshold", sleepThreshold);
            } catch (ClassCastException e) {
                Long thresholdAsLong = toml.getLong("sleep_threshold", null);
                if (thresholdAsLong != null) {
                    sleepThreshold = thresholdAsLong.doubleValue();
                } else {
                    System.err.println("Invalid format for 'sleep_threshold'. Using default value: " + sleepThreshold);
                }
            }

            // Leer opción para limpiar tormentas
            clearStorms = toml.getBoolean("clear_storms", clearStorms);
            System.out.println("clearStorms: " + clearStorms); // Verifica el valor cargado

            // Mensaje de tormenta persistente
            stormPersistMessage = toml.getString("storm_persist_message", stormPersistMessage);

            // Listas de mensajes
            sleepingMessages.clear();
            List<Object> readSleeping = toml.getList("sleeping_messages");
            if (readSleeping != null) {
                for (Object obj : readSleeping) {
                    if (obj instanceof String) {
                        sleepingMessages.add((String) obj);
                    }
                }
            }

            morningMessages.clear();
            List<Object> readMorning = toml.getList("morning_messages");
            if (readMorning != null) {
                for (Object obj : readMorning) {
                    if (obj instanceof String) {
                        morningMessages.add((String) obj);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al leer el archivo TOML: " + e.getMessage());
        }
    }

    /**
     * Crea un archivo TOML por defecto en caso de que no exista.
     */
    private void createDefaultConfig(File configFile) {
        try {
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Para poder incluir comentarios, podemos escribirlo manualmente
            // o usar un TomlWriter con un objeto de config.
            // A continuación, un ejemplo manual con comentarios:

            String defaultToml =
                    "# ============ SleepingMod Config ============\n" +
                            "# Name of the server\n" +
                            "server_name = \"Server\"\n\n" +

                            "# Color and format for the server name\n" +
                            "server_name_color = \"§6§l\"\n" +
                            "message_color = \"§a\"\n" +
                            "format = \"§o\"\n\n" +

                            "# Percentage of players required to sleep to skip the night (value between 0 and 1, 0.5 = 50%)\n" +
                            "sleep_threshold = 0.5\n\n" +

                            "# Setting to clear storms when players sleep (true = Clear storms or false = Keep storms active.)\n" +
                            "clear_storms = true\n\n" +

                            "# persistent storm message\n" +
                            "storm_persist_message = \"It's still storming! Unable to skip the night.\"\n\n" +

                            "# Messages sent when the majority is sleeping\n" +
                            "sleeping_messages = [\n" +
                            "  \"Oh no! Everyone is entering the land of dreams... 😴\",\n" +
                            "  \"Is this a pajama party? 🛌\",\n" +
                            "  \"Watch out! Even the monsters are falling asleep. 💤\"\n" +
                            "]\n\n" +

                            "# Messages sent when the morning arrives\n" +
                            "morning_messages = [\n" +
                            "  \"Good morning, sleepyheads! The sun is here ☀️.\",\n" +
                            "  \"Time to get up! Where's the coffee? ☕\",\n" +
                            "  \"The dawn is here! Let's get to work 💪.\"\n" +
                            "]\n";

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultToml);
            }

            System.out.println("Archivo de configuración TOML creado en: " + configFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error al crear el archivo TOML: " + e.getMessage());
        }
    }

    /**
     * Retorna un mensaje aleatorio de la lista o uno por defecto.
     */
    private String getRandomMessage(List<String> messages, String defaultMessage) {
        if (messages.isEmpty()) {
            return defaultMessage;
        }
        return messages.get(random.nextInt(messages.size()));
    }

    /**
     * Formatea un mensaje con los colores y formato configurados.
     */
    private String formatMessage(String serverName, String message) {
        return serverNameColor + "[" + serverName + "] "
                + messageColor + textFormat + message + "§r";
    }

    private void initializeDefaultConfig() {
        // Agrega todas las claves y valores predeterminados aquí
        defaultConfig.put("server_name", "Server");
        defaultComments.put("server_name", "# Name of the server");

        defaultConfig.put("server_name_color", "§6§l");
        defaultComments.put("server_name_color", "# Color and format for the server name");

        defaultConfig.put("message_color", "§a");
        defaultComments.put("message_color", "# Color for messages");

        defaultConfig.put("format", "§o");
        defaultComments.put("format", "# Text format for messages");

        defaultConfig.put("sleep_threshold", 0.5);
        defaultComments.put("sleep_threshold", "# Percentage of players required to sleep to skip the night (value between 0 and 1, 0.5 = 50%)");

        defaultConfig.put("clear_storms", true);
        defaultComments.put("clear_storms", "# Setting to clear storms when players sleep, (true = Clear storms or false = Keep storms active.)");

        defaultConfig.put("storm_persist_message", "It's still storming! Unable to skip the night.");
        defaultComments.put("storm_persist_message", "# Message sent when the storm persists and cannot skip the night");

        defaultConfig.put("sleeping_messages", Arrays.asList(
                "Oh no! Everyone is entering the land of dreams... 😴",
                "Is this a pajama party? 🛌",
                "Watch out! Even the monsters are falling asleep. 💤"
        ));
        defaultComments.put("sleeping_messages", "# Messages sent when the majority is sleeping");

        defaultConfig.put("morning_messages", Arrays.asList(
                "Good morning, sleepyheads! The sun is here ☀️.",
                "Time to get up! Where's the coffee? ☕",
                "The dawn is here! Let's get to work 💪."
        ));
        defaultComments.put("morning_messages", "# Messages sent when the morning arrives");
    }

    private void updateConfigIfNeeded(File configFile) {
        try {
            List<String> lines = new ArrayList<>();
            Map<String, String> existingConfig = new HashMap<>();
            boolean updated = false;

            // Leer el archivo línea por línea
            try (Scanner scanner = new Scanner(configFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    lines.add(line);

                    // Si la línea contiene una clave-valor, agrégala al mapa
                    if (line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        existingConfig.put(key, value);
                    }
                }
            }

            // Buscar claves faltantes
            for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
                String key = entry.getKey();
                if (!existingConfig.containsKey(key)) {
                    updated = true;

                    // Agrega el comentario si existe
                    if (defaultComments.containsKey(key)) {
                        lines.add(defaultComments.get(key)); // Comentario
                    }

                    // Agrega la clave con su valor predeterminado
                    String newValue = formatValue(entry.getValue());
                    lines.add(key + " = " + newValue); // Clave y valor
                    lines.add(""); // Línea vacía para separación

                    System.out.println("Adding missing key to config: " + key);
                }
            }

            // Si se actualizaron claves, escribir el archivo actualizado
            if (updated) {
                try (FileWriter writer = new FileWriter(configFile)) {
                    for (String line : lines) {
                        writer.write(line + System.lineSeparator());
                    }
                }
                System.out.println("Configuration file updated with missing keys.");
            }
        } catch (IOException e) {
            System.err.println("Error updating configuration file: " + e.getMessage());
        }
    }

    /**
     * Formatea un valor en formato TOML.
     */
    private String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            return "[" + String.join(", ", list.stream().map(this::formatValue).toArray(String[]::new)) + "]";
        } else {
            return value.toString();
        }
    }
}