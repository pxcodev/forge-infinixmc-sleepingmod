# 🎉 MOD COMPILADO EXITOSAMENTE PARA MINECRAFT 1.20.1

## ✅ **Estado:** LISTO PARA USAR

### 📁 **Archivo del mod:**
- **Ubicación:** `build/libs/better-sleep-infinixmc-forge-1.0.2.jar`
- **Tamaño:** ~50KB aproximadamente
- **Compilado:** 15 de septiembre de 2025

### 🎮 **Compatibilidad:**
- **Minecraft:** 1.20.1
- **Forge:** 47.4.8+ (compatible con tu servidor)
- **Java:** 17+ (recomendado)

### 🔧 **Problema resuelto:**
El error original era:
```
Missing language javafml version [52,) wanted by better-sleep-infinixmc-forge-1.0.2.jar, found 47
```

**Solución aplicada:**
- Actualizadas las versiones en `gradle.properties` para MC 1.20.1
- Cambiado `loaderVersion="[47,)"` en `mods.toml`
- Corregidos los métodos de API para MC 1.20.1
- Usado `setWeatherParameters()` en lugar de acceso directo a `serverLevelData`

### 📋 **Funcionalidades del mod:**
- ⚡ Porcentaje configurable de jugadores para adelantar la noche (50% por defecto)
- 🌧️ Control automático de tormentas
- 💬 Mensajes personalizables aleatorios al dormir y al amanecer
- ⚙️ Sistema de configuración TOML
- 🎨 Colores y formato personalizables
- ⏱️ Delay configurable (5 segundos por defecto)

### 🚀 **Cómo usar:**
1. Copia `better-sleep-infinixmc-forge-1.0.2.jar` a la carpeta `mods` de tu servidor MC 1.20.1
2. Inicia el servidor
3. El archivo de configuración se creará automáticamente en `config/sleepingmod-config.toml`
4. ¡Disfruta del mod mejorado de sueño!

### 📝 **Configuración automática:**
Al iniciar el servidor por primera vez, se creará:
```
config/sleepingmod-config.toml
```

Con configuraciones predeterminadas que puedes personalizar.

---
**✨ Mod adaptado exitosamente de Fabric a Forge para MC 1.20.1 ✨**