/*
 * AdminActionLogger - 관리자 행동 감시 및 로깅 플러그인
 * 기존 CreativeItemLogger 플러그인을 확장하여 다양한 관리자 행동을 감시합니다.
 */

package com.suda.creativeitemlogger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class CreativeItemLogger extends JavaPlugin implements Listener {

    private boolean enableLogging;
    private List<String> commandsToExecute;
    private SimpleDateFormat dateFormat;
    private List<String> monitoredPlayers;
    private List<String> monitoredWorlds;
    private List<String> exemptPlayers;
    private Map<UUID, List<ItemStack>> playerInventories;
    private Set<String> sensitiveCommands;
    private File logFile;
    private boolean logToFile;
    private boolean detailedLogging;
    private boolean logGameModeChanges;
    private boolean logItemTransfers;
    private boolean logSensitiveCommands;
    
    // 플레이어별 로그 관련 필드
    private boolean playerSpecificLogs;
    private String playerLogsDirectory;
    private String playerLogsFilename;
    private Map<UUID, String> playerNameCache; // UUID와 플레이어 이름 매핑
    private File playersMappingFile; // UUID와 플레이어 이름 매핑 정보 저장 파일

    @Override
    public void onEnable() {
        // 설정 파일 저장
        saveDefaultConfig();
        
        // 설정 로드
        loadConfig();
        
        // 로그 파일 설정
        setupLogFile();
        
        // 플레이어 매핑 파일 설정
        setupPlayerMappingFile();
        
        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(this, this);
        
        // 명령어 등록
        getCommand("creativelogger").setExecutor(new CreativeLoggerCommand(this));
        
        // 인벤토리 추적 초기화
        playerInventories = new HashMap<>();
        playerNameCache = new HashMap<>();
        
        // 플레이어 UUID-이름 매핑 로드
        loadPlayerMapping();
        
        // 주기적 인벤토리 체크 (1분마다)
        if (detailedLogging && logItemTransfers) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkInventoryChanges();
                }
            }.runTaskTimer(this, 1200L, 1200L); // 1분마다 실행 (20 ticks * 60)
        }
        
        // 현재 접속한 플레이어의 인벤토리 저장
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldMonitorPlayer(player)) {
                savePlayerInventory(player);
                
                // 플레이어 매핑 업데이트
                updatePlayerMapping(player.getUniqueId(), player.getName());
            }
        }
        
        getLogger().info("AdminActionLogger가 활성화되었습니다. 모니터링 대상: " + String.join(", ", monitoredPlayers));
    }

    @Override
    public void onDisable() {
        // 플레이어 매핑 저장
        savePlayerMapping();
        getLogger().info("AdminActionLogger가 비활성화되었습니다.");
    }
    
    public void loadConfig() {
        reloadConfig();
        enableLogging = getConfig().getBoolean("enable-logging", true);
        commandsToExecute = getConfig().getStringList("commands-to-execute");
        String dateFormatPattern = getConfig().getString("date-format", "yyyy-MM-dd HH:mm:ss");
        dateFormat = new SimpleDateFormat(dateFormatPattern);
        
        // 추가 설정
        monitoredPlayers = getConfig().getStringList("monitored-players");
        monitoredWorlds = getConfig().getStringList("worlds");
        exemptPlayers = getConfig().getStringList("exempt-players");
        sensitiveCommands = new HashSet<>(getConfig().getStringList("sensitive-commands"));
        logToFile = getConfig().getBoolean("log-to-file", true);
        detailedLogging = getConfig().getBoolean("detailed-logging", true);
        logGameModeChanges = getConfig().getBoolean("log-gamemode-changes", true);
        logItemTransfers = getConfig().getBoolean("log-item-transfers", true);
        logSensitiveCommands = getConfig().getBoolean("log-sensitive-commands", true);
        
        // 플레이어별 로그 설정
        playerSpecificLogs = getConfig().getBoolean("player-specific-logs", true);
        playerLogsDirectory = getConfig().getString("player-logs.directory", "players");
        playerLogsFilename = getConfig().getString("player-logs.filename", "actions.log");
    }
    
    private void setupLogFile() {
        if (!logToFile) return;
        
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        logFile = new File(dataFolder, getConfig().getString("log-file.filename", "admin-actions.log"));
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (IOException e) {
            getLogger().severe("로그 파일을 생성할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void setupPlayerMappingFile() {
        if (!playerSpecificLogs) return;
        
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        playersMappingFile = new File(dataFolder, "player-mapping.txt");
        try {
            if (!playersMappingFile.exists()) {
                playersMappingFile.createNewFile();
            }
        } catch (IOException e) {
            getLogger().severe("플레이어 매핑 파일을 생성할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void loadPlayerMapping() {
        if (!playerSpecificLogs || playersMappingFile == null || !playersMappingFile.exists()) return;
        
        try {
            List<String> lines = Files.readAllLines(playersMappingFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    try {
                        UUID uuid = UUID.fromString(parts[0]);
                        String name = parts[1];
                        playerNameCache.put(uuid, name);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("잘못된 UUID 형식: " + parts[0]);
                    }
                }
            }
            getLogger().info(playerNameCache.size() + "명의 플레이어 매핑 데이터를 로드했습니다.");
        } catch (IOException e) {
            getLogger().severe("플레이어 매핑 파일을 로드할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void savePlayerMapping() {
        if (!playerSpecificLogs || playersMappingFile == null) return;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(playersMappingFile, false))) {
            for (Map.Entry<UUID, String> entry : playerNameCache.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
            getLogger().info(playerNameCache.size() + "명의 플레이어 매핑 데이터를 저장했습니다.");
        } catch (IOException e) {
            getLogger().severe("플레이어 매핑 파일을 저장할 수 없습니다: " + e.getMessage());
        }
    }
    
    private void updatePlayerMapping(UUID uuid, String name) {
        playerNameCache.put(uuid, name);
    }
    
    private File getPlayerLogFile(UUID playerId) {
        if (!playerSpecificLogs) return null;
        
        String playerName = playerNameCache.getOrDefault(playerId, "unknown");
        File playerDir = new File(getDataFolder(), playerLogsDirectory + File.separator + playerId.toString());
        
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
        
        // 최상위에 UUID 정보 파일 생성
        File uuidInfoFile = new File(playerDir, "info.txt");
        if (!uuidInfoFile.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(uuidInfoFile))) {
                writer.println("UUID: " + playerId);
                writer.println("Username: " + playerName);
                writer.println("First logged: " + dateFormat.format(new Date()));
            } catch (IOException e) {
                getLogger().severe("플레이어 정보 파일을 생성할 수 없습니다: " + e.getMessage());
            }
        }
        
        return new File(playerDir, playerLogsFilename);
    }
    
    private void writeToLog(String message) {
        // 콘솔에 로그 출력
        getLogger().info(message);
        
        // 파일에 로그 기록
        if (logToFile && logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(message);
            } catch (IOException e) {
                getLogger().severe("로그를 파일에 쓸 수 없습니다: " + e.getMessage());
            }
        }
    }
    
    private void writeToPlayerLog(UUID playerId, String message) {
        if (!playerSpecificLogs) return;
        
        File playerLogFile = getPlayerLogFile(playerId);
        if (playerLogFile == null) return;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(playerLogFile, true))) {
            writer.println(message);
        } catch (IOException e) {
            getLogger().severe("플레이어 로그를 파일에 쓸 수 없습니다: " + e.getMessage());
        }
    }
    
    public boolean isLoggingEnabled() {
        return enableLogging;
    }
    
    public void setLoggingEnabled(boolean enabled) {
        enableLogging = enabled;
        getConfig().set("enable-logging", enabled);
        saveConfig();
    }
    
    private boolean shouldMonitorPlayer(Player player) {
        // 모니터링 제외 플레이어 확인
        if (exemptPlayers.contains(player.getName())) {
            return false;
        }
        
        // 모니터링 대상 플레이어 확인
        if (monitoredPlayers.isEmpty()) {
            // 모니터링 대상이 없으면 OP나 특정 권한을 가진 플레이어만 모니터링
            return player.isOp() || player.hasPermission("essentials.gamemode");
        } else {
            return monitoredPlayers.contains(player.getName());
        }
    }
    
    private boolean shouldMonitorWorld(String worldName) {
        return monitoredWorlds.isEmpty() || monitoredWorlds.contains(worldName);
    }
    
    // 플레이어별 로그 디렉토리 가져오기
    public File getPlayerLogDirectory(UUID playerId) {
        return new File(getDataFolder(), playerLogsDirectory + File.separator + playerId.toString());
    }
    
    // 플레이어 이름으로 UUID 가져오기
    public UUID getPlayerUUID(String playerName) {
        for (Map.Entry<UUID, String> entry : playerNameCache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // UUID로 플레이어 이름 가져오기
    public String getPlayerName(UUID uuid) {
        return playerNameCache.getOrDefault(uuid, null);
    }
    
    // 모니터링된 모든 플레이어 UUID 목록 가져오기
    public List<UUID> getAllMonitoredPlayerUUIDs() {
        return new ArrayList<>(playerNameCache.keySet());
    }
    
    // 플레이어별 로그 활성화 여부 확인
    public boolean isPlayerSpecificLogsEnabled() {
        return playerSpecificLogs;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (enableLogging && shouldMonitorPlayer(player)) {
            String message = String.format("[%s] %s님이 서버에 접속했습니다. (IP: %s, 위치: %s)", 
                    dateFormat.format(new Date()), 
                    player.getName(),
                    player.getAddress().getAddress().getHostAddress(),
                    formatLocation(player.getLocation()));
            writeToLog(message);
            
            // 플레이어별 로그에도 기록
            writeToPlayerLog(player.getUniqueId(), message);
            
            // 플레이어 매핑 업데이트
            updatePlayerMapping(player.getUniqueId(), player.getName());
            
            // 인벤토리 저장
            if (detailedLogging && logItemTransfers) {
                savePlayerInventory(player);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (enableLogging && shouldMonitorPlayer(player)) {
            String message = String.format("[%s] %s님이 서버에서 나갔습니다. (마지막 위치: %s)", 
                    dateFormat.format(new Date()), 
                    player.getName(),
                    formatLocation(player.getLocation()));
            writeToLog(message);
            
            // 플레이어별 로그에도 기록
            writeToPlayerLog(player.getUniqueId(), message);
            
            // 플레이어가 나가면 인벤토리 기록 삭제
            if (detailedLogging && logItemTransfers) {
                playerInventories.remove(player.getUniqueId());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!enableLogging || !logGameModeChanges) return;
        
        Player player = event.getPlayer();
        if (shouldMonitorPlayer(player) && shouldMonitorWorld(player.getWorld().getName())) {
            String message = String.format("[%s] %s님의 게임모드가 %s에서 %s로 변경되었습니다. (위치: %s)", 
                    dateFormat.format(new Date()), 
                    player.getName(),
                    player.getGameMode().toString(),
                    event.getNewGameMode().toString(),
                    formatLocation(player.getLocation()));
            writeToLog(message);
            
            // 플레이어별 로그에도 기록
            writeToPlayerLog(player.getUniqueId(), message);
            
            executeCommands(player, "게임모드 변경", player.getGameMode() + " -> " + event.getNewGameMode(), 1);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enableLogging) return;
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!shouldMonitorPlayer(player) || !shouldMonitorWorld(player.getWorld().getName())) return;
        
        if (player.getGameMode() != GameMode.CREATIVE) return;
        
        // 마우스 휠 클릭(중간 버튼) 확인
        if (event.getClick() == ClickType.MIDDLE) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                logItemAction(player, clickedItem, "복사");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!enableLogging) return;
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!shouldMonitorPlayer(player) || !shouldMonitorWorld(player.getWorld().getName())) return;
        
        ItemStack item = event.getCursor();
        if (item != null && !item.getType().isAir()) {
            // 아이템 배치(획득)인지 아이템 가져오기인지 구분
            if (event.getSlotType().toString().equals("QUICKBAR") || 
                event.getSlotType().toString().equals("CONTAINER")) {
                if (event.getClick() == ClickType.CREATIVE) {
                    logItemAction(player, item, "생성");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!enableLogging || !logItemTransfers) return;
        
        Player player = event.getPlayer();
        
        if (!shouldMonitorPlayer(player) || !shouldMonitorWorld(player.getWorld().getName())) return;
        
        ItemStack item = event.getItemDrop().getItemStack();
        if (player.getGameMode() == GameMode.CREATIVE) {
            logItemAction(player, item, "드롭");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!enableLogging || !logItemTransfers) return;
        
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        if (!shouldMonitorPlayer(player) || !shouldMonitorWorld(player.getWorld().getName())) return;
        
        ItemStack item = event.getItem().getItemStack();
        if (player.getGameMode() == GameMode.CREATIVE) {
            logItemAction(player, item, "주웠음");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!enableLogging || !logSensitiveCommands) return;
        
        Player player = event.getPlayer();
        
        if (!shouldMonitorPlayer(player)) return;
        
        String command = event.getMessage().toLowerCase();
        
        // 민감한 명령어 확인
        boolean isSensitive = false;
        for (String sensitiveCmd : sensitiveCommands) {
            if (command.startsWith("/" + sensitiveCmd.toLowerCase())) {
                isSensitive = true;
                break;
            }
        }
        
        if (isSensitive) {
            String message = String.format("[%s] %s님이 민감한 명령어를 사용했습니다: %s (위치: %s)", 
                    dateFormat.format(new Date()), 
                    player.getName(),
                    event.getMessage(),
                    formatLocation(player.getLocation()));
            writeToLog(message);
            
            // 플레이어별 로그에도 기록
            writeToPlayerLog(player.getUniqueId(), message);
            
            // 명령어 실행
            executeCommands(player, "명령어 사용", event.getMessage(), 1);
        }
    }
    
    private void logItemAction(Player player, ItemStack item, String action) {
        String itemName = item.getType().toString();
        int amount = item.getAmount();
        
        // 아이템 이름 가져오기 (만약 아이템에 이름이 설정되어 있다면)
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            itemName = item.getItemMeta().getDisplayName() + " (" + itemName + ")";
        }
        
        String message = String.format("[%s] %s님이 크리에이티브 모드에서 %s x%d을(를) %s했습니다. (위치: %s)", 
                dateFormat.format(new Date()), 
                player.getName(), 
                itemName, 
                amount,
                action,
                formatLocation(player.getLocation()));
        
        writeToLog(message);
        
        // 플레이어별 로그에도 기록
        writeToPlayerLog(player.getUniqueId(), message);
        
        // 명령어 실행
        executeCommands(player, action, itemName, amount);
    }
    
    private void executeCommands(Player player, String action, String itemName, int amount) {
        String timestamp = dateFormat.format(new Date());
        String playerName = player.getName();
        
        // 설정된 명령어 실행
        for (String command : commandsToExecute) {
            String formattedCommand = command
                .replace("%player%", playerName)
                .replace("%item%", itemName)
                .replace("%amount%", String.valueOf(amount))
                .replace("%time%", timestamp)
                .replace("%action%", action)
                .replace("%location%", formatLocation(player.getLocation()));
            
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
            } catch (Exception e) {
                getLogger().warning("명령어 실행 중 오류 발생: " + e.getMessage());
            }
        }
    }
    
    private void savePlayerInventory(Player player) {
        List<ItemStack> inventory = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                inventory.add(item.clone());
            }
        }
        playerInventories.put(player.getUniqueId(), inventory);
    }
    
    private void checkInventoryChanges() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!shouldMonitorPlayer(player)) continue;
            
            UUID playerId = player.getUniqueId();
            List<ItemStack> oldInventory = playerInventories.get(playerId);
            
            if (oldInventory != null) {
                List<ItemStack> currentInventory = new ArrayList<>();
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        currentInventory.add(item.clone());
                    }
                }
                
                // 새로 추가된 아이템 확인
                for (ItemStack current : currentInventory) {
                    boolean found = false;
                    for (ItemStack old : oldInventory) {
                        if (isSimilarItem(current, old) && current.getAmount() <= old.getAmount()) {
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        // 새로운 아이템이 추가됨
                        if (player.getGameMode() == GameMode.SURVIVAL) {
                            String message = String.format("[%s] %s님의 인벤토리에 새 아이템이 추가되었습니다: %s x%d (위치: %s)", 
                                    dateFormat.format(new Date()), 
                                    player.getName(), 
                                    current.getType().toString(),
                                    current.getAmount(),
                                    formatLocation(player.getLocation()));
                            writeToLog(message);
                            
                            // 플레이어별 로그에도 기록
                            writeToPlayerLog(player.getUniqueId(), message);
                        }
                    }
                }
            }
            
            // 현재 인벤토리 저장
            savePlayerInventory(player);
        }
    }
    
    private boolean isSimilarItem(ItemStack item1, ItemStack item2) {
        if (item1.getType() != item2.getType()) return false;
        if (item1.hasItemMeta() != item2.hasItemMeta()) return false;
        
        if (item1.hasItemMeta() && item2.hasItemMeta()) {
            if (item1.getItemMeta().hasDisplayName() != item2.getItemMeta().hasDisplayName()) return false;
            if (item1.getItemMeta().hasDisplayName() && item2.getItemMeta().hasDisplayName()) {
                return item1.getItemMeta().getDisplayName().equals(item2.getItemMeta().getDisplayName());
            }
        }
        
        return true;
    }
    
    private String formatLocation(org.bukkit.Location location) {
        return String.format("%s, %.2f, %.2f, %.2f", 
                location.getWorld().getName(), 
                location.getX(), 
                location.getY(), 
                location.getZ());
    }
}